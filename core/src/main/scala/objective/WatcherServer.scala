package objective

import cats.effect.IO
import com.comcast.ip4s.UnixSocketAddress
import fs2.Stream
import fs2.concurrent.SignallingRef
import fs2.io.net.*
import fs2.text

class WatcherServer(tasks: Stream[IO, List[Item]]):
  private val connections: Stream[IO, Socket[IO]] = Network.forIO.bindAndAccept(
    Configuration.watcherSocketAddress,
    List(
      SocketOption.unixSocketDeleteIfExists(true),
      SocketOption.unixSocketDeleteOnClose(true)
    )
  )
  val server =
    connections
      .map(socket =>
        socket.reads
          .through(text.utf8.decode)
          .through(text.lines)
          .head
          .evalMap(s => IO.println(s"Received $s"))
          .handleErrorWith(exn =>
            Stream.eval(IO(exn.printStackTrace()))
          ): Stream[IO, Unit]
      )
      .parJoin(Configuration.maxConcurrentConnections)
