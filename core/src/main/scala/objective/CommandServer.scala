package objective

import cats.effect.IO
import com.comcast.ip4s.UnixSocketAddress
import fs2.Stream
import fs2.concurrent.SignallingRef
import fs2.io.net.*
import fs2.text
import parsley.Failure
import parsley.Success

// The command serves owns writes to the tasks ref.
class CommandServer(tasks: SignallingRef[IO, List[Item]]):

  private val connections: Stream[IO, Socket[IO]] = Network.forIO.bindAndAccept(
    Configuration.commandSocketAddress,
    List(
      SocketOption.unixSocketDeleteIfExists(true),
      SocketOption.unixSocketDeleteOnClose(true)
    )
  )

  private val reload: IO[Unit] =
    Tasks.read
      .evalMap {
        case Failure(message) =>
          IO.println(s"Parsing tasks failed with reason: $message")
        case Success(newTasks) => tasks.set(newTasks)
      }
      .compile
      .drain

  private val inspect: IO[Unit] =
    tasks.get.flatMap(IO.println)

  val server =
    // Read the tasks on start
    Stream.exec(reload) ++
      connections
        .map(socket =>
          socket.reads
            .through(text.utf8.decode)
            .through(text.lines)
            .map(Command.parse)
            .evalMap {
              case Right(Command.Reload) =>
                IO.println("Reloading...") >> reload

              case Right(Command.Inspect) =>
                IO.println("Inspecting...") >> inspect

              case other => IO.println(s"Unknown command: $other")
            }
        )
        .parJoin(Configuration.maxConcurrentConnections)
