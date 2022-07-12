package hellozio.api.common

import hellozio.domain.common.errors.AppError
import pureconfig.ConfigSource
import pureconfig.*
import pureconfig.generic.derivation.default.*
import zio.*

object config {

  final case class ServerConfig(
      host: String,
      port: Int
  ) derives ConfigReader

  final case class KafkaConfig(
      bootstrapServers: String,
      topic: String
  ) derives ConfigReader

  final case class AppConfig(
      server: ServerConfig,
      kafka: KafkaConfig
  ) derives ConfigReader

  object AppConfig {
    lazy val layer: Layer[AppError, AppConfig] =
      ZLayer {
        ZIO.attemptBlocking(ConfigSource.default.load[AppConfig])
          .flatMap(result => ZIO.fromEither(result).mapError(e => AppError.Config(e.head.description)))
          .orDie
      }
  }

}
