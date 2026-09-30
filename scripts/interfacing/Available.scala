//> using scala 3.9.0
//> using platform native

// Exercise 3: how many bytes are waiting to be read? Uses ioctl with FIONREAD,
// which returns its result through a pointer to an int.

import scala.scalanative.posix.sys.ioctl.{FIONREAD, ioctl}
import scala.scalanative.posix.unistd.STDIN_FILENO
import scala.scalanative.unsafe.*

def bytesAvailable(): Int =
  Zone:
    // Space for ioctl to write its result into
    val count = alloc[CInt]()
    // Scala Native's ioctl takes a Ptr[Byte], so we must cast our pointer
    if ioctl(STDIN_FILENO, FIONREAD, count.asInstanceOf[Ptr[Byte]]) != 0 then
      throw new RuntimeException("Could not get number of bytes available")
    // Read the value the pointer points to
    !count

@main def available(): Unit =
  // We don't need raw mode, but without it input is only available after
  // the user presses Enter
  for _ <- 1 to 3 do
    Thread.sleep(1000)
    println(s"Bytes available: ${bytesAvailable()}")
