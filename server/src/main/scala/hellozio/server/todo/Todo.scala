package hellozio.server.todo

import java.time.Instant

final case class Todo(
    id: Todo.Id,
    task: Todo.Task,
    createdAt: Instant
)

object Todo {
  final case class Id(value: String)   extends AnyVal
  final case class Task(value: String) extends AnyVal
}

final case class CreateTodo(
    task: Todo.Task,
    createdAt: Instant
)

sealed trait TodoUpdate {
  def id: Todo.Id
}

object TodoUpdate {
  final case class Created(id: Todo.Id, todo: Todo)  extends TodoUpdate
  final case class Updated(id: Todo.Id, todo: Todo)  extends TodoUpdate
  final case class Deleted(id: Todo.Id) extends TodoUpdate
}
