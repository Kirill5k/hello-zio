package hellozio.api.todo

import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.{Todo, Todos}
import mongo4cats.zio.embedded.EmbeddedMongo
import mongo4cats.zio.*
import zio.*
import zio.test.Assertion.*
import zio.test.*

object TodoRepositorySpec extends ZIOSpecDefault with EmbeddedMongo {

  def spec = suite("TodoRepository")(
    suite("inmemory")(
      test("store todo in memory") {
        val todo   = Todos.genCreate()
        val result = TodoRepository.create(todo) *> TodoRepository.getAll.map(_.head)

        result.provide(TodoRepository.inmemory).map { t =>
          assert(t.task)(equalTo(todo.task)) && assert(t.createdAt)(equalTo(todo.createdAt))
        }
      },
      test("update todo") {
        val todo = Todos.genCreate()
        val result = for {
          newTodo     <- TodoRepository.create(todo)
          _           <- TodoRepository.update(newTodo.copy(task = Todo.Task("updated")))
          updatedTodo <- TodoRepository.get(newTodo.id)
        } yield updatedTodo

        result.provide(TodoRepository.inmemory).map { todo =>
          assert(todo.task)(equalTo(Todo.Task("updated")))
        }
      },
      test("return error when todo does not exist on get") {
        val result = TodoRepository.get(Todos.id).provide(TodoRepository.inmemory)
        assertZIO(result.exit)(fails(equalTo(AppError.TodoNotFound(Todos.id))))
      },
      test("return error when todo does not exist on delete") {
        val result = TodoRepository.delete(Todos.id).provide(TodoRepository.inmemory)
        assertZIO(result.exit)(fails(equalTo(AppError.TodoNotFound(Todos.id))))
      },
      test("return error when todo does not exist on update") {
        val result = TodoRepository.update(Todos.todo).provide(TodoRepository.inmemory)
        assertZIO(result.exit)(fails(equalTo(AppError.TodoNotFound(Todos.id))))
      }
    ),
    suite("mongo")(
      test("store todo in db") {
        withEmbeddedMongoDatabase { db =>
          val todo = Todos.genCreate()
          val result = TodoRepository.create(todo) *> TodoRepository.getAll.map(_.head)

          result.provide(ZLayer.succeed(db) >>> TodoRepository.mongo).map { t =>
            assert(t.task)(equalTo(todo.task)) && assert(t.createdAt)(equalTo(todo.createdAt))
          }
        }
      },
      test("update todo") {
        withEmbeddedMongoDatabase { db =>
          val todo = Todos.genCreate()
          val result = for {
            newTodo <- TodoRepository.create(todo)
            _ <- TodoRepository.update(newTodo.copy(task = Todo.Task("updated")))
            updatedTodo <- TodoRepository.get(newTodo.id)
          } yield updatedTodo

          result.provide(ZLayer.succeed(db) >>> TodoRepository.inmemory).map { todo =>
            assert(todo.task)(equalTo(Todo.Task("updated")))
          }
        }
      }
    )
  )

  def withEmbeddedMongoDatabase[A](test: ZMongoDatabase => ZIO[Any, Throwable, A]): ZIO[Scope, Throwable, A] =
    withRunningEmbeddedMongo {
      ZIO
        .serviceWithZIO[ZMongoDatabase](test(_))
        .provide(
          ZLayer.scoped(ZMongoClient.fromConnectionString(s"mongodb://localhost:$mongoPort")),
          ZLayer.fromZIO(ZIO.serviceWithZIO[ZMongoClient](_.getDatabase("test-db")))
        )
    }
}
