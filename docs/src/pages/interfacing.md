# Interfacing with C

In this section we'll gain some experience writing bindings.
We'll work through a sequence of small exercises, each introducing one or two new ideas.
First we'll use the `termios` structure we've already seen to make calls to `read` that time out.
This will allow us to distinguish keycodes starting with `ESC` from the user pressing the Escape key.
We'll then work our way up to a binding that gets the terminal's size, which will require us to define a C struct in Scala and write a small amount of C code.

## Reading with a Timeout

Reading from `System.in` blocks until a character is available. This is a problem when reading from the terminal. As we know, some keys produce a sequence of characters. It's not always clear when a sequence has finished or we should continue reading. This is most obviously the case for the `ESC` character. When we see an `ESC` it could indicate the user has pressed the Escape key, or it could be the start of a character sequence. How do we tell?

The standard way to tell them apart (and this shows the hacked together nature of a lot of the terminal) is the duration between characters. If we see an `ESC` followed by a pause of a few hundred milliseconds we can assume it is the Escape key. If we see an `ESC` immediately followed by more bytes we assume it is the start of a sequence.

To implement this we need to be able to read with a timeout. The `termios` structure supports two fields, known as `VMIN` and `VTIME`, that allow us to change how the `read` system call works. [This article][vmin-vtime] gives a good overview of what the settings mean. You can also check the man pages.

Note that we can't use `System.in.read()` with these changes, as it will convert the timeout into a -1, which incorrectly indicates an end-of-file. We have to call `unistd.read` ourselves, which we introduced in the previous section.

@:exercise(Reading with a Timeout)
Change `withRawMode` so that `read` waits at most 0.1 seconds for input.
Then use this to tell the Escape key apart from the arrow keys.
@:@


@:solution
Here are the new concepts you'll need:

- An array inside a struct: `VMIN` and `VTIME` are indexes into the `c_cc` array. Assigning to an element updates the struct in place.
- `UByte`, as `c_cc` holds `cc_t`, which is `unsigned char`.
- Calling `read` directly, with a buffer we allocate.

We want `VMIN = 0` and `VTIME = n`, which means "return as soon as any input is available, or after n tenths of a second". A timeout makes `read` return 0. 

```scala
// In withRawMode, before calling tcsetattr
raw.c_cc(VMIN) = 0.toUByte
raw.c_cc(VTIME) = 1.toUByte
```

Now we can read a `Byte` on a timeout, returning `None` if no data is available.

```scala
def readByte(): Option[Byte] =
  Zone:
    val buffer = alloc[Byte]()
    unistd.read(STDIN_FILENO, buffer, 1.toCSize) match
      case 1 => Some(!buffer)
      case 0 => None
      case _ => throw new RuntimeException("Could not read input")
```

Distinguishing keys: after reading `ESC`, read again. A timeout means it was the Escape key. `[` followed by `A`–`D` is an arrow key.

```scala
Terminal.readByte().map:
  case 27 =>
    Terminal.readByte() match
      case Some('[') =>
        Terminal.readByte() match
          case Some('A') => Key.Up
          case Some('B') => Key.Down
          case Some('C') => Key.Right
          case Some('D') => Key.Left
          case _         => Key.Escape
      case _ => Key.Escape
  case byte => Key.Character(byte.toChar)
```
@:@

## Error Messages

This exercise is optional. None of the later exercises depend on it, so skip it if you're short on time.

So far, when a C function fails we throw an exception with a message like "Could not get terminal settings". That tells us *what* failed, but not *why*. As we saw in the previous section, most C functions report failure by returning `-1` and setting the global variable `errno` to a code saying what went wrong. The C function `strerror` converts this code into a human readable message. Scala Native binds `errno` in `scala.scalanative.libc.errno`, and `strerror` in `scala.scalanative.libc.string`.

@:exercise(Better Error Messages)
Write a method `fail(message: String): Nothing` that throws an exception including both `message` and the reason for the error given by `errno`. Use it in `withRawMode`. Test it by running your program with input redirected from `/dev/null`, which isn't a terminal.
@:@

@:solution
Here are the new concepts you'll need:

