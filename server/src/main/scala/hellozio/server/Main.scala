package hellozio.server

import hellozio.server.common.config.AppConfig
import hellozio.server.todo.TodoController
import hellozio.server.todo.TodoRepository
import hellozio.server.todo.TodoService
import zhttp.service.Server
import zio.{ExitCode, URIO, ZIO}

object Main extends zio.App {

  val configLayer = AppConfig.layer
  val httpLayer   = TodoRepository.inmemory >>> TodoService.layer >>> TodoController.layer
  val layer       = configLayer ++ httpLayer

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    ZIO
      .services[AppConfig, TodoController]
      .flatMap { case (config, controller) => Server.start(config.server.port, controller.routes) }
      .orDie
      .provideLayer(layer)
      .exitCode
}
