package hellozio.consumer

import zio._

object Application extends ZIOAppDefault {
  override def run: URIO[zio.ZEnv, ExitCode] =
    Console.printLine("Hello, World!").orDie.exitCode
}
