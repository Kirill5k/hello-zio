package hellozio.api

import hellozio.api.common.config.AppConfig
import hellozio.api.todo.{TodoController, TodoPublisher, TodoRepository, TodoService}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio._
import zio.interop.catz._

object Application extends ZIOAppDefault {

  val configLayer    = AppConfig.layer
  val publisherLayer = configLayer >>> TodoPublisher.live
  val repoLayer      = TodoRepository.inmemory
  val serviceLayer   = (publisherLayer ++ repoLayer) >>> TodoService.layer
  val httpLayer      = (serviceLayer ++ Clock.live) >>> TodoController.layer

  override def run: URIO[zio.ZEnv, ExitCode] =
    ZIO
      .runtime[ZEnv]
      .flatMap { implicit runtime =>
        ZIO
          .service[AppConfig]
          .zip(ZIO.service[TodoController])
          .provideLayer(configLayer ++ httpLayer)
          .flatMap { case (config, controller) =>
            BlazeServerBuilder[RIO[Clock, *]]
              .withExecutionContext(runtime.runtimeConfig.executor.asExecutionContext)
              .bindHttp(config.server.port, config.server.host)
              .withHttpApp(Router("/" -> controller.routes).orNotFound)
              .serve
              .compile
              .drain
          }
      }
      .exitCode

}
