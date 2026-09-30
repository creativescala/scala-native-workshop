//> using scala 3.9.0
//> using platform native

// Testing raw mode

import scala.scalanative.posix.termios.*
import scala.scalanative.posix.termiosOps.*
import scala.scalanative.posix.unistd.STDIN_FILENO
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

object Terminal:
  def withRawMode[A](f: => A): A =
    Zone:
      // Allocate space for two termios structs
      val original = alloc[termios]()
      val raw = alloc[termios]()

      // Get the current settings twice: one copy we keep, one we modify
      if tcgetattr(STDIN_FILENO, original) != 0 ||
        tcgetattr(STDIN_FILENO, raw) != 0
      then throw new RuntimeException("Could not get terminal settings")

      // Turn off the flags for raw mode
      raw.c_iflag =
        raw.c_iflag & ~(BRKINT | ICRNL | INPCK | ISTRIP | IXON).toUInt
      raw.c_oflag = raw.c_oflag & ~OPOST.toUInt
      raw.c_cflag = raw.c_cflag | CS8.toUInt
      raw.c_lflag = raw.c_lflag & ~(ECHO | ICANON | IEXTEN | ISIG).toUInt

      if tcsetattr(STDIN_FILENO, TCSAFLUSH, raw) != 0 then
        throw new RuntimeException("Could not set terminal settings")

      // Run the user's code, and always restore the original settings
      try f
      finally tcsetattr(STDIN_FILENO, TCSAFLUSH, original)

@main def keys(): Unit =
  Terminal.withRawMode:
    var byte = System.in.read()
    while byte != 'q' && byte != -1 do
      // We turned off output processing, so we need \r\n to get a newline
      print(s"$byte\r\n")
      System.out.flush()
      byte = System.in.read()
