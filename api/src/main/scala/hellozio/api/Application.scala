package hellozio.api

import hellozio.api.common.config.AppConfig
import hellozio.api.todo.{TodoController, TodoPublisher, TodoRepository, TodoService}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import zio._
import zio.interop.catz._

object Application extends ZIOAppDefault {

  val serviceLayer   = (TodoPublisher.layer ++ TodoRepository.inmemory) >>> TodoService.layer
  val httpLayer      = serviceLayer >+> TodoController.layer

  override def run: URIO[Scope, ExitCode] =
    ZIO
      .service[AppConfig]
      .zip(TodoController.routes)
      .flatMap { case (config, routes) =>
        BlazeServerBuilder[Task]
          .withExecutionContext(runtime.executor.asExecutionContext)
          .bindHttp(config.server.port, config.server.host)
          .withHttpApp(Router("/" -> routes).orNotFound)
          .serve
          .compile
          .drain
      }
      .provideLayer(Clock.live ++ AppConfig.layer >+> httpLayer)
      .exitCode
}
