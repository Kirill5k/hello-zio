package hellozio.api.todo

import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.Todos
import org.mockito.Mockito.when
import org.http4s._
import org.http4s.implicits._
import org.scalatestplus.mockito.MockitoSugar
import zio.interop.catz._
import zio._

class TodoControllerSpec extends ControllerSpec with MockitoSugar {

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
        pending
      }
    }

    "PUT /api/todos/:id" should {
      "update existing todo" in {
        pending
      }

      "return 404 when todo does not exist" in {
        pending
      }
    }

    "DELETE /api/todos/:id" should {
      "delete existing todo and return 204" in {
        val svc = mock[TodoService]
        when(svc.delete(Todos.id)).thenReturn(IO.succeed(Todos.todo))

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

  def routes(service: TodoService): UIO[HttpRoutes[RIO[Clock, *]]] =
    TodoController.routes
      .provideLayer(ZLayer.succeed(service) ++ Clock.live >>> TodoController.layer)
}
