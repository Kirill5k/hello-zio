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
      .tupledPar(ZIO.service[AppConfig].map(_.server), ZIO.service[TodoController].map(_.routes))
      .flatMap { case (config, routes) => Server.start(config.port, routes) }
      .orDie
      .provideLayer(layer)
      .exitCode
}
