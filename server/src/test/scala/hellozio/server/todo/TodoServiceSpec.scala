package hellozio.server.todo

import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import zio.{Has, ULayer, ZIO, ZLayer, Runtime}

class TodoServiceSpec extends AsyncWordSpec with Matchers with MockitoSugar {

  "A TodoService" should {

    val todos = List(Todos.gen(), Todos.gen())

    "get todos from repository" in {
      val repo = mock[TodoRepository]
      when(repo.getAll).thenReturn(ZIO.succeed(todos))

      Runtime
        .default
        .unsafeRunToFuture(TodoService.getAll.provideLayer(mockLayer(repo) >>> TodoService.layer))
        .map(_ mustBe todos)
    }
  }

  def mockLayer(repo: TodoRepository): ULayer[Has[TodoRepository]] = ZLayer.succeed(repo)
}
