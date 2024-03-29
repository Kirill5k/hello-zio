package hellozio.domain.todo

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

object Todos {

  lazy val id: Todo.Id        = genId
  lazy val todo: Todo         = gen(id)
  lazy val create: CreateTodo = genCreate()

  def genId: Todo.Id = Todo.Id(UUID.randomUUID().toString)

  def genCreate(task: Todo.Task = Todo.Task("task to do")): CreateTodo = CreateTodo(task, Instant.now().truncatedTo(ChronoUnit.MILLIS))

  def gen(
      id: Todo.Id = genId,
      task: Todo.Task = Todo.Task("task to do")
  ): Todo = Todo(id, task, Instant.now())

}
