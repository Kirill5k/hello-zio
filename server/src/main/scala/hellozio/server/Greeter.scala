package hellozio.server

import java.time.OffsetDateTime
import zio.Has
import zio.UIO
import zio.URIO
import zio.URLayer
import zio.ZIO
import zio.clock.Clock
import zio.random.Random

final case class Greeting(
    greet: String,
    name: String,
    date: OffsetDateTime
)

trait Greeter {
  def greet(name: String): UIO[Greeting]
}

final private case class GreeterLive(random: Random.Service, clock: Clock.Service) extends Greeter {

  private val greetings = Vector("Hello", "Bonjour", "Hola", "Zdravstvuyte", "Salve")

  override def greet(name: String): UIO[Greeting] =
    ZIO
      .tupledPar(random.nextIntBounded(greetings.size).map(greetings.apply), clock.currentDateTime)
      .map { case (greet, date) =>
        Greeting(name, greet, date)
      }
      .orDie

}

object Greeter {
  val layer: URLayer[Has[Random.Service] with Has[Clock.Service], Has[Greeter]] = (GreeterLive(_, _)).toLayer

  def greet(name: String): URIO[Has[Greeter], Greeting] = ZIO.serviceWith[Greeter](_.greet(name))
}
