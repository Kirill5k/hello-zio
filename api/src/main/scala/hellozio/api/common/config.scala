package hellozio.api.common

import hellozio.domain.common.errors.AppError
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio._

object config {

  final case class ServerConfig(
      host: String,
      port: Int
  )

  final case class KafkaConfig(
      bootstrapServers: String,
      topic: String
  )

  final case class AppConfig(
      server: ServerConfig,
      kafka: KafkaConfig
  )

  object AppConfig {
    lazy val layer: Layer[AppError, AppConfig] =
      ZLayer {
        ZIO.attemptBlocking(ConfigSource.default.load[AppConfig])
          .flatMap(result => ZIO.fromEither(result).mapError(e => AppError.Config(e.head.description)))
          .orDie
      }
  }

}
