package hellozio.api.todo

import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.{CreateTodo, Todo, TodoUpdate}
import zio.{Accessible, Function2ToLayerSyntax, Has, IO, URLayer}

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
    repository.delete(id).tap(_ => publisher.send(TodoUpdate.Deleted(id)))
  override def update(todo: Todo): IO[AppError, Unit] =
    repository.update(todo).tap(_ => publisher.send(TodoUpdate.Updated(todo.id, todo)))
}

object TodoService extends Accessible[TodoService] {
  lazy val layer: URLayer[Has[TodoRepository] with Has[TodoPublisher], Has[TodoService]] = (TodoServiceLive(_, _)).toLayer
}
