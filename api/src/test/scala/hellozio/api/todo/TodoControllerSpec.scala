package hellozio.api.todo

import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.{CreateTodo, Todo, Todos}
import org.http4s._
import org.http4s.implicits._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import zio._
import zio.interop.catz._

import java.time.Instant

class TodoControllerSpec extends ControllerSpec with MockitoSugar {

  val ts = Instant.parse("2022-02-22T22:02:22Z")

  "A TodoController" when {

    "GET /api/todos" should {
      "return all todos" in {
        val svc = mock[TodoService]
        when(svc.getAll).thenReturn(IO.succeed(List(Todos.todo)))

        val req = Request[RIO[Clock, *]](uri = uri"/api/todos", method = Method.GET)
        val res = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""[{"id":"${Todos.todo.id.value}", "task":"task to do", "createdAt":"${Todos.todo.createdAt}"}]"""
        verifyJsonResponse(res, Status.Ok, Some(expectedRes))
      }
    }

    "GET /api/todos/:id" should {
      "find todo by id" in {
        val svc = mock[TodoService]
        when(svc.get(Todos.id)).thenReturn(IO.succeed(Todos.todo))

        val req = Request[RIO[Clock, *]](uri = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}"), method = Method.GET)
        val res = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"id":"${Todos.todo.id.value}", "task":"task to do", "createdAt":"${Todos.todo.createdAt}"}"""
        verifyJsonResponse(res, Status.Ok, Some(expectedRes))
      }

      "return 404 when todo does not exist" in {
        val svc = mock[TodoService]
        when(svc.get(Todos.id)).thenReturn(IO.fail(AppError.TodoNotFound(Todos.id)))

        val req = Request[RIO[Clock, *]](uri = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}"), method = Method.GET)
        val res = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"message":"Todo with id ${Todos.id.value} does not exist"}"""
        verifyJsonResponse(res, Status.NotFound, Some(expectedRes))
      }
    }

    "POST /api/todos" should {
      "create new todo" in {
        val svc = mock[TodoService]
        when(svc.create(CreateTodo(Todo.Task("task todo"), ts))).thenReturn(IO.succeed(Todos.id))

        val reqBody = """{"task":"task todo"}"""
        val req     = Request[RIO[Clock, *]](uri = uri"/api/todos", method = Method.POST).withEntity(reqBody)
        val res     = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"id":"${Todos.todo.id.value}"}"""
        verifyJsonResponse(res, Status.Created, Some(expectedRes))
      }
    }

    "PUT /api/todos/:id" should {
      "update existing todo" in {
        val svc = mock[TodoService]
        when(svc.update(Todo(Todos.id, Todo.Task("update to do"), ts))).thenReturn(IO.unit)

        val url     = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}")
        val reqBody = s"""{"id":"${Todos.id.value}","task":"update to do","createdAt":"$ts"}"""
        val req     = Request[RIO[Clock, *]](uri = url, method = Method.PUT).withEntity(reqBody)
        val res     = routes(svc).flatMap(_.orNotFound.run(req))

        verifyJsonResponse(res, Status.NoContent, None)
      }

      "return 404 when todo does not exist" in {
        val svc = mock[TodoService]
        when(svc.update(Todos.todo)).thenReturn(IO.fail(AppError.TodoNotFound(Todos.id)))

        val url     = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}")
        val reqBody = s"""{"id":"${Todos.id.value}","task":"${Todos.todo.task.value}","createdAt":"${Todos.todo.createdAt}"}"""
        val req     = Request[RIO[Clock, *]](uri = url, method = Method.PUT).withEntity(reqBody)
        val res     = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"message":"Todo with id ${Todos.id.value} does not exist"}"""
        verifyJsonResponse(res, Status.NotFound, Some(expectedRes))
      }

      "return 400 when ids do not match" in {
        val url     = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}")
        val reqBody = s"""{"id":"${Todos.id.value}1","task":"update to do","createdAt":"$ts"}"""
        val req     = Request[RIO[Clock, *]](uri = url, method = Method.PUT).withEntity(reqBody)
        val res     = routes(mock[TodoService]).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"message":"id in path is different from id in request body"}"""
        verifyJsonResponse(res, Status.BadRequest, Some(expectedRes))
      }
    }

    "DELETE /api/todos/:id" should {
      "delete existing todo and return 204" in {
        val svc = mock[TodoService]
        when(svc.delete(Todos.id)).thenReturn(IO.unit)

        val req = Request[RIO[Clock, *]](uri = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}"), method = Method.DELETE)
        val res = routes(svc).flatMap(_.orNotFound.run(req))

        verifyJsonResponse(res, Status.NoContent, None)
      }

      "return 404 when todo does not exist" in {
        val svc = mock[TodoService]
        when(svc.delete(Todos.id)).thenReturn(IO.fail(AppError.TodoNotFound(Todos.id)))

        val req = Request[RIO[Clock, *]](uri = Uri.unsafeFromString(s"/api/todos/${Todos.id.value}"), method = Method.DELETE)
        val res = routes(svc).flatMap(_.orNotFound.run(req))

        val expectedRes = s"""{"message":"Todo with id ${Todos.id.value} does not exist"}"""
        verifyJsonResponse(res, Status.NotFound, Some(expectedRes))
      }
    }
  }

  def routes(service: TodoService): UIO[HttpRoutes[RIO[Clock, *]]] = {
    val clock = mock[Clock]
    when(clock.instant(ArgumentMatchers.any[ZTraceElement]())).thenReturn(UIO.succeed(ts))

    TodoController.routes
      .provideLayer(ZLayer.succeed(service) ++ ZLayer.succeed(clock) >>> TodoController.layer)
  }
}
