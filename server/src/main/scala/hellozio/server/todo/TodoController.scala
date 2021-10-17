package hellozio.server.todo

import hellozio.server.todo.TodoController.ErrorResponse
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
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

  private val error = oneOf[ErrorResponse](
    oneOfMapping(StatusCode.NotFound, jsonBody[ErrorResponse.NotFound]),
    oneOfMapping(StatusCode.InternalServerError, jsonBody[ErrorResponse.InternalError]),
    oneOfDefaultMapping(jsonBody[ErrorResponse.Unknown])
  )

  private val getAllTodos = endpoint
    .get
    .in(basepath)
    .errorOut(error)
    .out(jsonBody[List[Todo]])
    .zServerLogic { _ =>
      service.getAll.mapError(e => ErrorResponse.InternalError(e.message))
    }

  private val getTodo = endpoint
    .get
    .in(basepath / path[String].map(Todo.Id)(_.value))
    .errorOut(error)
    .out(jsonBody[Todo])
    .zServerLogic { id =>
      service
        .get(id)
        .mapError(e => ErrorResponse.InternalError(e.message))
        .flatMap(_.fold(ZIO.fail(ErrorResponse.NotFound(s"Todo with id $id does not exist")))(ZIO.succeed(_)))
    }

  override def routes: UHttpApp = ???
}

object TodoController {

  sealed trait ErrorResponse extends Throwable {
    def message: String
  }

  object ErrorResponse {
    final case class InternalError(message: String) extends ErrorResponse
    final case class NotFound(message: String)      extends ErrorResponse
    final case class Unknown(message: String)       extends ErrorResponse
  }

  val live: URLayer[Has[TodoService], Has[TodoController]] = ZLayer
    .fromService[TodoService, TodoController](TodoControllerLive)

  def routes: URIO[Has[TodoController], UHttpApp] = ZIO.access[Has[TodoController]](_.get.routes)
}
