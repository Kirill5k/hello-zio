package hellozio.server.common

object errors {

  trait AppError extends Throwable {
    def message: String
    override def getMessage: String = message
  }

  object AppError {
    final case class DbError(message: String)     extends AppError
    final case class ConfigError(message: String) extends AppError
  }

}
