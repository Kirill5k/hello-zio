package hellozio.server.common

import hellozio.server.todo.Todo

object errors {

  trait AppError extends Throwable {
    def message: String
    override def getMessage: String = message
  }

  object AppError {
    final case class DbError(message: String)     extends AppError
    final case class ConfigError(message: String) extends AppError
    final case class KafkaError(message: String)  extends AppError

    final case class TodoNotFound(id: Todo.Id) extends AppError {
      val message: String = s"Todo with id ${id.value} does not exist"
    }

  }

}
