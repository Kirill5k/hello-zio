package hellozio.api.todo

import hellozio.domain.todo._
import hellozio.domain.common.errors.AppError
import hellozio.api.todo.TodoController.CreateTodoRequest
import hellozio.api.todo.TodoController.CreateTodoResponse
import hellozio.api.todo.TodoController.ErrorResponse
import hellozio.api.todo.TodoController.ErrorResponse.BadRequest
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.ztapir._
import zio._

trait TodoController {
  def routes: HttpRoutes[RIO[Clock, *]]
}

final private case class TodoControllerLive(service: TodoService, clock: Clock) extends TodoController with SchemaDerivation {
  implicit val todoIdEncoder: Encoder[Todo.Id]     = deriveUnwrappedEncoder
  implicit val todoTaskEncoder: Encoder[Todo.Task] = deriveUnwrappedEncoder

  private val basepath = "api" / "todos"
  private val itemPath = basepath / path[String].map(Todo.Id)(_.value)

  private val error = oneOf[ErrorResponse](
    oneOfVariant(StatusCode.BadRequest, jsonBody[ErrorResponse.BadRequest]),
    oneOfVariant(StatusCode.NotFound, jsonBody[ErrorResponse.NotFound]),
    oneOfVariant(StatusCode.InternalServerError, jsonBody[ErrorResponse.InternalError]),
    oneOfDefaultVariant(jsonBody[ErrorResponse.Unknown])
  )

  private val getAllTodos: ZServerEndpoint[Any, Any] = endpoint.get
    .in(basepath)
    .errorOut(error)
    .out(jsonBody[List[Todo]])
    .zServerLogic { _ =>
      service.getAll
        .mapError(ErrorResponse.from)
    }

  private val getTodo: ZServerEndpoint[Any, Any] = endpoint.get
    .in(itemPath)
    .errorOut(error)
    .out(jsonBody[Todo])
    .zServerLogic { id =>
      service
        .get(id)
        .mapError(ErrorResponse.from)
    }

  private val addTodo: ZServerEndpoint[Any, Any] = endpoint.post
    .in(basepath)
    .in(jsonBody[CreateTodoRequest])
    .errorOut(error)
    .out(statusCode(StatusCode.Created).and(jsonBody[CreateTodoResponse]))
    .zServerLogic { req =>
      clock.currentDateTime.map(_.toInstant).flatMap { now =>
        service
          .create(CreateTodo(Todo.Task(req.task), now))
          .mapError(ErrorResponse.from)
          .map(CreateTodoResponse)
      }
    }

  private val deleteTodo: ZServerEndpoint[Any, Any] = endpoint.delete
    .in(itemPath)
    .errorOut(error)
    .out(statusCode(StatusCode.NoContent))
    .zServerLogic { id =>
      service
        .delete(id)
        .mapError(ErrorResponse.from)
    }

  private val updateTodo: ZServerEndpoint[Any, Any] = endpoint.put
    .in(itemPath)
    .in(jsonBody[Todo])
    .errorOut(error)
    .out(statusCode(StatusCode.NoContent))
    .zServerLogic { case (id, todo) =>
      ZIO
        .cond(id == todo.id, todo, BadRequest("id in path is different from id in request body"))
        .flatMap(todo => service.update(todo).mapError(ErrorResponse.from))
    }

  override def routes: HttpRoutes[RIO[Clock, *]] =
    ZHttp4sServerInterpreter()
      .from(
        List(
          getAllTodos,
          getTodo,
          addTodo,
          deleteTodo,
          updateTodo
        )
      )
      .toRoutes

}

object TodoController {

  sealed trait ErrorResponse extends Throwable {
    def message: String
  }

  object ErrorResponse {
    final case class InternalError(message: String) extends ErrorResponse
    final case class NotFound(message: String)      extends ErrorResponse
    final case class BadRequest(message: String)    extends ErrorResponse
    final case class Unknown(message: String)       extends ErrorResponse

    def from(err: AppError): ErrorResponse =
      err match {
        case e: AppError.TodoNotFound => ErrorResponse.NotFound(e.message)
        case e                        => ErrorResponse.InternalError(e.message)
      }

  }

  final case class CreateTodoRequest(task: String)
  final case class CreateTodoResponse(id: Todo.Id)

  lazy val layer: URLayer[TodoService with Clock, TodoController] = (TodoControllerLive(_, _)).toLayer

  def routes: URIO[TodoController, HttpRoutes[RIO[Clock, *]]] = ZIO.serviceWith[TodoController](_.routes)

}