- `errno`, which is how C functions report the reason for an error.
- Converting a C string, `CString`, to a Scala `String` using `fromCString`. `strerror` returns a `CString`.

```scala
import scala.scalanative.libc.errno.errno
import scala.scalanative.libc.string.strerror

def fail(message: String): Nothing =
  val code = errno
  throw new RuntimeException(s"$message: ${fromCString(strerror(code))}")
```

We read `errno` into a local variable before doing anything else. `errno` is shared by every C function we call, so any other call could change it. For the same reason we should call `fail` immediately after the call that failed.

Running with `< /dev/null` gives

```
java.lang.RuntimeException: Could not get terminal settings: Inappropriate ioctl for device
```

The message mentions `ioctl`, which is the system call `tcgetattr` uses on Linux. We'll meet `ioctl` directly in the next exercise.
@:@


## How Much Input Is Waiting?

`ioctl` is the system call for everything that doesn't fit anywhere else. Its name is short for "input/output control", and it performs a device-specific operation on a file descriptor. The operation is chosen by a *request* number, and the request determines what the third argument means. A lot of terminal functionality is only available through `ioctl`.

One request is `FIONREAD`, which tells us how many bytes are waiting to be read. The third argument is a pointer to an `int`, and `ioctl` writes its result there. This is a very common pattern in C, known as an *out-parameter*. C functions can only return a single value, which is usually used to report errors, so results are returned by writing them to memory the caller provides. We saw this before with `tcgetattr`, which writes into the `termios` struct we give it.

Scala Native binds both `ioctl` and `FIONREAD` in `scala.scalanative.posix.sys.ioctl`. Its binding for `ioctl` looks like

```scala
def ioctl(fd: CInt, request: CLongInt, argp: Ptr[Byte]): CInt
```

@:exercise(Bytes Available)
Write a method `bytesAvailable(): Int` that returns the number of bytes waiting to be read on standard input. To try it out, write a program that calls `bytesAvailable` once a second and prints the result. You don't need raw mode for this. Type some characters and press Enter, and watch the count go up.
@:@

@:solution
Here are the new concepts you'll need:

- Out-parameters: allocate memory for the result, pass a pointer to it, and then read the result from the pointer using `!`.
- Casting pointers: the binding for `ioctl` expects a `Ptr[Byte]`, but we have a `Ptr[CInt]`. We can convert one to the other with `asInstanceOf`. This doesn't change the pointer, just what Scala Native thinks it points to.

```scala
import scala.scalanative.posix.sys.ioctl.{FIONREAD, ioctl}

def bytesAvailable(): Int =
  Zone:
    // Space for ioctl to write its result into
    val count = alloc[CInt]()
    if ioctl(STDIN_FILENO, FIONREAD, count.asInstanceOf[Ptr[Byte]]) != 0 then
      throw new RuntimeException("Could not get number of bytes available")
    // Read the value count points to
    !count
```

Why does Scala Native's binding take a `Ptr[Byte]` when the third argument can point to anything? In C, `ioctl` is declared as

```c
int ioctl(int fd, unsigned long request, ...);
```

The `...` means `ioctl` is *variadic*: it takes any number of extra arguments, of any type. Variadic functions are awkward to call from other languages. On some platforms, including macOS on Apple Silicon, variadic arguments are passed differently from normal arguments, so a binding that pretends `ioctl` takes exactly three arguments might work on Linux and fail on a Mac. Scala Native avoids this by calling a small C function that takes exactly three arguments and passes them on to `ioctl`. The third argument had to be given some type, and `void *` (which Scala Native writes as `Ptr[Byte]` here) is the natural choice.

`bytesAvailable` gives us another way to distinguish the Escape key from escape codes. After reading `ESC`, if more bytes are available, it's probably an escape code. Unlike the timeout approach, this doesn't wait at all, but it can be fooled if the rest of the escape code hasn't arrived yet. This might happen, for example, over a slow network connection.
@:@


## Terminal Size

We'd really like to know the size of the terminal, so we can draw things that fit on the screen. We can get this using the `ioctl` request `TIOCGWINSZ` (terminal I/O control get window size). This time the third argument is a pointer to a struct, which the `ioctl` fills in. From `man TIOCGWINSZ` on Linux (`man ioctl_tty` on older systems), the struct is

