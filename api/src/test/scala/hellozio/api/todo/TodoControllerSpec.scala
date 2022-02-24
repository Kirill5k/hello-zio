package hellozio.api.todo

import org.scalatestplus.mockito.MockitoSugar

class TodoControllerSpec extends ControllerSpec with MockitoSugar {

  "A TodoController" when {

    "GET /api/todos" should {
      "return all todos" in {
        pending
      }
    }

    "GET /api/todos/:id" should {
      "find todo by id" in {
        pending
      }

      "return 404 when todo does not exist" in {
        pending
      }
    }

    "POST /api/todos" should {
      "create new todo" in {
        pending
      }
    }

    "PUT /api/todos/:id" should {
      "update existing todo" in {
        pending
      }

      "return 404 when todo does not exist" in {
        pending
      }
    }

    "DELETE /api/todos/:id" should {
      "delete existing todo and return 204" in {
        pending
      }

      "return 404 when todo does not exist" in {
        pending
      }
    }
  }
}
