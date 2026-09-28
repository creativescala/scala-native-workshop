# Project Setup for Scala Native

In this section we'll look at how to setup and build a Scala Native project with sbt, and some particular details of this project.


## Getting Started

1. Clone or fork this repository to create your own project.
2. Change the settings in `project/Settings.scala`.
3. Start sbt and run `build` to make it happen.

Most likely lots of stuff will be downloaded, Scala will churn for a while, and it will finish.

How do we setup a sbt project to build for Scala Native? What are the sbt commands that do Scala Native *stuff*?


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

`nativeLink`
