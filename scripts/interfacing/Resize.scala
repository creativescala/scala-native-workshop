//> using scala 3.9.0
//> using platform native
//> using resourceDir resources

// Exercise 6: report the terminal size whenever it changes, by handling the
// SIGWINCH signal

import scala.scalanative.libc.signal.signal
import scala.scalanative.posix.sys.ioctl.ioctl
import scala.scalanative.posix.termios.*
import scala.scalanative.posix.termiosOps.*
import scala.scalanative.posix.unistd
import scala.scalanative.posix.unistd.{STDIN_FILENO, STDOUT_FILENO}
import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

type winsize =
  CStruct4[CUnsignedShort, CUnsignedShort, CUnsignedShort, CUnsignedShort]

@extern
object WinSize:
  @name("workshop_tiocgwinsz")
  def TIOCGWINSZ: CLongInt = extern

  @name("workshop_sigwinch")
  def SIGWINCH: CInt = extern

def terminalSize(): (Int, Int) =
  Zone:
    val size = alloc[winsize]()
    if ioctl(STDOUT_FILENO, WinSize.TIOCGWINSZ, size.asInstanceOf[Ptr[Byte]]) != 0
    then throw new RuntimeException("Could not get terminal size")
    (size._1.toInt, size._2.toInt)

object Resize:
  // Set by the signal handler, and cleared by the main loop
  @volatile var resized = true

  // The signal handler. It can't capture any local variables, as C only
  // gives us a function pointer to call, with nowhere to store them.
  val handler = CFuncPtr1.fromScalaFunction[CInt, Unit](_ => resized = true)

  def install(): Unit =
    signal(WinSize.SIGWINCH, handler)

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
      raw.c_cc(VMIN) = 0.toUByte
      raw.c_cc(VTIME) = 1.toUByte
      if tcsetattr(STDIN_FILENO, TCSAFLUSH, raw) != 0 then
        throw new RuntimeException("Could not set terminal settings")
      try f
      finally tcsetattr(STDIN_FILENO, TCSAFLUSH, original)

  def readByte(): Option[Byte] =
    Zone:
      val buffer = alloc[Byte]()
      unistd.read(STDIN_FILENO, buffer, 1.toCSize) match
        case 1 => Some(!buffer)
        // A timeout returns 0. If a signal arrives during read it may fail
        // with EINTR, which we treat the same as a timeout.
        case _ => None

@main def watchSize(): Unit =
  Resize.install()
  Terminal.withRawMode:
    var byte = Terminal.readByte()
    while byte != Some('q'.toByte) do
      if Resize.resized then
        Resize.resized = false
        val (rows, cols) = terminalSize()
        print(s"$cols columns x $rows rows\r\n")
        System.out.flush()
      byte = Terminal.readByte()
