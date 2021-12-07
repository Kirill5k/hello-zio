package hellozio.consumer.todo

import hellozio.consumer.common.config.{AppConfig, KafkaConfig}
import hellozio.domain.todo.{TodoUpdate, Todos}
import io.circe.syntax._
import io.circe.generic.auto._
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringDeserializer, StringSerializer}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration.Duration
import zio.{Runtime, ZLayer}

import scala.concurrent.duration._

class TodoConsumerSpec extends AnyWordSpec with Matchers with EmbeddedKafka {

  val topic     = "todo-updates"
  val kafkaPort = 29095
  val appConfig = AppConfig(KafkaConfig(s"localhost:$kafkaPort", "todo-consumer", topic))

  val layer = (ZLayer.succeed(appConfig) ++ Blocking.live ++ Clock.live) >>> TodoConsumer.live

  implicit val serializer: Serializer[String]     = new StringSerializer()
  implicit val deserializer: Deserializer[String] = new StringDeserializer()

  "A TodoConsumer" should {

    "consume todo updates" in {
      implicit val config = EmbeddedKafkaConfig(kafkaPort = kafkaPort)
      val json = TodoUpdate.Created(Todos.id, Todos.todo).asJson.noSpaces
      withRunningKafka {
        publishToKafka(topic, Todos.id.value, json)

        val updatesStream = TodoConsumer.updates.timeout(Duration.fromScala(10.seconds))
        val update = Runtime.default.unsafeRunSync(updatesStream.runHead.provideLayer(layer ++ Clock.live))

        update mustBe Some(TodoUpdate.Created(Todos.id, Todos.todo))
      }
    }
  }
}
