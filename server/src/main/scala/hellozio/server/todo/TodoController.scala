package hellozio.server.todo

import io.circe.generic.auto._
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.ztapir._
import zhttp.http.UHttpApp
import zio.Has
import zio.URIO
import zio.URLayer
import zio.ZIO
import zio.ZLayer

trait TodoController {
  def routes: UHttpApp
}

final private case class TodoControllerLive(service: TodoService) extends TodoController with SchemaDerivation {
  private val basepath = "api" / "todos"

  private val getAllTodos = endpoint
    .get
    .in(basepath)
    .out(jsonBody[List[Todo]])

  private val getTodo = endpoint
    .get
    .in(basepath / path[String])
    .out(jsonBody[Todo])

  override def routes: UHttpApp = ???
}

object TodoController {

  sealed trait ErrorResponse {
    def message
  }

  object ErrorResponse {

  }

  val live: URLayer[Has[TodoService], Has[TodoController]] = ZLayer
    .fromService[TodoService, TodoController](TodoControllerLive)

  def routes: URIO[Has[TodoController], UHttpApp] = ZIO.access[Has[TodoController]](_.get.routes)
}
