package hellozio.server

import hellozio.server.common.config.AppConfig
import hellozio.server.todo.{TodoController, TodoRepository, TodoService}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio.{ExitCode, RIO, URIO, ZEnv, ZIO}
import zio.blocking.Blocking
import zio.clock.Clock
import zio.interop.catz._

object Main extends zio.App {

  val configLayer = Blocking.live >>> AppConfig.layer
  val httpLayer   = TodoRepository.inmemory >>> (TodoService.layer ++ Clock.live) >>> TodoController.layer

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    ZIO
      .runtime[ZEnv]
      .flatMap { implicit runtime =>
        ZIO
          .services[AppConfig, TodoController]
          .provideLayer(configLayer ++ httpLayer)
          .flatMap { case (config, controller) =>
            BlazeServerBuilder[RIO[Clock with Blocking, *]](runtime.platform.executor.asEC)
              .bindHttp(config.server.port, config.server.host)
              .withHttpApp(Router("/" -> controller.routes).orNotFound)
              .serve
              .compile
              .drain
          }
      }
      .exitCode

}
