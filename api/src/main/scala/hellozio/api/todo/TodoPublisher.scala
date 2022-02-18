package hellozio.api.todo

import hellozio.api.common.config.AppConfig
import hellozio.domain.common.errors.AppError
import hellozio.domain.todo.{Todo, TodoUpdate}
import hellozio.domain.common.kafka.Serde
import io.circe.generic.auto._
import org.apache.kafka.clients.producer.ProducerRecord
import zio._
import zio.kafka.producer.Producer
import zio.kafka.producer.ProducerSettings

trait TodoPublisher {
  def send(update: TodoUpdate): IO[AppError, Unit]
}

final private case class TodoPublisherLive(
    producer: Producer,
    topic: String
) extends TodoPublisher {

  private val keySerde   = Serde.todoId
  private val valueSerde = Serde.json[TodoUpdate]

  override def send(update: TodoUpdate): IO[AppError, Unit] =
    producer
      .produceAsync(new ProducerRecord[Todo.Id, TodoUpdate](topic, update.id, update), keySerde, valueSerde)
      .mapError(e => AppError.Kafka(e.getMessage))
      .unit
}

object TodoPublisher extends Accessible[TodoPublisher] {

  lazy val live: URLayer[AppConfig, TodoPublisher] = {
    ZIO
      .service[AppConfig]
      .toManaged
      .flatMap { config =>
        Producer
          .make(ProducerSettings(List(config.kafka.bootstrapServers)))
          .map(p => TodoPublisherLive(p, config.kafka.topic))
      }
      .orDie
      .toLayer
  }
}
