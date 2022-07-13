package hellozio.api.todo

import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.{CreateTodo, Todo, TodoUpdate}
import zio.*

trait TodoService {
  def create(todo: CreateTodo): IO[AppError, Todo.Id]
  def getAll: IO[AppError, List[Todo]]
  def get(id: Todo.Id): IO[AppError, Todo]
  def delete(id: Todo.Id): IO[AppError, Unit]
  def update(todo: Todo): IO[AppError, Unit]
}

final private case class TodoServiceLive(repository: TodoRepository, publisher: TodoPublisher) extends TodoService {
  override def getAll: IO[AppError, List[Todo]]     = repository.getAll
  override def get(id: Todo.Id): IO[AppError, Todo] = repository.get(id)
  override def create(todo: CreateTodo): IO[AppError, Todo.Id] =
    repository.create(todo).tap(todo => publisher.send(TodoUpdate.Created(todo.id, todo))).map(_.id)
  override def delete(id: Todo.Id): IO[AppError, Unit] =
    repository.delete(id).zipLeft(publisher.send(TodoUpdate.Deleted(id)))
  override def update(todo: Todo): IO[AppError, Unit] =
    repository.update(todo).zipLeft(publisher.send(TodoUpdate.Updated(todo.id, todo)))
}

object TodoService {
  val layer: URLayer[TodoRepository with TodoPublisher, TodoService] = ZLayer.fromFunction(TodoServiceLive.apply _)

  def create(todo: CreateTodo): ZIO[TodoService, AppError, Todo.Id] = ZIO.serviceWithZIO[TodoService](_.create(todo))
  def getAll: ZIO[TodoService, AppError, List[Todo]]                = ZIO.serviceWithZIO[TodoService](_.getAll)
  def get(id: Todo.Id): ZIO[TodoService, AppError, Todo]            = ZIO.serviceWithZIO[TodoService](_.get(id))
  def delete(id: Todo.Id): ZIO[TodoService, AppError, Unit]         = ZIO.serviceWithZIO[TodoService](_.delete(id))
  def update(todo: Todo): ZIO[TodoService, AppError, Unit]          = ZIO.serviceWithZIO[TodoService](_.update(todo))
}