```c
struct winsize {
    unsigned short ws_row;
    unsigned short ws_col;
    unsigned short ws_xpixel;   /* unused */
    unsigned short ws_ypixel;   /* unused */
};
```

Scala Native binds neither `TIOCGWINSZ` nor `struct winsize`, so we must define them ourselves.

In Scala Native a struct is defined using one of the `CStruct` types, which are numbered by the number of fields they have. `CStruct4[A, B, C, D]` is a struct with four fields of types `A`, `B`, `C`, and `D`. Fields don't have names, but are accessed by position using `_1`, `_2`, and so on. We usually define a type alias for the struct, as in

```scala
type point = CStruct2[CInt, CInt]
```

and then allocate and use it like the `termios` struct we have already seen.

```scala
val p = alloc[point]()
p._1 = 3
p._2 = 4
```

@:exercise(Terminal Size)
Define `struct winsize` in Scala, and write a method `terminalSize(): (Int, Int)` that returns the number of rows and columns in the terminal. For now, hard-code the value of `TIOCGWINSZ`. It's `0x5413` on Linux, and `0x40087468` on macOS.
@:@

@:solution
Here are the new concepts you'll need:

- Defining a struct using `CStruct4`, and accessing its fields.
- `CLongInt`, the type of `ioctl`'s request, is an alias for `Size`. We convert an `Int` to a `Size` using `toSize`.

```scala
type winsize =
  CStruct4[CUnsignedShort, CUnsignedShort, CUnsignedShort, CUnsignedShort]

// 0x5413 on Linux; use 0x40087468 on macOS
val TIOCGWINSZ: CLongInt = 0x5413.toSize

def terminalSize(): (Int, Int) =
  Zone:
    val size = alloc[winsize]()
    if ioctl(STDOUT_FILENO, TIOCGWINSZ, size.asInstanceOf[Ptr[Byte]]) != 0 then
      throw new RuntimeException("Could not get terminal size")
    // _1 is ws_row and _2 is ws_col
    (size._1.toInt, size._2.toInt)
```

This is the same as the previous exercise, except `ioctl` writes into a struct instead of an `int`. We ask about standard output, as that's where we'll draw, but any file descriptor connected to the terminal will work.

Although the value of `TIOCGWINSZ` is different on Linux and macOS, the layout of `struct winsize` is the same. This means our definition of `winsize` is portable, but our value for `TIOCGWINSZ` is not.

`size._1` isn't very readable. If you'd like names like `size.ws_row`, you can define extension methods in the same way that Scala Native's `termiosOps` does.
@:@


## Terminal Size, Portably

Our code for getting the terminal size only works on the operating system we hard-coded `TIOCGWINSZ` for. To make it portable we need to get the value of `TIOCGWINSZ` from the C header files, which means writing some C. As we discussed in the previous section, this is exactly what Scala Native does for constants like `ECHO`.

The C code we need is very simple. It's a function that returns the value of `TIOCGWINSZ`.

```c
#include <sys/ioctl.h>

long workshop_tiocgwinsz(void) { return TIOCGWINSZ; }
```

`TIOCGWINSZ` is a macro, defined in `sys/ioctl.h`, so it only has a value when C code is compiled. The function makes this value available at run time. The return type is `long` to match the type of `ioctl`'s request argument in Scala Native's binding.

C has a single global namespace for functions. If two libraries define functions with the same name, linking will fail. For this reason it's conventional to prefix function names with the name of your library or project. Here we've used `workshop_`.

Scala Native compiles any C files it finds in a directory called `scala-native` in the project's resources, and links them with our program. In an sbt project that's `src/main/resources/scala-native/`. With the Scala CLI, specify a resource directory using `//> using resourceDir resources` and put your C files in `resources/scala-native/`.

We then bind to the C function using an `extern` object, as before. We don't want to call it `workshop_tiocgwinsz` in Scala, so we use the `@name` annotation to tell Scala Native the name of the C function.

```scala
@extern
object Example:
  @name("c_function_name")
  def scalaName(): CInt = extern
```

@:exercise(Portable Terminal Size)
Replace your hard-coded value for `TIOCGWINSZ` with the value from C.
@:@

