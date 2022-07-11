package hellozio.api.todo

import hellozio.api.todo.TodoController.{CreateTodoRequest, CreateTodoResponse, ErrorResponse}
import hellozio.api.todo.TodoController.ErrorResponse.BadRequest
import hellozio.domain.common.errors.AppError
import hellozio.domain.todo._
import io.circe.Codec
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir.generic.auto.SchemaDerivation
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.ztapir._
import zio._

trait TodoController {
  def routes: HttpRoutes[Task]
}

final private case class TodoControllerLive(service: TodoService, clock: Clock) extends TodoController with SchemaDerivation {
  implicit val todoIdCodec: Codec[Todo.Id]     = deriveUnwrappedCodec
  implicit val todoTaskCodec: Codec[Todo.Task] = deriveUnwrappedCodec

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
      clock.instant.flatMap { now =>
        service
          .create(CreateTodo(Todo.Task(req.task), now))
          .mapBoth(ErrorResponse.from, CreateTodoResponse)
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

  override def routes: HttpRoutes[Task] =
    ZHttp4sServerInterpreter[Any]()
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

  val layer: URLayer[TodoService with Clock, TodoController] = ZLayer.fromFunction(TodoControllerLive.apply _)

  def routes: URIO[TodoController, HttpRoutes[Task]] = ZIO.serviceWith[TodoController](_.routes)

}
