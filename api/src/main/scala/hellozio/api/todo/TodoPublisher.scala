package hellozio.api.todo

import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerRecords, ProducerSettings}
import hellozio.api.common.config.AppConfig
import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.{Todo, TodoUpdate}
import hellozio.domain.common.kafka.Serde
import io.circe.generic.auto._
import zio.interop.catz._
import zio._

trait TodoPublisher {
  def send(update: TodoUpdate): IO[AppError, Unit]
}

final private case class TodoPublisherLive(
    producer: KafkaProducer[RIO[Clock, *], Todo.Id, TodoUpdate],
    topic: String
) extends TodoPublisher {

  override def send(update: TodoUpdate): IO[AppError, Unit] =
    producer
      .produce(ProducerRecords.one(ProducerRecord(topic, update.id, update)))
      .mapError(e => AppError.Kafka(e.getMessage))
      .unit
}

object TodoPublisher {

  lazy val layer: URLayer[AppConfig, TodoPublisher] =
    ZLayer.scoped {
      ZIO
        .service[AppConfig]
        .flatMap { config =>
          val settings = ProducerSettings(
            keySerializer = Serde.todoIdSerializer,
            valueSerializer = Serde.jsonSerializer[TodoUpdate]
          ).withBootstrapServers(config.kafka.bootstrapServers)
          KafkaProducer
            .resource(settings)
            .toScopedZIO
            .map(producer => TodoPublisherLive(producer, config.kafka.topic))
        }
        .orDie
    }

  def send(update: TodoUpdate): ZIO[TodoPublisher, AppError, Unit] = ZIO.serviceWithZIO[TodoPublisher](_.send(update))
}
