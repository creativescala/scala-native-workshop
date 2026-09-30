# POSIX, Man Pages, and Bindings

In the previous section we called `tcgetattr` and `tcsetattr` without asking where they came from.
To write our own bindings we need to answer that question.
This will tell us where to find the functions we want to call, how to read their documentation, and what C types we need to map to Scala.


## Where System Calls Come From

The operating system kernel manages the hardware, the processes, and the tty driver we met earlier.
Our program can't touch any of these directly.
Instead it asks the kernel to act on its behalf by making a *system call*.

We rarely make system calls directly.
A system call is a special CPU instruction with arguments in particular registers, and the details differ between operating systems and processors.
Instead we call ordinary C functions in the C standard library, *libc*, which make the system call for us.
Some of these functions are thin wrappers around a single system call.
`read`, for example, does little more than pass its arguments to the kernel's `read` system call.
Others do more work.
On Linux there is no `tcgetattr` system call.
The libc function `tcgetattr` calls the more general `ioctl` system call instead, and converts the result into a `termios` struct.

In practice people use "system call" to mean any of these functions, and so will we.
What matters to us is that they are all C functions in libc, and Scala Native can call C functions.


## POSIX

Linux and macOS are different operating systems with different kernels, yet the code in the previous section runs on both.
This is thanks to [POSIX][posix], a standard that defines the interface a Unix-like operating system provides.
POSIX specifies the names of functions like `read` and `tcgetattr`, what arguments they take, how they behave, and how they report errors.
It also specifies shell commands like `stty` and `cat`.

Linux and macOS both implement (most of) POSIX, so a program written against POSIX runs on both.
Scala Native's `scala.scalanative.posix` package is a binding to this standard.
When we need something that isn't in POSIX, such as Linux's `epoll` or macOS's `kqueue`, our program is no longer portable.


## Man Pages

Traditional Unix documentation is provided by man pages, which we access by running the `man` command.
For example, `man tcgetattr` will display the man page for the `tcgetattr` function we used earlier.
The trouble with man pages is they require us to know the answer (e.g. "tcgetattr") and tell us the question ("what is the function to set terminal attributes").
Usually we want things the other way around.
Although we can search man pages by keyword, using `man -k`, in our experience a much better approach is to put the question to an LLM.
Your average LLM will know all the arcana scattered across the Unix man pages, and will at least be able to point you in the right direction if it doesn't get all the details right itself.

Once you know what you're looking for, man pages are the reference.
They are divided into numbered sections.
The ones we care about are:

- section 2, system calls, such as `read` and `ioctl`;
- section 3, library functions, such as `tcgetattr`; and
- section 3p, the POSIX standard itself.

The same name can appear in several sections, so we specify the section we want.
For example, `man 1 printf` describes the shell command, while `man 3 printf` describes the C function.

Section 3p is the most useful when we care about portability, as it describes what POSIX guarantees rather than what one particular system does.
On Linux it may need installing (it's included in Arch's `man-pages` package, and is `manpages-posix-dev` on Debian and Ubuntu).
macOS doesn't have it, but its section 2 and 3 pages are close to POSIX.

A man page has several parts, but two matter most when we're writing a binding:

- *SYNOPSIS* gives the header to `#include` and the C declaration of the function. This is what we translate into Scala.
- *RETURN VALUE* and *ERRORS* describe how the function reports failure. Most return `-1` and set a global variable called `errno` to a code that says what went wrong.

The rest of the page describes what the function does, and is well worth reading when a call doesn't behave as you expect.


## From C Types to Scala Types

Let's look at the SYNOPSIS for `read` in `man 3p read`.

```c
#include <unistd.h>

ssize_t read(int fildes, void *buf, size_t nbyte);
```

The first line tells us that the function is defined in the `unistd.h` *header file*.
A header file is how C declares an interface.
This isn't so important to us, except we should know that the system call bindings provided by Scala Native are organized by header file.
The Scala Native package `scala.scalanative.posix.unistd` corresponds to this header file, and within it we will find a binding for `read`.

