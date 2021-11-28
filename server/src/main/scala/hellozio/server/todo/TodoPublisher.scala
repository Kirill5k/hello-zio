package hellozio.server.todo

import hellozio.server.common.config.AppConfig
import hellozio.server.common.errors.AppError
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.auto._
import org.apache.kafka.clients.producer.ProducerRecord
import zio.Has
import zio.IO
import zio.URLayer
import zio.ZIO
import zio.blocking.Blocking
import zio.kafka.producer.Producer
import zio.kafka.producer.ProducerSettings
import zio.kafka.serde.Serde

trait TodoPublisher {
  def send(update: TodoUpdate): IO[AppError, Unit]
}

final private case class TodoPublisherLive(
    producer: Producer,
    topic: String
) extends TodoPublisher {

  override def send(update: TodoUpdate): IO[AppError, Unit] =
    producer
      .produce(
        new ProducerRecord[Todo.Id, TodoUpdate](topic, update.id, update),
        TodoPublisher.todoIdSerde,
        TodoPublisher.jsonSerde[TodoUpdate]
      )
      .mapError(e => AppError.KafkaError(e.getMessage))
      .unit

}

object TodoPublisher {

  val todoIdSerde: Serde[Any, Todo.Id] = Serde.string.inmap(Todo.Id.apply)(_.value)

  def jsonSerde[A](implicit enc: Encoder[A], dec: Decoder[A]): Serde[Any, A] =
    Serde.string.inmapM(j => ZIO.fromEither(dec.decodeJson(Json.fromString(j))))(v => ZIO.effect(enc(v).noSpaces))

  val live: URLayer[Has[AppConfig] with Blocking, Has[TodoPublisher]] =
    ZIO
      .access[Has[AppConfig]](_.get)
      .toManaged_
      .flatMap { config =>
        Producer
          .make(ProducerSettings(List(config.kafka.bootstrapServers)))
          .map(p => TodoPublisherLive(p, config.kafka.topic))
      }
      .orDie
      .toLayer

}
