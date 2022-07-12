package hellozio.domain.todo

import hellozio.domain.common.types.StringType

import java.time.Instant

final case class Todo(
    id: Todo.Id,
    task: Todo.Task,
    createdAt: Instant
)

object Todo {
  opaque type Id = String
  object Id extends StringType[Id]
  opaque type Task = String
  object Task extends StringType[Task]
}

final case class CreateTodo(
    task: Todo.Task,
    createdAt: Instant
)

sealed trait TodoUpdate {
  def id: Todo.Id
}

object TodoUpdate {
  final case class Created(id: Todo.Id, todo: Todo) extends TodoUpdate
  final case class Updated(id: Todo.Id, todo: Todo) extends TodoUpdate
  final case class Deleted(id: Todo.Id)             extends TodoUpdate
}
