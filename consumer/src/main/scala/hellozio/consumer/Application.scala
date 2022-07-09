package hellozio.consumer

import hellozio.consumer.common.config.AppConfig
import hellozio.consumer.todo.TodoConsumer
import zio._

object Application extends ZIOAppDefault {

  override def run: URIO[Scope, ExitCode] =
    TodoConsumer
      .updates
      .mapZIO(u => ZIO.logInfo(s"Received update $u"))
      .runDrain
      .provideSome(
        AppConfig.layer,
        TodoConsumer.layer
      )
      .orDie
      .exitCode
}