The C declaration tells us that `read` takes a file descriptor, a pointer to a buffer, and the number of bytes to read. 
C function parameter declarations use the reverse order to Scala: type then name, rather than name followed by type. So `int fildes` means an integer parameter called `fildes`. 
By C convention file descriptors are represented by `int`. 
C is not a very type-safe language.

A `*` means a pointer, so `void *` means a pointer to memory.
As the `void` type has no known size or underlying representation, a `void *` is a pointer to an area of memory that the user program can interpret however it sees fit. `read` will treat it like a sequence of bytes and write `nbyte` or fewer bytes into it.
You might wonder how this can possibly be safe if there are no constraints on the memory represented by the pointer.
The answer is quite simple: it isn't.
We can ask `read` to write some bytes into any memory location.
Hopefully our program will crash if we do this, but that's the best case.
In the worst case it silently continues with corrupted memory.
This is how security holes are made.

Finally `size_t` is an unsigned integer type used to represent the size of data in bytes. The machine architecture determines what `size_t` corresponds to. On a 32-bit machine we'd expect a 32-bit unsigned integer, while it will usually be 64-bits on a 64-bit machine. Whatever it is, `size_t` is guaranteed to be large enough to represent the maximum number of bytes any single value can occupy.

The `ssize_t` in front of `read` is the return type of the function.
`ssize_t` is the signed counterpart to `size_t`, so it can represent negative numbers.
`read` returns the number of bytes it actually read, or `-1` on error.

To call this from Scala we need Scala equivalents for each of the C types.
Scala Native provides these as type aliases in `scala.scalanative.unsafe`.
Here are the ones you'll meet most often.

| C type           | Scala Native type | Underlying Scala type |
|------------------|-------------------|-----------------------|
| `char`           | `CChar`           | `Byte`                |
| `short`          | `CShort`          | `Short`               |
| `int`            | `CInt`            | `Int`                 |
| `unsigned int`   | `CUnsignedInt`    | `UInt`                |
| `long`           | `CLong`           | `Size`                |
| `long long`      | `CLongLong`       | `Long`                |
| `double`         | `CDouble`         | `Double`              |
| `size_t`         | `CSize`           | `USize`               |
| `ssize_t`        | `CSSize`          | `Size`                |
| `char *`         | `CString`         | `Ptr[CChar]`          |
| `void *`         | `CVoidPtr`        | `Ptr[?]`              |
| `T *`            | `Ptr[T]`          |                       |

A few of these need explanation.

- C doesn't fix the size of its integer types. `int` is 32 bits on every system we care about, and so is `CInt`. `long`, however, is 64 bits on 64-bit Linux and macOS and 32 bits on 32-bit systems. `Size` and `USize` are Scala Native types whose size matches the platform's pointer size, which is also the size of `long` on Linux and macOS.
- In C a pointer to any type `T` is written `T *`. In Scala Native it's `Ptr[T]`, which we have already met: `alloc[termios]()` returned a `Ptr[termios]`.

Putting this together, `read` becomes

```scala
def read(fildes: CInt, buf: CVoidPtr, nbyte: CSize): CSSize
```

If you look at Scala Native's binding you'll see it returns `CInt` rather than `CSSize`.
In practice this makes no difference, as both Linux and macOS limit a single `read` to less than 2GB, which fits in an `Int`.


## Our First Binding

We now know how to translate a C declaration into Scala types.
The final step is telling Scala Native that a Scala method is really a C function.
We do this with an `extern` object.

```scala
import scala.scalanative.unsafe.*

@extern
object MyBindings:
  def cFunctionName(param: ParamType): ReturnType = extern
```

The object is annotated with `@extern`, and each method has the same name as the C function it binds to.
Instead of a body, each method has `extern`, which tells Scala Native the implementation lives elsewhere.
We call these methods like any other Scala method.

@:exercise(Is It a Terminal?)
Programs often behave differently depending on whether they're connected to a terminal.
For example, `ls` prints its output in columns when writing to a terminal, but one file per line when writing to a pipe.
Many tools similarly turn off colored output when they aren't writing to a terminal.
They find this out using the POSIX function `isatty`.

