package hellozio.server.todo

import hellozio.server.AppError
import zhttp.http.{UHttpApp}
import zio.{Has, ZIO, ZLayer}

trait TodoController {
  def routes: UHttpApp
}

final private case class TodoControllerLive(service: TodoService) extends TodoController {
  override def routes: UHttpApp = ???
}

object TodoController {

  val live: ZLayer[Has[TodoService], AppError, Has[TodoController]] = ZLayer
    .fromService[TodoService, TodoController](TodoControllerLive)

  def routes: ZIO[Has[TodoController], Nothing, UHttpApp] = ZIO.access[Has[TodoController]](_.get.routes)
}
