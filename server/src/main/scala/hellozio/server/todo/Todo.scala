package hellozio.server.todo

import java.time.Instant

final case class CreateTodo(
    task: String,
    createdAt: Instant
)

final case class Todo(
    id: Todo.Id,
    task: Todo.Task,
    createdAt: Instant
)

object Todo {
  final case class Id(value: String)   extends AnyVal
  final case class Task(value: String) extends AnyVal
}
