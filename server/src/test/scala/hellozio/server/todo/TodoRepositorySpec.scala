package hellozio.server.todo

import java.time.Instant
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import zio.Runtime

class TodoRepositorySpec extends AsyncWordSpec with Matchers {

  "An TodoRepositoryInmemory" should {

    "store todo in memory" in {
      val todo = CreateTodo(Todo.Task("task"), Instant.now())

      val result = TodoRepository.create(todo) *> TodoRepository.getAll.map(_.head)

      Runtime
        .default
        .unsafeRunToFuture(result.provideLayer(TodoRepository.inmemory))
        .map { t =>
          t.task mustBe todo.task
          t.createdAt mustBe todo.createdAt
        }
    }
  }
}
