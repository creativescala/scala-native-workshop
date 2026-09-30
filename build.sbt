/*
 * Copyright 2015-2020 Creative Scala
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import scala.sys.process.*
import creativescala.ExternalLink
import laika.ast.Path
import laika.config.LinkConfig
import laika.config.ApiLinks
import laika.theme.Theme
import laika.helium.config.TextLink
import scala.scalanative.build.*

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / organization := Settings.organization
ThisBuild / organizationName := Settings.organizationName
ThisBuild / startYear := Settings.startYear
ThisBuild / licenses := Seq(Settings.license)
ThisBuild / developers := List(
  tlGitHubDev(Settings.githubHandle, Settings.fullName)
)

ThisBuild / crossScalaVersions := List(Settings.scalaVersion)
ThisBuild / scalaVersion := Settings.scalaVersion
ThisBuild / semanticdbEnabled := true
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))
ThisBuild / tlSitePublishBranch := Some("main")

// Run this (build) to do everything involved in building the project
commands += Command.command("build") { state =>
  "clean" ::
    "compile" ::
    "test" ::
    "nativeLink" ::
    "scalafixAll" ::
    "scalafmtAll" ::
    "scalafmtSbt" ::
    // "headerCreateAll" ::
    "docs / tlSite" ::
    // "githubWorkflowGenerate" ::
    "dependencyUpdates" ::
    "reload plugins; dependencyUpdates; reload return" ::
    state
}

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
    Dependencies.munit.value,
    Dependencies.munitScalaCheck.value
  )
)

// This project exists only to aggregate the other projects. If you create any
// additional sub-projects you should aggregate them here.
lazy val root = project.aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(
    commonSettings,
    moduleName := s"${Settings.module}-core",
    Compile / run / fork := true,
    libraryDependencies ++= Seq(
      // Dependencies.catsEffect.value,
      Dependencies.fs2.value,
      Dependencies.fs2Io.value,
      Dependencies.parsley.value,
      Dependencies.declineDerive.value
    )
  )
  .enablePlugins(ScalaNativePlugin)

lazy val docs =
  project
    .in(file("docs"))
    .settings(
      tlSiteApiUrl := Some(
        sbt.url(Settings.javadoc)
      ),
      laikaConfig := laikaConfig.value.withConfigValue(
        LinkConfig.empty
          .addApiLinks(ApiLinks(baseUri = Settings.javadoc))
      ),
      mdocIn := file("docs/src/pages"),
      mdocVariables := {
        mdocVariables.value ++ Map()
      },
      Laika / sourceDirectories ++=
        Seq(
          // (examples.js / Compile / fastOptJS / artifactPath).value
          //   .getParentFile() / s"${(examples.js / moduleName).value}-fastopt"
        ),
      laikaTheme := CreativeScalaTheme.empty
        .withHome(
          TextLink
            .internal(
              laika.ast.Path.Root / "README.md", {
                Settings.projectName
              }
            )
        )
        .withCommunity(
          ExternalLink(Settings.community, "Community")
        )
        .withApi(
          ExternalLink(Settings.javadoc, "API")
        )
        .withSource(
          ExternalLink(Settings.source, "Source")
        )
        .addJs(Path.Root / "main.js")
        .build,
      laikaExtensions ++= Seq(
        laika.format.Markdown.GitHubFlavor,
        laika.config.SyntaxHighlighting
      ),
      tlSite := Def
        .sequential(
          // (examples.js / Compile / fastLinkJS),
          mdoc.toTask(""),
          laikaSite
        )
        .value
    )
    .enablePlugins(TypelevelSitePlugin)

// lazy val unidocs = project
//   .in(file("unidocs"))
//   .enablePlugins(TypelevelUnidocPlugin) // also enables the ScalaUnidocPlugin
//   .settings(
//     name := s"${Settings.module}-docs",
//     ScalaUnidoc / unidoc / unidocProjectFilter :=
//       inAnyProject -- inProjects(
//         docs,
//         examples.jvm,
//         examples.js
//       )
//   )

// The examples project will not get published. It exists to hold code that is
// used in your documentation, which is usually Scala.js code though you can
// write JVM code if that works better for you.
// lazy val examples = crossProject(JSPlatform, JVMPlatform)
//   .in(file("examples"))
//   .settings(
//     commonSettings,
//     moduleName := s"${Settings.module}-examples",
//     // Used in the example code we've written. Delete if you don't need it.
//     libraryDependencies += "org.creativescala" %%% "doodle" % "0.31.0",
//     mimaPreviousArtifacts := Set.empty
//   )
// .jvmConfigure(
//   _.dependsOn(core.jvm)
// )
// .jsConfigure(
//   _.dependsOn(core.js)
// )
