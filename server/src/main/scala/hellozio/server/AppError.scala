package hellozio.server

trait AppError extends Throwable {
  def message: String
  override def getMessage: String = message
}

object AppError {
  final case class DbError(message: String) extends AppError
}