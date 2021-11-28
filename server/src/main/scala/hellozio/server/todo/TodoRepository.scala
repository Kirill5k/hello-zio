package hellozio.server.todo

import hellozio.server.common.errors.AppError
import java.util.UUID
import zio.Has
import zio.IO
import zio.Ref
import zio.ULayer
import zio.ZIO

trait TodoRepository {
  def create(todo: CreateTodo): IO[AppError, Todo]
  def update(todo: Todo): IO[AppError, Unit]
  def get(id: Todo.Id): IO[AppError, Todo]
  def getAll: IO[AppError, List[Todo]]
  def delete(id: Todo.Id): IO[AppError, Unit]
}

final private case class TodoRepositoryInmemory(storage: Ref[Map[Todo.Id, Todo]]) extends TodoRepository {

  override def create(todo: CreateTodo): IO[AppError, Todo] = ZIO
    .effect(UUID.randomUUID().toString)
    .map(id => Todo(Todo.Id(id), todo.task, todo.createdAt))
    .tap(todo => storage.update(_ + (todo.id -> todo)))
    .mapError(e => AppError.DbError(e.getMessage))

  override def getAll: IO[AppError, List[Todo]] = storage.get.map(_.values.toList)

  override def get(id: Todo.Id): IO[AppError, Todo] = storage
    .get
    .map(_.get(id))
    .flatMap {
      case Some(todo) => ZIO.succeed(todo)
      case None       => ZIO.fail(AppError.TodoNotFound(id))
    }

  override def delete(id: Todo.Id): IO[AppError, Unit] = storage
    .get
    .filterOrFail(_.contains(id))(AppError.TodoNotFound(id))
    .flatMap(s => storage.set(s.removed(id)))

  override def update(todo: Todo): IO[AppError, Unit] = storage
    .get
    .filterOrFail(_.contains(todo.id))(AppError.TodoNotFound(todo.id))
    .flatMap(s => storage.set(s + (todo.id -> todo)))

}

object TodoRepository {

  val inmemory: ULayer[Has[TodoRepository]] = Ref.make(Map.empty[Todo.Id, Todo]).map(TodoRepositoryInmemory).toLayer

  def create(todo: CreateTodo): ZIO[Has[TodoRepository], AppError, Todo] = ZIO
    .serviceWith[TodoRepository](_.create(todo))

  def getAll: ZIO[Has[TodoRepository], AppError, List[Todo]]        = ZIO.serviceWith[TodoRepository](_.getAll)
  def get(id: Todo.Id): ZIO[Has[TodoRepository], AppError, Todo]    = ZIO.serviceWith[TodoRepository](_.get(id))
  def delete(id: Todo.Id): ZIO[Has[TodoRepository], AppError, Unit] = ZIO.serviceWith[TodoRepository](_.delete(id))
  def update(todo: Todo): ZIO[Has[TodoRepository], AppError, Unit]  = ZIO.serviceWith[TodoRepository](_.update(todo))
}
