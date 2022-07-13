package hellozio.api.todo

import io.circe.parser._
import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.{CreateTodo, Todo, Todos}
import io.circe.{Json, ParsingFailure}
import org.http4s.*
import org.http4s.implicits.*
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import zio.*
import zio.interop.catz.*
import zio.test.*
import zio.test.Assertion.*

import java.time.Instant

object TodoControllerSpec extends ZIOSpecDefault with MockitoSugar {

  val ts = Instant.parse("2022-02-22T22:02:22Z")

  def assertEmptyResponse(res: ZIO[Any, Throwable, Response[Task]])(status: Status) =
    assertResponse(res)(status, "")

  def assertResponse(res: ZIO[Any, Throwable, Response[Task]])(status: Status, responseBody: String) =
    assertZIO(res.flatMap(r => r.as[String].map(rb => r.status -> rb)))(
      hasField[(Status, String), Status]("status", _._1, equalTo(status)) &&
        hasField[(Status, String), Either[ParsingFailure, Json]]("responseBody", r => parse(r._2), equalTo(parse(responseBody)))
    )

  def spec = suite("A TodoController when")(
    suite("GET /api/todos")(
      test("return all todos") {
        val svc = mock[TodoService]
        when(svc.getAll).thenReturn(ZIO.succeed(List(Todos.todo)))

        val req = Request[Task](uri = uri"/api/todos", method = Method.GET)
        val res = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""[{"id":"${Todos.todo.id.value}", "task":"task to do", "createdAt":"${Todos.todo.createdAt}"}]"""
        assertResponse(res)(Status.Ok, expectedRes)
      }
    ),
    suite("GET /api/todos/:id")(
      test("find todo by id") {
        val svc = mock[TodoService]
        when(svc.get(Todos.id)).thenReturn(ZIO.succeed(Todos.todo))

        val req = Request[Task](uri = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}"), method = Method.GET)
        val res = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"id":"${Todos.todo.id.value}", "task":"task to do", "createdAt":"${Todos.todo.createdAt}"}"""
        assertResponse(res)(Status.Ok, expectedRes)
      },
      test("return 404 when todo does not exist") {
        val svc = mock[TodoService]
        when(svc.get(Todos.id)).thenReturn(ZIO.fail(AppError.TodoNotFound(Todos.id)))

        val req = Request[Task](uri = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}"), method = Method.GET)
        val res = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"message":"Todo with id ${Todos.id.value} does not exist"}"""
        assertResponse(res)(Status.NotFound, expectedRes)
      }
    ),
    suite("POST /api/todos")(
      test("create new todo") {
        val svc = mock[TodoService]
        when(svc.create(CreateTodo(Todo.Task("task todo"), ts))).thenReturn(ZIO.succeed(Todos.id))

        val reqBody = """{"task":"task todo"}"""
        val req     = Request[Task](uri = uri"/api/todos", method = Method.POST).withEntity(reqBody)
        val res     = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"id":"${Todos.todo.id.value}"}"""
        assertResponse(res)(Status.Created, expectedRes)
      }
    ),
    suite("PUT /api/todos/:id")(
      test("update existing todo") {
        val svc = mock[TodoService]
        when(svc.update(Todo(Todos.id, Todo.Task("update to do"), ts))).thenReturn(ZIO.unit)

        val url     = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}")
        val reqBody = s"""{"id":"${Todos.id.value}","task":"update to do","createdAt":"$ts"}"""
        val req     = Request[Task](uri = url, method = Method.PUT).withEntity(reqBody)
        val res     = routes(svc).flatMap(_.orNotFound.run(req))
        assertEmptyResponse(res)(Status.NoContent)
      },
      test("return 404 when todo does not exist") {
        val svc = mock[TodoService]
        when(svc.update(Todos.todo)).thenReturn(ZIO.fail(AppError.TodoNotFound(Todos.id)))

        val url     = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}")
        val reqBody = s"""{"id":"${Todos.id.value}","task":"${Todos.todo.task.value}","createdAt":"${Todos.todo.createdAt}"}"""
        val req     = Request[Task](uri = url, method = Method.PUT).withEntity(reqBody)
        val res     = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"message":"Todo with id ${Todos.id.value} does not exist"}"""
        assertResponse(res)(Status.NotFound, expectedRes)
      },
      test("return 400 when ids do not match") {
        val url     = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}")
        val reqBody = s"""{"id":"${Todos.id.value}1","task":"update to do","createdAt":"$ts"}"""
        val req     = Request[Task](uri = url, method = Method.PUT).withEntity(reqBody)
        val res     = routes(mock[TodoService]).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"message":"id in path is different from id in request body"}"""
        assertResponse(res)(Status.BadRequest, expectedRes)
      }
    ),
    suite("DELETE /api/todos/:id")(
      test("delete existing todo and return 204") {
        val svc = mock[TodoService]
        when(svc.delete(Todos.id)).thenReturn(ZIO.unit)

        val req = Request[Task](uri = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}"), method = Method.DELETE)
        val res = routes(svc).flatMap(_.orNotFound.run(req))

        assertEmptyResponse(res)(Status.NoContent)
      },
      test("return 404 when todo does not exist") {
        val svc = mock[TodoService]
        when(svc.delete(Todos.id)).thenReturn(ZIO.fail(AppError.TodoNotFound(Todos.id)))

        val req = Request[Task](uri = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}"), method = Method.DELETE)
        val res = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"message":"Todo with id ${Todos.id.value} does not exist"}"""
        assertResponse(res)(Status.NotFound, expectedRes)
      }
    )
  )

  def routes(service: TodoService): UIO[HttpRoutes[Task]] = {
    val clock = mock[Clock]
    when(clock.instant(ArgumentMatchers.any[Trace]())).thenReturn(ZIO.succeed(ts))

    TodoController.routes
      .provideLayer(ZLayer.succeed(service) ++ ZLayer.succeed(clock) >>> TodoController.layer)
  }
}
