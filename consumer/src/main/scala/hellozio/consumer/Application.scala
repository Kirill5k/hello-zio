package hellozio.consumer

import hellozio.consumer.common.config.AppConfig
import hellozio.consumer.todo.TodoConsumer
import zio._

object Application extends ZIOAppDefault {

  val configLayer    = AppConfig.layer
  val consumerLayer = (configLayer ++ Clock.live) >>> TodoConsumer.layer

  override def run: URIO[zio.ZEnv, ExitCode] =
    TodoConsumer
      .updates
      .mapZIO(u => ZIO.logInfo(s"Received update $u"))
      .runDrain
      .provideLayer(consumerLayer)
      .orDie
      .exitCode
}