@:solution
Here are the new concepts you'll need:

- Adding C code to a Scala Native project.
- Using `@name` when the Scala name differs from the C name.

With the C code above in `winsize.c`, the binding is

```scala
@extern
object WinSize:
  @name("workshop_tiocgwinsz")
  def TIOCGWINSZ: CLongInt = extern
```

and `terminalSize` uses `WinSize.TIOCGWINSZ` in place of the hard-coded value. Notice we've defined `TIOCGWINSZ` without parentheses, making it look like a constant. This is how Scala Native defines its own constants, such as `ECHO`.

If Scala Native doesn't find your C file you'll get a linking error like

```
undefined reference to `workshop_tiocgwinsz'
```

just like the misspelled `isatty` in the previous section.
@:@


## Redrawing on Resize

This final exercise is a stretch goal.

When the terminal is resized, the kernel sends the program running in it the `SIGWINCH` signal. A *signal* is a way for the kernel (or another process) to interrupt our program to tell it something has happened. We've already met some signals: Ctrl-C sends `SIGINT`, and Ctrl-Z sends `SIGTSTP`. Each signal has a default action, which for `SIGINT` is terminating the program and for `SIGWINCH` is doing nothing. We can change this by installing a *signal handler*, a function that is called when the signal arrives.

The simplest way to install a signal handler is the C function `signal`, which Scala Native binds in `scala.scalanative.libc.signal`.

```scala
def signal(sig: CInt, handler: CFuncPtr1[CInt, Unit]): CFuncPtr1[CInt, Unit]
```

The handler is a `CFuncPtr1[CInt, Unit]`, which is a pointer to a C function that takes one `CInt` argument (the signal number) and returns nothing. We can create one from a Scala function using `CFuncPtr1.fromScalaFunction`. There is one restriction: the Scala function cannot capture any local variables. A C function pointer is just the address of some code, with nowhere to store the values of captured variables. The function can refer to fields of top-level objects, however.

A signal handler can run at any time, interrupting whatever our program was doing. This makes it very easy to write handlers that go wrong in strange ways. The safest thing to do in a signal handler is as little as possible. Usually this means setting a flag, which the main loop of the program checks.

Scala Native doesn't bind `SIGWINCH`, which only recently joined the POSIX standard, so you'll need to get its value from C in the same way as `TIOCGWINSZ`. It's defined in `signal.h`.

@:exercise(Watching for Resizes)
Write a program that prints the terminal size when it starts, and again every time the terminal is resized. It should quit when the user presses `q`.

Hint: use the timeout from the first exercise, so that your main loop regularly checks for a resize even when the user isn't pressing any keys.
@:@

@:solution
Here are the new concepts you'll need:

- Signals and signal handlers.
- Creating C function pointers from Scala functions.

We add another function to our C file

```c
#include <signal.h>

int workshop_sigwinch(void) { return SIGWINCH; }
```

and bind it in our `extern` object

```scala
@name("workshop_sigwinch")
def SIGWINCH: CInt = extern
```

The signal handler sets a flag in a top-level object. It starts as `true`, so we print the size when the program starts. The flag is `@volatile` as it is changed by the signal handler, outside the normal flow of the program.

```scala
object Resize:
  @volatile var resized = true

  val handler = CFuncPtr1.fromScalaFunction[CInt, Unit](_ => resized = true)

  def install(): Unit = signal(WinSize.SIGWINCH, handler)
```

The main loop reads with a timeout, and checks the flag each time around.

```scala
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
```

There is one more wrinkle. If a signal arrives while our program is waiting in `read`, the `read` may fail with `errno` set to `EINTR` (interrupted system call). Our `readByte` from the first exercise throws an exception when `read` fails, which is not what we want here. The simplest fix is to treat a failed `read` as a timeout. A better fix is to check whether `errno` is `EINTR`, using what we learned earlier, and only treat that case as a timeout.

`signal` is the oldest and simplest way to install a signal handler. Its behavior varies between systems, and modern code should use `sigaction` instead. `sigaction` needs another struct, however, and `signal` is good enough for our purposes.
@:@

[vmin-vtime]: http://www.unixwiz.net/techtips/termios-vmin-vtime.html
