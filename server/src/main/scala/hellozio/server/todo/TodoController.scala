package hellozio.server.todo

import hellozio.server.common.errors.AppError
import hellozio.server.todo.TodoController.CreateTodoRequest
import hellozio.server.todo.TodoController.CreateTodoResponse
import hellozio.server.todo.TodoController.ErrorResponse
import io.circe.Encoder
import io.circe.generic.auto._
import io.circe.generic.extras.semiauto._
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir.generic.SchemaDerivation
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.ztapir._
import zio.Has
import zio.RIO
import zio.URIO
import zio.URLayer
import zio.ZIO
import zio.blocking.Blocking
import zio.clock.Clock

trait TodoController {
  def routes: HttpRoutes[RIO[Clock with Blocking, *]]
}

final private case class TodoControllerLive(service: TodoService, clock: Clock.Service) extends TodoController with SchemaDerivation {
  implicit val todoIdEncoder: Encoder[Todo.Id]     = deriveUnwrappedEncoder
  implicit val todoTaskEncoder: Encoder[Todo.Task] = deriveUnwrappedEncoder

  private val basepath = "api" / "todos"
  private val itemPath = basepath / path[String].map(Todo.Id)(_.value)

  private val error = oneOf[ErrorResponse](
    oneOfMapping(StatusCode.NotFound, jsonBody[ErrorResponse.NotFound]),
    oneOfMapping(StatusCode.InternalServerError, jsonBody[ErrorResponse.InternalError]),
    oneOfDefaultMapping(jsonBody[ErrorResponse.Unknown])
  )

  private val getAllTodos: ZServerEndpoint[Any, Unit, ErrorResponse, List[Todo], Any] = endpoint
    .get
    .in(basepath)
    .errorOut(error)
    .out(jsonBody[List[Todo]])
    .zServerLogic { _ =>
      service
        .getAll
        .mapError(ErrorResponse.from)
    }

  private val getTodo: ZServerEndpoint[Any, Todo.Id, ErrorResponse, Todo, Any] = endpoint
    .get
    .in(itemPath)
    .errorOut(error)
    .out(jsonBody[Todo])
    .zServerLogic { id =>
      service
        .get(id)
        .mapError(ErrorResponse.from)
    }

  private val addTodo: ZServerEndpoint[Any, CreateTodoRequest, ErrorResponse, CreateTodoResponse, Any] = endpoint
    .post
    .in(basepath)
    .in(jsonBody[CreateTodoRequest])
    .errorOut(error)
    .out(statusCode(StatusCode.Created).and(jsonBody[CreateTodoResponse]))
    .zServerLogic { req =>
      clock.currentDateTime.map(_.toInstant).orDie.flatMap { now =>
        service
          .create(CreateTodo(Todo.Task(req.task), now))
          .mapError(ErrorResponse.from)
          .map(CreateTodoResponse)
      }
    }

  private val deleteTodo: ZServerEndpoint[Any, Todo.Id, ErrorResponse, Unit, Any] = endpoint
    .delete
    .in(itemPath)
    .errorOut(error)
    .out(statusCode(StatusCode.NoContent))
    .zServerLogic { id =>
      service
        .delete(id)
        .mapError(ErrorResponse.from)
    }

  override def routes: HttpRoutes[RIO[Clock with Blocking, *]] =
    ZHttp4sServerInterpreter[Any]()
      .from(
        List(
          getAllTodos.widen[Any],
          getTodo.widen[Any],
          addTodo.widen[Any],
          deleteTodo.widen[Any]
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
    final case class Unknown(message: String)       extends ErrorResponse

    def from(err: AppError): ErrorResponse =
      err match {
        case e: AppError.TodoNotFound =>
          ErrorResponse.NotFound(e.message)
        case e =>
          ErrorResponse.InternalError(e.message)
      }

  }

  final case class CreateTodoRequest(task: String)
  final case class CreateTodoResponse(id: Todo.Id)

  val layer: URLayer[Has[TodoService] with Clock, Has[TodoController]] = (TodoControllerLive(_, _)).toLayer

  def routes: URIO[Has[TodoController], HttpRoutes[RIO[Clock with Blocking, *]]] = ZIO
    .access[Has[TodoController]](_.get.routes)

}
