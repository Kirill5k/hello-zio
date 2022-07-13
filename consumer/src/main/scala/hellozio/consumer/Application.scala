package hellozio.consumer

import hellozio.consumer.common.config.AppConfig
import hellozio.consumer.todo.TodoConsumer
import zio.*

object Application extends ZIOAppDefault {

  override def run: URIO[Scope, ExitCode] =
    TodoConsumer
      .updates
      .mapZIO(u => ZIO.logInfo(s"Received update $u"))
      .runDrain
      .provide(
        AppConfig.layer,
        TodoConsumer.layer,
        ZLayer.succeed(Clock.ClockLive)
      )
      .orDie
      .exitCode
}
