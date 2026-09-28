package objective

import cats.effect.ExitCode
import cats.effect.IO
import cats.syntax.all.*
import fs2.Chunk
import fs2.io.net.Network

object CommandClient:
  def send(command: Command): IO[Unit] =
    Network.forIO.connect(Configuration.commandSocketAddress).use { socket =>
      socket.write(Chunk.array(command.unparse.getBytes()))
    }
