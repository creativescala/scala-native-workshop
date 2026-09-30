//> using scala 3.9.0
//> using platform native

// Exercise 2: useful error messages using errno and strerror

import scala.scalanative.libc.errno.errno
import scala.scalanative.libc.string.strerror
import scala.scalanative.posix.termios.*
import scala.scalanative.posix.unistd.STDIN_FILENO
import scala.scalanative.unsafe.*

/** Throw an exception describing the error in errno. Call this immediately
  * after the failing call, before anything else can change errno.
  */
def fail(message: String): Nothing =
  val code = errno
  throw new RuntimeException(s"$message: ${fromCString(strerror(code))} (errno $code)")

@main def errors(): Unit =
  Zone:
    val settings = alloc[termios]()
    if tcgetattr(STDIN_FILENO, settings) != 0 then
      fail("Could not get terminal settings")
    println("Got terminal settings")
