package hellozio.consumer.todo

import hellozio.consumer.common.config.{AppConfig, KafkaConfig}
import hellozio.domain.todo.{TodoUpdate, Todos}
import io.circe.syntax.*
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringDeserializer, StringSerializer}
import zio.*
import zio.test.Assertion.*
import zio.test.*

object TodoConsumerSpec extends ZIOSpecDefault {

  val topic     = "todo-updates"
  val kafkaPort = 29095
  val appConfig = AppConfig(KafkaConfig(s"localhost:$kafkaPort", "todo-consumer", topic))

  implicit val serializer: Serializer[String]     = new StringSerializer()
  implicit val deserializer: Deserializer[String] = new StringDeserializer()
  implicit val config: EmbeddedKafkaConfig        = EmbeddedKafkaConfig(kafkaPort = kafkaPort)

  def spec = suite("A TodoConsumer should")(
    test("consume todo updates") {
      val todo = TodoUpdate.Created(Todos.id, Todos.todo)
      for {
        _ <- ZIO.acquireRelease(ZIO.attempt(EmbeddedKafka.start()))(s => ZIO.attempt(EmbeddedKafka.stop(s)).orDie)
        fib <- TodoConsumer.updates
          .timeout(10.seconds)
          .runCollect
          .map(_.toList)
          .provide(ZLayer.succeed(appConfig), ZLayer.succeed(Clock.ClockLive), TodoConsumer.layer)
          .fork
        _   <- ZIO.sleep(2.seconds)
        _   <- ZIO.attempt(EmbeddedKafka.publishToKafka(new ProducerRecord(topic, Todos.id.value, todo.asJson.noSpaces)))
        _   <- ZIO.sleep(2.seconds)
        res <- fib.join
      } yield assert(res)(equalTo(List(todo)))
    } @@ TestAspect.ignore
  )
}
