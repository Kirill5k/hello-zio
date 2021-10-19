package hellozio.server.todo

import hellozio.server.common.errors.AppError
import java.util.UUID
import zio.Has
import zio.IO
import zio.Ref
import zio.ULayer
import zio.ZIO

trait TodoRepository {
  def create(todo: CreateTodo): IO[AppError, Todo.Id]
  def get(id: Todo.Id): IO[AppError, Todo]
  def getAll: IO[AppError, List[Todo]]
  def delete(id: Todo.Id): IO[AppError, Unit]
}

final private case class TodoRepositoryInmemory(storage: Ref[Map[Todo.Id, Todo]]) extends TodoRepository {

  override def create(todo: CreateTodo): IO[AppError, Todo.Id] = ZIO
    .effect(UUID.randomUUID().toString)
    .map(Todo.Id.apply)
    .tap(id => storage.update(_ + (id -> Todo(id, todo.task, todo.createdAt))))
    .mapError(e => AppError.DbError(e.getMessage))

  override def getAll: IO[AppError, List[Todo]] = storage.get.map(_.values.toList)

  override def get(id: Todo.Id): IO[AppError, Todo] = storage
    .get
    .map(_.get(id))
    .flatMap {
      case Some(todo) =>
        ZIO.succeed(todo)
      case None =>
        ZIO.fail(AppError.TodoNotFound(id))
    }

  override def delete(id: Todo.Id): IO[AppError, Unit] = storage
    .get
    .filterOrFail(_.contains(id))(AppError.TodoNotFound(id))
    .flatMap(s => storage.set(s.removed(id)))

}

object TodoRepository {

  val inmemory: ULayer[Has[TodoRepository]] = Ref.make(Map.empty[Todo.Id, Todo]).map(TodoRepositoryInmemory).toLayer

}
