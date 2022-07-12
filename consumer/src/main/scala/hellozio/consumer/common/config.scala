package hellozio.consumer.common

import hellozio.domain.common.errors.AppError
import pureconfig.*
import pureconfig.generic.derivation.default.*
import zio.*

object config {

  final case class KafkaConfig(
      bootstrapServers: String,
      groupId: String,
      topic: String
  ) derives ConfigReader

  final case class AppConfig(kafka: KafkaConfig) derives ConfigReader

  object AppConfig {
    lazy val layer: Layer[AppError, AppConfig] =
      ZLayer {
        ZIO.attemptBlocking(ConfigSource.default.load[AppConfig])
          .flatMap { result =>
            ZIO.fromEither(result).mapError(e => AppError.Config(e.head.description))
          }
          .orDie
      }
  }
}
