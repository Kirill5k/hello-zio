package hellozio.api.todo

import hellozio.domain.todo.{TodoUpdate, Todos}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import zio.*
import zio.test.Assertion.*
import zio.test.*

object TodoServiceSpec extends ZIOSpecDefault with MockitoSugar {

  def spec = suite("A TodoService should")(
    test("delete todo") {
      val (repo, pub) = mocks
      when(repo.delete(Todos.id)).thenReturn(ZIO.unit)
      when(pub.send(TodoUpdate.Deleted(Todos.id))).thenReturn(ZIO.unit)

      assertZIO(TodoService.delete(Todos.id).provideLayer(mockLayer(repo, pub)))(isUnit)
    },
    test("update todo") {
      val (repo, pub) = mocks
      when(repo.update(Todos.todo)).thenReturn(ZIO.unit)
      when(pub.send(TodoUpdate.Updated(Todos.id, Todos.todo))).thenReturn(ZIO.unit)

      assertZIO(TodoService.update(Todos.todo).provideLayer(mockLayer(repo, pub)))(isUnit)
    },
    test("get todos from repository") {
      val (repo, pub) = mocks
      when(repo.getAll).thenReturn(ZIO.succeed(List(Todos.todo)))

      assertZIO(TodoService.getAll.provideLayer(mockLayer(repo, pub)))(hasSameElementsDistinct(List(Todos.todo)))
    },
    test("get todo by id") {
      val (repo, pub) = mocks
      when(repo.get(Todos.id)).thenReturn(ZIO.succeed(Todos.todo))

      assertZIO(TodoService.get(Todos.id).provideLayer(mockLayer(repo, pub)))(equalTo(Todos.todo))
    },
    test("create new todo") {
      val (repo, pub) = mocks
      when(repo.create(Todos.create)).thenReturn(ZIO.succeed(Todos.todo))
      when(pub.send(TodoUpdate.Created(Todos.id, Todos.todo))).thenReturn(ZIO.unit)

      assertZIO(TodoService.create(Todos.create).provideLayer(mockLayer(repo, pub)))(equalTo(Todos.id))
    }
  )

  def mocks: (TodoRepository, TodoPublisher) =
    (mock[TodoRepository], mock[TodoPublisher])

  def mockLayer(repo: TodoRepository, publisher: TodoPublisher): ULayer[TodoService] =
    (ZLayer.succeed(repo) ++ ZLayer.succeed(publisher)) >>> TodoService.layer
}
