package hellozio.server.todo

import hellozio.server.AppError
import zio.Function1ToLayerSyntax
import zio.Has
import zio.IO
import zio.ZIO
import zio.ZLayer

trait TodoService {
  def create(todo: CreateTodo): IO[AppError, Unit]
  def getAll: IO[AppError, List[Todo]]
}

final case class TodoServiceLive(repository: TodoRepository) extends TodoService {
  override def create(todo: CreateTodo): IO[AppError, Unit] = repository.create(todo)
  override def getAll: IO[AppError, List[Todo]]             = repository.getAll
}

object TodoService {
  val layer: ZLayer[Has[TodoRepository], AppError, Has[TodoService]] = (TodoServiceLive(_)).toLayer

  def create(todo: CreateTodo): ZIO[Has[TodoService], AppError, Unit] = ZIO.serviceWith[TodoService](_.create(todo))
  def getAll: ZIO[Has[TodoService], AppError, List[Todo]]             = ZIO.serviceWith[TodoService](_.getAll)
}
