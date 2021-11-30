package hellozio.consumer.todo

import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.TodoUpdate
import zio.stream.ZStream

trait TodoConsumer {
  def updates: ZStream[Any, AppError, TodoUpdate]
}
