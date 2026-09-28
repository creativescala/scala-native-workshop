package objective

import com.comcast.ip4s.UnixSocketAddress
import fs2.io.file.Files
import fs2.io.file.Path

object Configuration:
  val maxConcurrentConnections = 5
  // Clients send commands
  val commandSocketAddress = UnixSocketAddress(
    "/run/user/1000/objective-command.sock"
  )
  // Watchers receive updates
  val watcherSocketAddress = UnixSocketAddress(
    "/run/user/1000/objective-client.sock"
  )

  val tasksPath = Path("/home/noel/Dropbox/Personal/liminal/today.org")
