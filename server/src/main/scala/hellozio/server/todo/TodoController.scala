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
import zio.ZLayer
import zio.blocking.Blocking
import zio.clock.Clock
import zio.clock.currentDateTime
import zio.interop.catz._

trait TodoController {
  def routes: HttpRoutes[RIO[Nothing with Clock with Blocking, *]]
}

final private case class TodoControllerLive(service: TodoService) extends TodoController with SchemaDerivation {
  implicit val todoIdEncoder: Encoder[Todo.Id]     = deriveUnwrappedEncoder
  implicit val todoTaskEncoder: Encoder[Todo.Task] = deriveUnwrappedEncoder

  private val basepath = "api" / "todos"
  private val itemPath = basepath / path[String].map(Todo.Id)(_.value)

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
      service
        .getAll
        .mapError(ErrorResponse.from)
        .either
    }

  private val getTodo = endpoint
    .get
    .in(itemPath)
    .errorOut(error)
    .out(jsonBody[Todo])
    .zServerLogic { id =>
      service
        .get(id)
        .mapError(ErrorResponse.from)
        .either
    }

  private val addTodo = endpoint
    .post
    .in(basepath)
    .in(jsonBody[CreateTodoRequest])
    .errorOut(error)
    .out(statusCode(StatusCode.Created).and(jsonBody[CreateTodoResponse]))
    .zServerLogic { req =>
      currentDateTime.map(_.toInstant).orDie.flatMap { now =>
        service
          .create(CreateTodo(Todo.Task(req.task), now))
          .mapError(ErrorResponse.from)
          .map(CreateTodoResponse)
          .either
      }
    }

  private val deleteTodo = endpoint
    .delete
    .in(itemPath)
    .errorOut(error)
    .out(statusCode(StatusCode.NoContent))
    .zServerLogic { id =>
      service
        .delete(id)
        .mapError(ErrorResponse.from)
        .either
    }

  override def routes: HttpRoutes[RIO[Nothing with Clock with Blocking, *]] =
    ZHttp4sServerInterpreter()
      .from(
        List(
          getAllTodos,
          getTodo,
          addTodo,
          deleteTodo
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

  val layer: URLayer[Has[TodoService], Has[TodoController]] = ZLayer
    .fromService[TodoService, TodoController](TodoControllerLive)

  def routes: URIO[Has[TodoController], HttpRoutes[RIO[Nothing with Clock with Blocking, *]]] = ZIO
    .access[Has[TodoController]](_.get.routes)

}
