package hellozio.api.todo

import org.http4s.{Response, Status}
import org.scalatest.Assertion
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.parser._
import zio.interop.catz._
import zio._

trait ControllerSpec extends AnyWordSpec with Matchers {

  def run[E, A](zio: ZIO[Any, E, A]): A =
    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(zio).getOrThrowFiberFailure()
    }

  def verifyJsonResponse(
      response: Task[Response[Task]],
      expectedStatus: Status,
      expectedBody: Option[String] = None
  ): Assertion =
    run(
      response.flatMap { res =>
        expectedBody match {
          case Some(expectedJson) =>
            res.as[String].map { receivedJson =>
              res.status mustBe expectedStatus
              parse(receivedJson) mustBe parse(expectedJson)
            }
          case None =>
            res.body.compile.toVector.map { receivedJson =>
              res.status mustBe expectedStatus
              receivedJson mustBe empty
            }
        }
      }
    )
}
