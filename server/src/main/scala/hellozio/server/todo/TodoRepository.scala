package hellozio.server.todo

import hellozio.server.AppError
import zio.IO

trait TodoRepository {
  def create(todo: CreateTodo): IO[AppError.DbError, Unit]
  def getAll: IO[AppError.DbError, List[Todo]]
}
