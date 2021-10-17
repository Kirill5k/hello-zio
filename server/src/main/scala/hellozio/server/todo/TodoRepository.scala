package hellozio.server.todo

import hellozio.server.AppError
import java.util.UUID
import zio.Has
import zio.IO
import zio.Ref
import zio.ULayer
import zio.ZIO

trait TodoRepository {
  def create(todo: CreateTodo): IO[AppError.DbError, Todo.Id]
  def get(id: Todo.Id): IO[AppError.DbError, Option[Todo]]
  def getAll: IO[AppError.DbError, List[Todo]]
}

final private case class TodoRepositoryInmemory(storage: Ref[Map[Todo.Id, Todo]]) extends TodoRepository {

  override def create(todo: CreateTodo): IO[AppError.DbError, Todo.Id] = ZIO
    .effect(UUID.randomUUID().toString)
    .map(Todo.Id.apply)
    .tap(id => storage.update(_ + (id -> Todo(id, todo.task, todo.createdAt))))
    .mapError(e => AppError.DbError(e.getMessage))

  override def getAll: IO[AppError.DbError, List[Todo]] = storage.get.map(_.values.toList)

  override def get(id: Todo.Id): IO[AppError.DbError, Option[Todo]] = storage.get.map(_.get(id))
}

object TodoRepository {

  val inmemory: ULayer[Has[TodoRepository]] = Ref.make(Map.empty[Todo.Id, Todo]).map(TodoRepositoryInmemory).toLayer

}
