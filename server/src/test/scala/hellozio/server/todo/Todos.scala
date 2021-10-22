package hellozio.server.todo

import java.time.Instant
import java.util.UUID

object Todos {

  lazy val id: Todo.Id = genId

  def genId: Todo.Id = Todo.Id(UUID.randomUUID().toString)

  def genCreate(task: Todo.Task = Todo.Task("task to do")): CreateTodo = CreateTodo(task, Instant.now())

  def gen(
      id: Todo.Id = genId,
      task: Todo.Task = Todo.Task("task to do")
  ): Todo = Todo(id, task, Instant.now())
}
