package hellozio.api.todo

import io.circe.Codec
import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.Todo.Id
import hellozio.domain.todo.{CreateTodo, Todo}
import mongo4cats.bson.ObjectId
import mongo4cats.circe.given
import mongo4cats.operations.{Filter, Update}
import mongo4cats.zio.{ZMongoCollection, ZMongoDatabase}

import java.util.UUID
import java.time.Instant
import zio.*

trait TodoRepository {
  def create(todo: CreateTodo): IO[AppError, Todo]
  def update(todo: Todo): IO[AppError, Unit]
  def get(id: Todo.Id): IO[AppError, Todo]
  def getAll: IO[AppError, List[Todo]]
  def delete(id: Todo.Id): IO[AppError, Unit]
}

final private case class TodoEntity(_id: ObjectId, task: String, createdAt: Instant) derives Codec.AsObject

final private case class TodoRepositoryMongo(collection: ZMongoCollection[TodoEntity]) extends TodoRepository {

  def create(todo: CreateTodo): IO[AppError, Todo] =
    val id = ObjectId.gen
    collection
      .insertOne(TodoEntity(id, todo.task.value, todo.createdAt))
      .as(Todo(Todo.Id(id.toHexString), todo.task, todo.createdAt))
      .mapError(t => AppError.Db(t.getMessage))

  def update(todo: Todo): IO[AppError, Unit] =
    collection
      .updateOne(Filter.idEq(ObjectId(todo.id.value)), Update.set("task", todo.task.value).set("createdAt", todo.createdAt))
      .mapError(t => AppError.Db(t.getMessage))
      .flatMap(res => ZIO.cond(res.getMatchedCount > 0, (), AppError.TodoNotFound(todo.id)))

  def get(id: Id): IO[AppError, Todo] =
    collection
      .find(Filter.idEq(ObjectId(id.value)))
      .first
      .mapError(t => AppError.Db(t.getMessage))
      .flatMap {
        case None    => ZIO.fail(AppError.TodoNotFound(id))
        case Some(t) => ZIO.succeed(Todo(Todo.Id(t._id.toHexString), Todo.Task(t.task), t.createdAt))
      }

  def getAll: IO[AppError, List[Todo]] =
    collection.find.all
      .mapBoth(
        t => AppError.Db(t.getMessage),
        ts => ts.map(t => Todo(Todo.Id(t._id.toHexString), Todo.Task(t.task), t.createdAt)).toList
      )

  def delete(id: Id): IO[AppError, Unit] =
    collection
      .deleteOne(Filter.idEq(ObjectId(id.value)))
      .mapError(t => AppError.Db(t.getMessage))
      .flatMap(res => ZIO.cond(res.getDeletedCount > 0, (), AppError.TodoNotFound(id)))
}

final private case class TodoRepositoryInmemory(storage: Ref[Map[Todo.Id, Todo]]) extends TodoRepository {

  override def create(todo: CreateTodo): IO[AppError, Todo] = ZIO
    .attempt(UUID.randomUUID().toString)
    .map(id => Todo(Todo.Id(id), todo.task, todo.createdAt))
    .tap(todo => storage.update(_ + (todo.id -> todo)))
    .mapError(e => AppError.Db(e.getMessage))

  override def getAll: IO[AppError, List[Todo]] = storage.get.map(_.values.toList)

  override def get(id: Todo.Id): IO[AppError, Todo] = storage.get
    .map(_.get(id))
    .flatMap {
      case Some(todo) => ZIO.succeed(todo)
      case None       => ZIO.fail(AppError.TodoNotFound(id))
    }

  override def delete(id: Todo.Id): IO[AppError, Unit] = storage.get
    .filterOrFail(_.contains(id))(AppError.TodoNotFound(id))
    .flatMap(s => storage.set(s.removed(id)))

  override def update(todo: Todo): IO[AppError, Unit] = storage.get
    .filterOrFail(_.contains(todo.id))(AppError.TodoNotFound(todo.id))
    .flatMap(s => storage.set(s + (todo.id -> todo)))

}

object TodoRepository {
  val inmemory: ULayer[TodoRepository] = ZLayer(Ref.make(Map.empty[Todo.Id, Todo]).map(TodoRepositoryInmemory.apply))
  val mongo: URLayer[ZMongoDatabase, TodoRepository] = {
    val collection = ZIO.serviceWithZIO[ZMongoDatabase](_.getCollectionWithCodec[TodoEntity]("todos"))
    ZLayer.fromZIO(collection.map(TodoRepositoryMongo.apply).orDie)
  }

  def create(todo: CreateTodo): ZIO[TodoRepository, AppError, Todo] = ZIO.serviceWithZIO[TodoRepository](_.create(todo))
  def getAll: ZIO[TodoRepository, AppError, List[Todo]]             = ZIO.serviceWithZIO[TodoRepository](_.getAll)
  def get(id: Todo.Id): ZIO[TodoRepository, AppError, Todo]         = ZIO.serviceWithZIO[TodoRepository](_.get(id))
  def delete(id: Todo.Id): ZIO[TodoRepository, AppError, Unit]      = ZIO.serviceWithZIO[TodoRepository](_.delete(id))
  def update(todo: Todo): ZIO[TodoRepository, AppError, Unit]       = ZIO.serviceWithZIO[TodoRepository](_.update(todo))
}
