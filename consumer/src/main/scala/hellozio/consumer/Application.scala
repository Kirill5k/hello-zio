package hellozio.consumer

import zio.{ExitCode, URIO}
import zio.console._

object Application extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    putStrLn("Hello, World!").orDie.exitCode
}
