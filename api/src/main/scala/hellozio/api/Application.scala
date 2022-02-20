package hellozio.api

import hellozio.api.common.config.AppConfig
import hellozio.api.todo.{TodoController, TodoPublisher, TodoRepository, TodoService}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio._
import zio.interop.catz._

object Application extends ZIOAppDefault {

  val publisherLayer = AppConfig.layer >>> TodoPublisher.layer
  val serviceLayer   = (publisherLayer ++ TodoRepository.inmemory) >>> TodoService.layer
  val httpLayer      = (serviceLayer ++ Clock.live) >>> TodoController.layer

  override def run: URIO[zio.ZEnv, ExitCode] =
    ZIO
      .service[AppConfig]
      .zip(TodoController.routes)
      .flatMap { case (config, routes) =>
        BlazeServerBuilder[RIO[Clock, *]]
          .withExecutionContext(runtime.runtimeConfig.executor.asExecutionContext)
          .bindHttp(config.server.port, config.server.host)
          .withHttpApp(Router("/" -> routes).orNotFound)
          .serve
          .compile
          .drain
      }
      .provideLayer(AppConfig.layer ++ httpLayer ++ Clock.live)
      .exitCode


}
