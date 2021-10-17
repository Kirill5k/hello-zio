package hellozio.server.todo

import hellozio.server.errors.AppError
import zio.Function1ToLayerSyntax
import zio.Has
import zio.IO
import zio.URLayer
import zio.ZIO

trait TodoService {
  def create(todo: CreateTodo): IO[AppError, Todo.Id]
  def getAll: IO[AppError, List[Todo]]
  def get(id: Todo.Id): IO[AppError, Option[Todo]]
}

final private case class TodoServiceLive(repository: TodoRepository) extends TodoService {
  override def create(todo: CreateTodo): IO[AppError, Todo.Id] = repository.create(todo)
  override def getAll: IO[AppError, List[Todo]]                = repository.getAll
  override def get(id: Todo.Id): IO[AppError, Option[Todo]]    = repository.get(id)
}

object TodoService {
  val layer: URLayer[Has[TodoRepository], Has[TodoService]] = (TodoServiceLive(_)).toLayer

  def create(todo: CreateTodo): ZIO[Has[TodoService], AppError, Todo.Id] = ZIO.serviceWith[TodoService](_.create(todo))
  def getAll: ZIO[Has[TodoService], AppError, List[Todo]]                = ZIO.serviceWith[TodoService](_.getAll)
  def get(id: Todo.Id): ZIO[Has[TodoService], AppError, Option[Todo]]    = ZIO.serviceWith[TodoService](_.get(id))
}
