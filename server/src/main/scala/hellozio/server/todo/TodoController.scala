package hellozio.server.todo

import hellozio.server.todo.TodoController.CreateTodoRequest
import hellozio.server.todo.TodoController.CreateTodoResponse
import hellozio.server.todo.TodoController.ErrorResponse
import io.circe.generic.auto._
import java.time.Instant
import sttp.model.StatusCode
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import sttp.tapir.ztapir._
import zhttp.http.RHttpApp
import zio.Has
import zio.URIO
import zio.URLayer
import zio.ZIO
import zio.ZLayer

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

  private val addTodo = endpoint
    .post
    .in(basepath)
    .in(jsonBody[CreateTodoRequest])
    .errorOut(error)
    .out(statusCode(StatusCode.Created).and(jsonBody[CreateTodoResponse]))

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
          .flatMap {
            case Some(todo) =>
              ZIO.succeed(todo)
            case None =>
              ZIO.fail(ErrorResponse.NotFound(s"Todo with id $id does not exist"))
          }
          .either
      } <>
      ZioHttpInterpreter().toHttp(addTodo) { req =>
        service
          .create(CreateTodo(Todo.Task(req.task), Instant.now()))
          .mapError(e => ErrorResponse.InternalError(e.message))
          .map(CreateTodoResponse)
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

  final case class CreateTodoRequest(task: String)
  final case class CreateTodoResponse(id: Todo.Id)

  val layer: URLayer[Has[TodoService], Has[TodoController]] = ZLayer
    .fromService[TodoService, TodoController](TodoControllerLive)

  def routes: URIO[Has[TodoController], RHttpApp[Any]] = ZIO.access[Has[TodoController]](_.get.routes)
}
