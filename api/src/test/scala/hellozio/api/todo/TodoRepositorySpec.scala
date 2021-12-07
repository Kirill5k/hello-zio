package hellozio.api.todo

import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.{Todo, Todos}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import zio.Runtime

class TodoRepositorySpec extends AsyncWordSpec with Matchers {

  "An InmemoryTodoRepository" should {

    "store todo in memory" in {
      val todo = Todos.genCreate()

      val result = TodoRepository(_.create(todo)) *> TodoRepository(_.getAll.map(_.head))

      Runtime.default
        .unsafeRunToFuture(result.provideLayer(TodoRepository.inmemory))
        .map { t =>
          t.task mustBe todo.task
          t.createdAt mustBe todo.createdAt
        }
    }

    "update todo" in {
      val todo = Todos.genCreate()

      val result =
        for {
          newTodo     <- TodoRepository(_.create(todo))
          _           <- TodoRepository(_.update(newTodo.copy(task = Todo.Task("updated"))))
          updatedTodo <- TodoRepository(_.get(newTodo.id))
        } yield updatedTodo

      Runtime.default
        .unsafeRunToFuture(result.provideLayer(TodoRepository.inmemory))
        .map(_.task mustBe Todo.Task("updated"))
    }

    "return error when todo does not exist on get" in {
      Runtime.default
        .unsafeRunToFuture(TodoRepository(_.get(Todos.id)).either.provideLayer(TodoRepository.inmemory))
        .map(_ mustBe Left(AppError.TodoNotFound(Todos.id)))
    }

    "return error when todo does not exist on delete" in {
      Runtime.default
        .unsafeRunToFuture(TodoRepository(_.delete(Todos.id)).either.provideLayer(TodoRepository.inmemory))
        .map(_ mustBe Left(AppError.TodoNotFound(Todos.id)))
    }

    "return error when todo does not exist on update" in {
      Runtime.default
        .unsafeRunToFuture(TodoRepository(_.update(Todos.todo)).either.provideLayer(TodoRepository.inmemory))
        .map(_ mustBe Left(AppError.TodoNotFound(Todos.id)))
    }
  }
}
