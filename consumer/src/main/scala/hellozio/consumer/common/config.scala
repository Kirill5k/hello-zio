package hellozio.consumer.common

import hellozio.domain.common.errors.AppError
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import zio.{Has, ZIO, ZLayer}
import zio.blocking.{blocking, Blocking}

object config {

  final case class KafkaConfig(
      bootstrapServers: String,
      groupId: String,
      topic: String
  )

  final case class AppConfig(kafka: KafkaConfig)

  object AppConfig {

    lazy val layer: ZLayer[Blocking, AppError, Has[AppConfig]] =
      blocking(ZIO.effect(ConfigSource.default.load[AppConfig]))
        .flatMap { result =>
          ZIO.fromEither(result).mapError(e => AppError.ConfigError(e.head.description))
        }
        .orDie
        .toLayer

  }
}
