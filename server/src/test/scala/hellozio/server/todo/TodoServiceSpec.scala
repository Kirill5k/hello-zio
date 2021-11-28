package hellozio.server.todo

import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import zio.Has
import zio.Runtime
import zio.ULayer
import zio.ZIO
import zio.ZLayer

class TodoServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  "A TodoService" should {

    "delete todo" in {
      val repo = mock[TodoRepository]
      when(repo.delete(Todos.id)).thenReturn(ZIO.unit)

      Runtime
        .default
        .unsafeRunToFuture(TodoService.delete(Todos.id).provideLayer(mockLayer(repo)))
        .map(_ mustBe (()))
    }

    "update todo" in {
      val repo = mock[TodoRepository]
      when(repo.update(Todos.todo)).thenReturn(ZIO.unit)

      Runtime
        .default
        .unsafeRunToFuture(TodoService.update(Todos.todo).provideLayer(mockLayer(repo)))
        .map(_ mustBe (()))
    }

    "get todos from repository" in {
      val repo = mock[TodoRepository]
      when(repo.getAll).thenReturn(ZIO.succeed(List(Todos.todo)))

      Runtime
        .default
        .unsafeRunToFuture(TodoService.getAll.provideLayer(mockLayer(repo)))
        .map(_ mustBe List(Todos.todo))
    }

    "get todo by id" in {
      val repo = mock[TodoRepository]
      when(repo.get(Todos.id)).thenReturn(ZIO.succeed(Todos.todo))

      Runtime
        .default
        .unsafeRunToFuture(TodoService.get(Todos.id).provideLayer(mockLayer(repo)))
        .map(_ mustBe Todos.todo)
    }

    "create new todo" in {
      val repo = mock[TodoRepository]
      when(repo.create(Todos.create)).thenReturn(ZIO.succeed(Todos.todo))

      Runtime
        .default
        .unsafeRunToFuture(TodoService.create(Todos.create).provideLayer(mockLayer(repo)))
        .map(_ mustBe Todos.id)
    }
  }

  def mockLayer(repo: TodoRepository): ULayer[Has[TodoService]] = ZLayer.succeed(repo) >>> TodoService.layer
}
