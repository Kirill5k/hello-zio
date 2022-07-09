package hellozio.consumer.todo

import hellozio.consumer.common.config.{AppConfig, KafkaConfig}
import hellozio.domain.todo.{TodoUpdate, Todos}
import io.circe.syntax._
import io.circe.generic.auto._
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringDeserializer, StringSerializer}
import org.scalatest.Ignore
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import zio._

@Ignore
class TodoConsumerSpec extends AnyWordSpec with Matchers with EmbeddedKafka {

  val topic     = "todo-updates"
  val kafkaPort = 29095
  val appConfig = AppConfig(KafkaConfig(s"localhost:$kafkaPort", "todo-consumer", topic))

  implicit val serializer: Serializer[String]     = new StringSerializer()
  implicit val deserializer: Deserializer[String] = new StringDeserializer()
  implicit val config: EmbeddedKafkaConfig        = EmbeddedKafkaConfig(kafkaPort = kafkaPort)

  "A TodoConsumer" should {

    "consume todo updates" in {
      withRunningKafka {
        val todo = TodoUpdate.Created(Todos.id, Todos.todo)
        val result = for {
          fib <- TodoConsumer.updates.timeout(10.seconds).runCollect.map(_.toList).fork
          _   <- ZIO.sleep(2.seconds)
          _   <- ZIO.attempt(publishToKafka(new ProducerRecord(topic, Todos.id.value, todo.asJson.noSpaces)))
          _   <- ZIO.sleep(2.seconds)
          res <- fib.join
        } yield res

        val update = run(result.provide(ZLayer.succeed(appConfig), ZLayer.succeed(Clock.ClockLive), TodoConsumer.layer))
        update mustBe List(todo)
      }
    }
  }

  def run[E, A](zio: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(zio).getOrThrowFiberFailure()
    }
}
