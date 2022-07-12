package hellozio.api.todo

import hellozio.api.common.config.{AppConfig, KafkaConfig, ServerConfig}
import hellozio.domain.todo.{TodoUpdate, Todos}
import io.circe.parser.*
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import zio.test.Assertion.*
import zio.test.*
import zio.{Clock, ZIO, ZLayer}

object TodoPublisherSpec extends ZIOSpecDefault {

  val topic     = "todo-updates"
  val kafkaPort = 29092
  val appConfig = AppConfig(ServerConfig("0.0.0.0", 8080), KafkaConfig(s"localhost:$kafkaPort", topic))

  def spec = suite("A TodoPublisher should")(
    test("publish todo updates to a topic") {
      given EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = kafkaPort)
      for {
        _ <- ZIO.acquireRelease(ZIO.attempt(EmbeddedKafka.start()))(s => ZIO.attempt(EmbeddedKafka.stop(s)).orDie)
        updates = List(
          TodoUpdate.Created(Todos.id, Todos.todo),
          TodoUpdate.Updated(Todos.id, Todos.todo),
          TodoUpdate.Deleted(Todos.id)
        )
        _ <- ZIO
          .foreach(updates)(TodoPublisher.send)
          .provide(ZLayer.succeed(appConfig), TodoPublisher.layer, ZLayer.succeed(Clock.ClockLive))
        messages <- ZIO.attempt(EmbeddedKafka.consumeNumberStringMessagesFrom(topic, 3))
      } yield assert(messages.flatMap(decode[TodoUpdate](_).toOption))(equalTo(updates))
    }
  )
}
