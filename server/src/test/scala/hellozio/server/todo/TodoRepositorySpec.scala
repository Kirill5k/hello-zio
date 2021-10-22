package hellozio.server.todo

import hellozio.server.common.errors.AppError
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import zio.Runtime

class TodoRepositorySpec extends AsyncWordSpec with Matchers {

  "An InmemoryTodoRepository" should {

    "store todo in memory" in {
      val todo = Todos.genCreate()

      val result = TodoRepository.create(todo) *> TodoRepository.getAll.map(_.head)

      Runtime
        .default
        .unsafeRunToFuture(result.provideLayer(TodoRepository.inmemory))
        .map { t =>
          t.task mustBe todo.task
          t.createdAt mustBe todo.createdAt
        }
    }

    "return error when todo does not exist on get" in {
      Runtime
        .default
        .unsafeRunToFuture(TodoRepository.get(Todos.id).either.provideLayer(TodoRepository.inmemory))
        .map(_ mustBe Left(AppError.TodoNotFound(Todos.id)))
    }

    "return error when todo does not exist on delete" in {
      Runtime
        .default
        .unsafeRunToFuture(TodoRepository.delete(Todos.id).either.provideLayer(TodoRepository.inmemory))
        .map(_ mustBe Left(AppError.TodoNotFound(Todos.id)))
    }
  }
}
