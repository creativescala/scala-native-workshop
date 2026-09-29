# Project Setup

In this section we'll look at how to set up and build a Scala Native project with sbt, and some particular details of this project.


## Getting Started

1. Clone or fork this repository to create your own project.
2. Change the settings in `project/Settings.scala`.
3. Start sbt and run `build` to make it happen.

Most likely lots of stuff will be downloaded, Scala will churn for a while, and it will finish successfully.
This is fairly standard for a Scala project (though `build` does some stuff you might not have seen before, such as checking dependencies have up-to-date versions).
What's interesting is that we've also built a native executable using Scala Native.
It is the file `core/target/scala-3.9.0/native-core`.
Run it and see what happens.
When you do so, notice how quickly it starts!

How do we set up an sbt project to build for Scala Native?
What are the sbt commands that do Scala Native *stuff*?


## Project Setup

There are two essential steps to use Scala Native in an sbt project. The first is to add the Scala Native sbt plugin to the project. In `project/plugins.sbt` you'll find the line

```scala
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.12")
```

Adding the Scala Native plugin gives our project the potential to use Scala Native, but we need to enable the plugin in a project for it to be active. If a project is only targeting Scala Native we can add `ScalaNativePlugin` to it. For example

```scala
lazy val core = project
  .in(file("core"))
  .settings(/* Settings here */)
  .enablePlugins(ScalaNativePlugin)
```

This creates a project that only builds for Scala Native. Sometimes we want a project that targets more than one platform. For this we can use [sbt-crossproject](https://github.com/portable-scala/sbt-crossproject).
This can be useful even if we don't want to target multiple platforms; running tests on the JVM can be faster than doing so via Scala Native.


## Dependencies

Adding a library dependency to a Scala Native project looks almost the same as on the JVM, but with one important difference. On the JVM we write a dependency using `%%`, like

```scala
libraryDependencies += "co.fs2" %% "fs2-core" % "3.13.0-M8"
```

The `%%` tells sbt to append the Scala binary version to the artifact name, so sbt looks for `fs2-core_3`. This is the JVM version of the library. As we discussed in the introduction, Scala Native needs the library's NIR, not its JVM bytecode, so a JVM artifact is no use to us. Libraries that support Scala Native publish a separate artifact that includes the Scala Native version as well as the Scala version, such as `fs2-core_native0.5_3`.

To get the right artifact we use `%%%` instead of `%%`.

```scala
libraryDependencies += "co.fs2" %%% "fs2-core" % "3.13.0-M8"
```

The `%%%` operator looks at the platform the project is building for and chooses the matching artifact: `_native0.5_3` for Scala Native, `_sjs1_3` for Scala.js, and plain `_3` for the JVM. This means the same dependency definition works in a cross-project that targets multiple platforms.

If you use `%%` by mistake in a Scala Native project, sbt will happily download the JVM artifact. Things then go wrong later, usually as a linking error complaining about missing definitions. So if you see errors from `nativeLink` about missing classes or methods, check your dependencies use `%%%`.

A few other points to be aware of:

- Only libraries that publish for Scala Native can be used. Most of the Typelevel ecosystem does, as do many other popular libraries, but check before relying on a library. The library's documentation, or searching [Maven Central](https://central.sonatype.com/) for artifacts with `_native0.5` in the name, will tell you.
- The Scala Native version in the artifact name (`0.5` above) must match the version of the Scala Native plugin. Libraries built for Scala Native 0.4 cannot be used with Scala Native 0.5.
- Java libraries cannot be used at all, as there is no JVM. Dependencies that use `%` (no Scala version) are therefore usually a mistake in a Scala Native project.

In this project the dependencies are defined in `project/Dependencies.scala`. You'll see that each dependency is wrapped in `Def.setting`, like

```scala
val fs2 = Def.setting("co.fs2" %%% "fs2-core" % fs2Version)
```

This is because `%%%` depends on sbt settings to know which platform we're building for, so the dependency must itself be a setting. We then refer to it with `.value` in `build.sbt`, as in

```scala
libraryDependencies ++= Seq(
  Dependencies.fs2.value,
  Dependencies.fs2Io.value
)
```

To add a new dependency, add a version and a definition to `project/Dependencies.scala`, and then add it to the `libraryDependencies` of the `core` project in `build.sbt`.


## sbt Commands

The `nativeLink` sbt command is the one that builds the native executable. Just compiling the code is not enough; we *must* run `nativeLink` to produce the executable. The executable's name is the value of the `moduleName` sbt setting, so change that value if you'd like a more informative name.

The `build` command is something I like to use before submitting a PR. It does a lot of checks, and takes a lot of time. You probably don't want to use it in this workshop, as iteration speed is more important than code quality. You can use the normal `compile`, `test`, and other sbt commands you are used to. Take a look at `build.sbt` if you want to see how `build` is defined.
