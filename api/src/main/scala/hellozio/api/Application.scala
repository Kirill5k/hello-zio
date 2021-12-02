package hellozio.api

import hellozio.api.common.config.AppConfig
import hellozio.api.todo.{TodoController, TodoPublisher, TodoRepository, TodoService}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio.{ExitCode, RIO, URIO, ZEnv, ZIO}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._

object Application extends zio.App {

  val configLayer    = Blocking.live >>> AppConfig.layer
  val publisherLayer = (Blocking.live ++ configLayer) >>> TodoPublisher.live
  val repoLayer      = TodoRepository.inmemory
  val serviceLayer   = (publisherLayer ++ repoLayer) >>> TodoService.layer
  val httpLayer      = (serviceLayer ++ Clock.live) >>> TodoController.layer

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    ZIO
      .runtime[ZEnv]
      .flatMap { implicit runtime =>
        ZIO
          .services[AppConfig, TodoController]
          .provideLayer(configLayer ++ httpLayer)
          .flatMap { case (config, controller) =>
            BlazeServerBuilder[RIO[Clock with Blocking, *]]
              .withExecutionContext(runtime.platform.executor.asEC)
              .bindHttp(config.server.port, config.server.host)
              .withHttpApp(Router("/" -> controller.routes).orNotFound)
              .serve
              .compile
              .drain
          }
      }
      .exitCode

}