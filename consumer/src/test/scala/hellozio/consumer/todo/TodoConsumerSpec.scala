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
import zio.{Runtime, ZIO, ZLayer}

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
      withRunningKafka {
        val todo = TodoUpdate.Created(Todos.id, Todos.todo)
        val result = for {
          fib <- TodoConsumer.updates.timeout(Duration.fromScala(10.seconds)).runCollect.map(_.toList).fork
          _   <- ZIO.sleep(Duration.fromScala(2.seconds))
          _   <- ZIO.effect(publishToKafka(topic, Todos.id.value, todo.asJson.noSpaces))
          res <- fib.join
        } yield res

        val update = Runtime.default.unsafeRunSync(result.provideLayer(layer ++ Clock.live))
        update mustBe List(todo)
      }
    }
  }
}
