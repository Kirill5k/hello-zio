package hellozio.api.todo

import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.{Todo, Todos}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import zio.{Runtime, Unsafe, ZIO}

import scala.concurrent.Future

class TodoRepositorySpec extends AsyncWordSpec with Matchers {

  "An InmemoryTodoRepository" should {

    "store todo in memory" in {
      val todo = Todos.genCreate()

      val result = TodoRepository.create(todo) *> TodoRepository.getAll.map(_.head)

      toFuture(result.provide(TodoRepository.inmemory))
        .map { t =>
          t.task mustBe todo.task
          t.createdAt mustBe todo.createdAt
        }
    }

    "update todo" in {
      val todo = Todos.genCreate()

      val result =
        for {
          newTodo     <- TodoRepository.create(todo)
          _           <- TodoRepository.update(newTodo.copy(task = Todo.Task("updated")))
          updatedTodo <- TodoRepository.get(newTodo.id)
        } yield updatedTodo

      toFuture(result.provide(TodoRepository.inmemory))
        .map(_.task mustBe Todo.Task("updated"))
    }

    "return error when todo does not exist on get" in {
      toFuture(TodoRepository.get(Todos.id).either.provide(TodoRepository.inmemory))
        .map(_ mustBe Left(AppError.TodoNotFound(Todos.id)))
    }

    "return error when todo does not exist on delete" in {
      toFuture(TodoRepository.delete(Todos.id).either.provide(TodoRepository.inmemory))
        .map(_ mustBe Left(AppError.TodoNotFound(Todos.id)))
    }

    "return error when todo does not exist on update" in {
      toFuture(TodoRepository.update(Todos.todo).either.provide(TodoRepository.inmemory))
        .map(_ mustBe Left(AppError.TodoNotFound(Todos.id)))
    }
  }

  def toFuture[E <: Throwable, A](zio: ZIO[Any, E, A]): Future[A] =
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.runToFuture(zio).future
    }
}
