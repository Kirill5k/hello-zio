package hellozio.server.todo

import hellozio.server.todo.TodoController.ErrorResponse
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir._
import zhttp.http.RHttpApp
import zio.{Has, URIO, URLayer, ZIO, ZLayer}

trait TodoController {
  def routes: RHttpApp[Any]
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

  private val getTodo = endpoint
    .get
    .in(basepath / path[String].map(Todo.Id)(_.value))
    .errorOut(error)
    .out(jsonBody[Todo])

  override def routes: RHttpApp[Any] =
    ZioHttpInterpreter().toHttp(getAllTodos) { _ =>
      service
        .getAll
        .mapError(e => ErrorResponse.InternalError(e.message))
        .either
    } <>
      ZioHttpInterpreter().toHttp(getTodo) { id =>
        service
          .get(id)
          .mapError(e => ErrorResponse.InternalError(e.message))
          .flatMap(_.fold(ZIO.fail(ErrorResponse.NotFound(s"Todo with id $id does not exist")))(ZIO.succeed(_)))
          .either
      }

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

  def routes: URIO[Has[TodoController], RHttpApp[Any]] = ZIO.access[Has[TodoController]](_.get.routes)
}
