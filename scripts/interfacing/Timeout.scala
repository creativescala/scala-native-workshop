//> using scala 3.9.0
//> using platform native

// Exercise 1: reading with a timeout, and telling the Escape key apart from
// arrow keys.

import scala.scalanative.posix.termios.*
import scala.scalanative.posix.termiosOps.*
import scala.scalanative.posix.unistd
import scala.scalanative.posix.unistd.STDIN_FILENO
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

object Terminal:
  def withRawMode[A](f: => A): A =
    Zone:
      val original = alloc[termios]()
      val raw = alloc[termios]()

      if tcgetattr(STDIN_FILENO, original) != 0 ||
        tcgetattr(STDIN_FILENO, raw) != 0
      then throw new RuntimeException("Could not get terminal settings")

      raw.c_iflag =
        raw.c_iflag & ~(BRKINT | ICRNL | INPCK | ISTRIP | IXON).toUInt
      raw.c_oflag = raw.c_oflag & ~OPOST.toUInt
      raw.c_cflag = raw.c_cflag | CS8.toUInt
      raw.c_lflag = raw.c_lflag & ~(ECHO | ICANON | IEXTEN | ISIG).toUInt

      // NEW: don't wait for any minimum number of bytes, but do wait
      // up to 0.1 seconds (VTIME is in tenths of a second) for input to arrive
      raw.c_cc(VMIN) = 0.toUByte
      raw.c_cc(VTIME) = 1.toUByte

      if tcsetattr(STDIN_FILENO, TCSAFLUSH, raw) != 0 then
        throw new RuntimeException("Could not set terminal settings")

      try f
      finally tcsetattr(STDIN_FILENO, TCSAFLUSH, original)

  /** Read a single byte, returning None if no input arrived before the timeout. */
  def readByte(): Option[Byte] =
    Zone:
      val buffer = alloc[Byte]()
      unistd.read(STDIN_FILENO, buffer, 1.toCSize) match
        case 1  => Some(!buffer)
        case 0  => None
        case _ => throw new RuntimeException("Could not read input")

enum Key:
  case Character(char: Char)
  case Up, Down, Left, Right, Escape

object Key:
  /** Read a key, returning None if no key was pressed before the timeout. */
  def read(): Option[Key] =
    Terminal.readByte().map:
      case 27 =>
        // Either the Escape key, or the start of an escape code. If nothing
        // follows before the timeout, it was the Escape key.
        Terminal.readByte() match
          case Some('[') =>
            Terminal.readByte() match
              case Some('A') => Key.Up
              case Some('B') => Key.Down
              case Some('C') => Key.Right
              case Some('D') => Key.Left
              case _         => Key.Escape // Some escape code we don't handle
          case _ => Key.Escape
      case byte => Key.Character(byte.toChar)

@main def timeout(): Unit =
  Terminal.withRawMode:
    var ticks = 0
    var key = Key.read()
    while key != Some(Key.Character('q')) do
      key match
        case None      => ticks = ticks + 1
        case Some(key) => print(s"$key after $ticks ticks\r\n")
      System.out.flush()
      key = Key.read()
