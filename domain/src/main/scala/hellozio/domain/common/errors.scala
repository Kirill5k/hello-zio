package hellozio.domain.common

import hellozio.domain.todo.Todo

object errors {

  trait AppError extends Throwable {
    def message: String
    override def getMessage: String = message
  }

  object AppError {
    final case class Db(message: String)     extends AppError
    final case class Config(message: String) extends AppError
    final case class Kafka(message: String)  extends AppError

    final case class TodoNotFound(id: Todo.Id) extends AppError {
      val message: String = s"Todo with id ${id.value} does not exist"
    }

  }

}
