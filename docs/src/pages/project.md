# Project Setup

In this section we'll look at how to setup and build a Scala Native project with sbt, and some particular details of this project.


## Getting Started

1. Clone or fork this repository to create your own project.
2. Change the settings in `project/Settings.scala`.
3. Start sbt and run `build` to make it happen.

Most likely lots of stuff will be downloaded, Scala will churn for a while, and it will finish successfully.
This is fairly standard for a Scala project (though `build` does some stuff you might not have seen before, such as checking dependencies have up-to-date versions.)
What's interesting is that we've also build a native executable using Scala Native.
It is the file `core/target/scala-3.9.0/native-core`.
Run it and see what happens.
When you do so, notice how quickly it starts!

How do we setup a sbt project to build for Scala Native? 
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
This can be useful even if we don't want to target multiple platforms, running tests on the JVM can be faster than doing so via Scala Native.


# Sbt Commands

The `nativeLink` sbt command is the one that builds the native executable. Just compiling the code is not enough; we *must* run `nativeLink` to produce the executable. The executable's name is the value of the `moduleName` sbt setting, so change that value if you'd like a more informative name.

The `build` command is something I like to use before submitting a PR. It does a lot of checks, and takes a lot of time. You probably don't want to use it in this workshop, as iteration speed is more important than code quality. You can use the normal `compile`, `test`, and other sbt commands you are used to. Take a look at `build.sbt` if you want to see how `build` is defined.