1. Look up `isatty` using `man isatty` and find its C declaration.
2. Write an `extern` object that binds `isatty`. Scala Native already has a binding in `scala.scalanative.posix.unistd`, but writing our own is good practice.
3. Write a program that uses your binding to print whether standard input (file descriptor 0) and standard output (file descriptor 1) are terminals.

Try running your program in these three ways and check the output makes sense.

```bash
scala-cli run IsATty.scala
scala-cli run IsATty.scala < /dev/null
scala-cli run IsATty.scala | cat
```
@:@

@:solution
The C declaration is

```c
#include <unistd.h>

int isatty(int fildes);
```

so the binding is

```scala
import scala.scalanative.unsafe.*

@extern
object Unistd:
  def isatty(fd: CInt): CInt = extern

@main def isATty(): Unit =
  println(s"stdin: ${Unistd.isatty(0)}, stdout: ${Unistd.isatty(1)}")
```

Running it the three ways gives

```
stdin: 1, stdout: 1
stdin: 0, stdout: 1
stdin: 1, stdout: 0
```
@:@

Notice that `isatty` returns an `int`, not a boolean.
C didn't have a boolean type until C99, and even now most of the POSIX API uses `int` instead: `0` means false, and any other value means true.
POSIX says `isatty` returns exactly `1` for true, but the idiomatic way to convert a C boolean to a Scala `Boolean` is to test for non-zero, as in `Unistd.isatty(0) != 0`.
Scala Native does have a `CBool` type, but it corresponds to C's newer `bool` type.
It's not correct for functions that return `int`, even ones where the `int` means true or false.

So how does Scala Native find the C function?
When Scala Native compiles our program, a call to an `extern` method becomes a call to a C function of the same name, which our program doesn't define.
The final step of building a native program is *linking*, where the linker combines our compiled code with libraries and connects each call to a function's definition.
libc is always linked, so the linker finds `isatty` there.
Functions in other libraries need the library named with the `@link` annotation on the `extern` object.
For example, `@link("m")` links the maths library `libm`.

This has a couple of consequences worth knowing about.
The linker only matches names.
If we misspell `isatty` as `isaty` our program compiles fine, but linking fails with an error like

```
undefined reference to `isaty'
```

Worse, nothing checks that the types in our binding match the C declaration.
If we get them wrong our program will compile, link, and quite possibly appear to work, until it doesn't.
This is why it's important to carefully read the man page.


## Where POSIX Runs Out

The man page gives us enough to write a binding for `read`, but not every function is so simple.
POSIX specifies names and behavior, but it deliberately leaves many details up to each system.
Two matter a lot when writing bindings.

The first is the *layout of structs*.
POSIX says the `termios` struct has fields called `c_iflag`, `c_oflag`, `c_cflag`, `c_lflag`, and `c_cc`.
It doesn't say what order they come in, how big they are, or what other fields the struct contains.
Linux and macOS disagree on all of these.
On macOS the flag fields are 64 bits, and on Linux they're 32 bits.
To call `tcgetattr` directly we'd need a different struct definition for each system.
Scala Native avoids this by calling a small C function that it compiles alongside our program.
This C function calls the real `tcgetattr` and copies the result into a struct with a fixed layout.
That's the struct `alloc[termios]()` gave us.

The second is the *value of constants*.
POSIX says there are constants called `ECHO` and `ICANON`, but not what their values are.
In C this doesn't matter, as the values come from header files when the program is compiled.
Scala Native doesn't read C header files, so it needs another way to get them.
Again the answer is a small C function per constant, which returns the value from the header.
This is why the `ECHO` in Scala Native's binding is a function (`def ECHO: CInt = extern`), not a Scala constant.

We'll hit this problem ourselves soon.
To find the size of the terminal we call `ioctl` with the request `TIOCGWINSZ`.
Scala Native binds `ioctl`, but not `TIOCGWINSZ`, and the value differs by system: `0x5413` on Linux and `0x40087468` on macOS.
When we write our own bindings we'll have to deal with this, either by writing a little C, or by knowing the values for the systems we support.

[posix]: https://en.wikipedia.org/wiki/POSIX
