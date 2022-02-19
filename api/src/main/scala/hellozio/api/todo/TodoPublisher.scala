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
      .provideLayer(Clock.live)
}

object TodoPublisher extends Accessible[TodoPublisher] {

  lazy val layer: URLayer[Clock with AppConfig, TodoPublisher] =
    ZIO
      .service[AppConfig]
      .toManaged
      .flatMap { config =>
        val settings = ProducerSettings(
          keySerializer = Serde.todoIdSerializer,
          valueSerializer = Serde.jsonSerializer[TodoUpdate]
        ).withBootstrapServers(config.kafka.bootstrapServers)
        KafkaProducer
          .resource(settings)
          .toManagedZIO
          .map(producer => TodoPublisherLive(producer, config.kafka.topic))
      }
      .orDie
      .toLayer
}
