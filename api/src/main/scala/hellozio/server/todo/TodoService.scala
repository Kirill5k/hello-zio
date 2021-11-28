package hellozio.server.todo

import hellozio.server.common.errors.AppError
import zio.Function1ToLayerSyntax
import zio.Has
import zio.IO
import zio.URLayer
import zio.ZIO

trait TodoService {
  def create(todo: CreateTodo): IO[AppError, Todo.Id]
  def getAll: IO[AppError, List[Todo]]
  def get(id: Todo.Id): IO[AppError, Todo]
  def delete(id: Todo.Id): IO[AppError, Unit]
  def update(todo: Todo): IO[AppError, Unit]
}

final private case class TodoServiceLive(repository: TodoRepository) extends TodoService {
  override def create(todo: CreateTodo): IO[AppError, Todo.Id] = repository.create(todo).map(_.id)
  override def getAll: IO[AppError, List[Todo]]                = repository.getAll
  override def get(id: Todo.Id): IO[AppError, Todo]            = repository.get(id)
  override def delete(id: Todo.Id): IO[AppError, Unit]         = repository.delete(id)
  override def update(todo: Todo): IO[AppError, Unit]          = repository.update(todo)
}

object TodoService {
  val layer: URLayer[Has[TodoRepository], Has[TodoService]] = (TodoServiceLive(_)).toLayer

  def create(todo: CreateTodo): ZIO[Has[TodoService], AppError, Todo.Id] = ZIO.serviceWith[TodoService](_.create(todo))
  def getAll: ZIO[Has[TodoService], AppError, List[Todo]]                = ZIO.serviceWith[TodoService](_.getAll)
  def get(id: Todo.Id): ZIO[Has[TodoService], AppError, Todo]            = ZIO.serviceWith[TodoService](_.get(id))
  def delete(id: Todo.Id): ZIO[Has[TodoService], AppError, Unit]         = ZIO.serviceWith[TodoService](_.delete(id))
  def update(todo: Todo): ZIO[Has[TodoService], AppError, Unit]          = ZIO.serviceWith[TodoService](_.update(todo))
}
