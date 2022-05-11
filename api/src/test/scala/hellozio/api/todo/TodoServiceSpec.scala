package hellozio.api.todo

import hellozio.domain.todo.{TodoUpdate, Todos}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import zio.Runtime
import zio._

class TodoServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  "A TodoService" should {

    "delete todo" in {
      val (repo, pub) = mocks
      when(repo.delete(Todos.id)).thenReturn(ZIO.unit)
      when(pub.send(TodoUpdate.Deleted(Todos.id))).thenReturn(ZIO.unit)

      Runtime.default
        .unsafeRunToFuture(TodoService.delete(Todos.id).provideLayer(mockLayer(repo, pub)))
        .map(_ mustBe (()))
    }

    "update todo" in {
      val (repo, pub) = mocks
      when(repo.update(Todos.todo)).thenReturn(ZIO.unit)
      when(pub.send(TodoUpdate.Updated(Todos.id, Todos.todo))).thenReturn(ZIO.unit)

      Runtime.default
        .unsafeRunToFuture(TodoService.update(Todos.todo).provideLayer(mockLayer(repo, pub)))
        .map(_ mustBe (()))
    }

    "get todos from repository" in {
      val (repo, pub) = mocks
      when(repo.getAll).thenReturn(ZIO.succeed(List(Todos.todo)))

      Runtime.default
        .unsafeRunToFuture(TodoService.getAll.provideLayer(mockLayer(repo, pub)))
        .map(_ mustBe List(Todos.todo))
    }

    "get todo by id" in {
      val (repo, pub) = mocks
      when(repo.get(Todos.id)).thenReturn(ZIO.succeed(Todos.todo))

      Runtime.default
        .unsafeRunToFuture(TodoService.get(Todos.id).provideLayer(mockLayer(repo, pub)))
        .map(_ mustBe Todos.todo)
    }

    "create new todo" in {
      val (repo, pub) = mocks
      when(repo.create(Todos.create)).thenReturn(ZIO.succeed(Todos.todo))
      when(pub.send(TodoUpdate.Created(Todos.id, Todos.todo))).thenReturn(ZIO.unit)

      Runtime.default
        .unsafeRunToFuture(TodoService.create(Todos.create).provideLayer(mockLayer(repo, pub)))
        .map(_ mustBe Todos.id)
    }
  }

  def mocks: (TodoRepository, TodoPublisher) =
    (mock[TodoRepository], mock[TodoPublisher])

  def mockLayer(repo: TodoRepository, publisher: TodoPublisher): ULayer[TodoService] =
    (ZLayer.succeed(repo) ++ ZLayer.succeed(publisher)) >>> TodoService.layer
}
