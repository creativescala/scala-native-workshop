# Real Applications in Scala Native

This is the content for the [Scala Native][scala-native] workshop delivered at Scala Days 2026. The overall goal of the workshop is to introduce developers to using Scala Native. In particular:

- project setup and sbt commands for using Scala Native;
- interfacing with the archaic world of C and system calls; and
- interacting with the terminal at a low and high level.

Our goal is to end with a simple terminal application built using Scala Native.



## What is Scala Native?

Before we go further, we should talk a bit about what Scala Native is.

Scala Native is an ahead-of-time (AOT) compiler and runtime for Scala. Most Scala code is compiled to JVM bytecode, which a Java Virtual Machine loads and just-in-time (JIT) compiles to machine code while the program runs. Scala Native instead compiles Scala to machine code before the program runs. The result is a standalone executable, much like the output of a C, Rust, or Go compiler, that runs without a JVM.

It helps to know a bit about how this works. The Scala compiler first compiles our code, as usual, but a Scala Native compiler plugin then produces an intermediate representation called NIR (Native Intermediate Representation) in place of JVM bytecode. When we link our program, Scala Native gathers the NIR for our code and all our dependencies, works out which code is actually reachable, optimizes the whole program, and hands the result to [LLVM][llvm]. LLVM then generates machine code for the target platform. This whole-program approach is why dependencies must be published specifically for Scala Native: the linker needs their NIR, not their bytecode.

There are three other parts of Scala Native that we'll meet during the workshop:

- A **runtime**, including a garbage collector. We still write normal Scala, with objects that are automatically managed, and don't have to free memory ourselves.
- A reimplementation of the parts of the **Java standard library** that Scala code commonly uses, such as `java.lang`, `java.util`, and `java.io`. There is no JVM, so these classes are written in Scala and compiled along with our code. Not all of the Java standard library is available, and Java libraries in general cannot be used.
- **Interoperability with C**. Scala Native provides types for pointers, C structs, C strings, and so on, along with a way to declare and call C functions. It also provides bindings to much of the C standard library and POSIX. This is how we'll talk to the operating system and the terminal.

Scala Native supports Scala 2.12, 2.13, and 3, and runs on Linux, macOS, and Windows.


## Why Scala Native?

If the JVM is so good, why would we want to avoid it? There are a few reasons.

**Startup time and memory usage.** The JVM is excellent for long-running servers, where it has time to warm up and JIT compile hot code. It's much less good for short-lived programs. A JVM application can take hundreds of milliseconds to start and use a lot of memory before it does anything useful. A Scala Native program starts in a few milliseconds and has a much smaller memory footprint. This matters for command-line tools, where the user notices every delay, and in other settings such as serverless functions and resource-constrained environments.

**Distribution.** A Scala Native program compiles to a single executable. Our users don't need to install a JVM, or the right version of a JVM, and we don't need to bundle one or write launcher scripts. We just give them the binary.

**Access to the platform.** The JVM deliberately hides the underlying operating system behind a portable abstraction. This is often what we want, but sometimes we need to get at the machine itself. Scala Native lets us call C libraries and system calls directly, and gives us low-level control over memory when we need it. For example, to build a terminal application we need to put the terminal into raw mode, which is not something the Java standard library supports. In Scala Native it's a few calls to the POSIX `termios` API.

**It's still Scala.** We get all the above while keeping the language we like, with its type system, functional programming support, and much of its ecosystem. Many popular libraries, including those from the Typelevel ecosystem, are published for Scala Native.

There are, of course, tradeoffs. The ecosystem is smaller than on the JVM, as only libraries published for Scala Native can be used and Java libraries are unavailable. Linking and optimizing a whole program is slower than compiling to bytecode, so the development loop is slower. For long-running programs a warmed-up JVM may achieve higher peak throughput. And tooling, such as debuggers and profilers, is less mature. It's worth mentioning [GraalVM Native Image][graal-native-image], which also produces native executables from JVM programs. It can use the Java ecosystem but gives less direct access to the underlying platform, and has its own set of restrictions.

In summary, Scala Native is a great fit for command-line tools, terminal applications, and other programs where fast startup, a small footprint, easy distribution, or access to the operating system matters. That's exactly the kind of application we'll build in this workshop.


[scala-native]: https://scala-native.org/en/latest/
[llvm]: https://llvm.org/
[graal-native-image]: https://www.graalvm.org/jdk25/reference-manual/native-image/
