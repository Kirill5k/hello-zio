package hellozio.api.todo

import hellozio.api.todo.TodoController.{CreateTodoRequest, CreateTodoResponse, ErrorResponse}
import hellozio.api.todo.TodoController.ErrorResponse.BadRequest
import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.*
import io.circe.{Codec, CursorOp, Decoder, DecodingFailure, Encoder, Json}
import io.circe.syntax.*
import org.http4s.HttpRoutes
import sttp.model.StatusCode
import sttp.tapir.Schema
import sttp.tapir.generic.auto.SchemaDerivation
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.ztapir.*
import zio.*

trait TodoController {
  def routes: HttpRoutes[Task]
}

final private case class TodoControllerLive(service: TodoService, clock: Clock) extends TodoController with SchemaDerivation {
  inline given Schema[Todo.Id]   = Schema.string
  inline given Schema[Todo.Task] = Schema.string

  private val basepath = "api" / "todos"
  private val itemPath = basepath / path[String].map(Todo.Id.apply)(_.value)

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
          .mapBoth(ErrorResponse.from, CreateTodoResponse.apply)
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

  sealed trait ErrorResponse(val kind: String) extends Throwable {
    def message: String
  }

  object ErrorResponse {
    final case class InternalError(message: String) extends ErrorResponse("internal") derives Codec.AsObject
    final case class NotFound(message: String)      extends ErrorResponse("not-found") derives Codec.AsObject
    final case class BadRequest(message: String)    extends ErrorResponse("bad-request") derives Codec.AsObject
    final case class Unknown(message: String)       extends ErrorResponse("unknown") derives Codec.AsObject

    def from(err: AppError): ErrorResponse =
      err match {
        case e: AppError.TodoNotFound => ErrorResponse.NotFound(e.message)
        case e                        => ErrorResponse.InternalError(e.message)
      }

    private val discriminatorField: String = "kind"

    inline given Encoder[ErrorResponse] = Encoder.instance {
      case e: NotFound      => Json.obj(discriminatorField -> Json.fromString(e.kind)).deepMerge(e.asJson)
      case e: InternalError => Json.obj(discriminatorField -> Json.fromString(e.kind)).deepMerge(e.asJson)
      case e: BadRequest    => Json.obj(discriminatorField -> Json.fromString(e.kind)).deepMerge(e.asJson)
      case e: Unknown       => Json.obj(discriminatorField -> Json.fromString(e.kind)).deepMerge(e.asJson)
    }

    inline given Decoder[ErrorResponse] = Decoder.instance { c =>
      c.downField(discriminatorField).as[String].flatMap {
        case "not-found"   => c.as[NotFound]
        case "bad-request" => c.as[BadRequest]
        case "unknown"     => c.as[Unknown]
        case "internal"    => c.as[InternalError]
        case kind          => Left(DecodingFailure(s"Unexpected error response kind $kind", List(CursorOp.Field(discriminatorField))))
      }
    }
  }

  final case class CreateTodoRequest(task: String) derives Codec.AsObject
  final case class CreateTodoResponse(id: Todo.Id) derives Codec.AsObject

  val layer: URLayer[TodoService with Clock, TodoController] = ZLayer.fromFunction(TodoControllerLive.apply _)

  def routes: URIO[TodoController, HttpRoutes[Task]] = ZIO.serviceWith[TodoController](_.routes)

}
