import Dependencies._

ThisBuild / scalaVersion := scala212
ThisBuild / organization := "com.eed3si9n.jarjarabrams"
ThisBuild / organizationName := "eed3si9n"
ThisBuild / organizationHomepage := Some(url("http://eed3si9n.com/"))
ThisBuild / version := "0.3.2"
ThisBuild / description := "utility to shade Scala libraries"
ThisBuild / licenses := Seq(
  "Apache 2" -> new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / homepage := Some(url("https://github.com/eed3si9n/jarjar-abrams"))


ThisBuild / publishMavenStyle := true

ThisBuild / publishTo := Some("GitHub Package Registry" at "https://maven.pkg.github.com/ramencloud/jarjar-abrams")
ThisBuild / credentials ++= {
  (sys.env.get("PUBLISH_TO_GITHUB_USERNAME"), sys.env.get("PUBLISH_TO_GITHUB_TOKEN")) match {
    case (Some(user), Some(pass)) =>
      Seq(Credentials("GitHub Package Registry", "maven.pkg.github.com", user, pass))
    case _ => Nil
  }
}

lazy val jarjar = project
  .in(file("./jarjar"))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(nocomma {
    name := "jarjar"
    version := "1.7.3"
    crossPaths := false
    autoScalaLibrary := false
    libraryDependencies ++= Seq(
      "org.ow2.asm" % "asm" % "7.0",
      "org.ow2.asm" % "asm-commons" % "7.0",
      "org.apache.ant" % "ant" % "1.9.9",
      "org.apache.maven" % "maven-plugin-api" % "3.3.9",
      "org.apache.commons" % "commons-lang3" % "3.8.1",
      "junit" % "junit" % "4.12" % "it,test",
      "com.github.sbt" % "junit-interface" % "0.13.2" % "it,test"
    )

    mainClass := Some("org.pantsbuild.jarjar.Main")

    testFrameworks += new TestFramework("com.novocode.junit.JUnitFramework")

    IntegrationTest / fork := true
    IntegrationTest / envVars := Map(
      "JARJAR_CLASSPATH" -> (Runtime / fullClasspath)
        .value
        .map(_.data).mkString(System.getProperty("path.separator"))
    )
  })

lazy val core = project
  .enablePlugins(ContrabandPlugin)
  .settings(nocomma {
    name := "jarjar-abrams-core"

    crossScalaVersions := Vector(scala212, scala213, scala211, scala210)
    
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2.10.")) Nil
      else Vector(verify % Test)
    }
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

    Compile / managedSourceDirectories += (Compile / generateContrabands / sourceManaged).value
    Compile / generateContrabands / sourceManaged := baseDirectory.value / "src" / "main" / "contraband-scala"
    Test / sources := {
      val orig = (Test / sources).value
      if (scalaVersion.value.startsWith("2.10.")) Nil
      else orig
    }

    testFrameworks += new TestFramework("verify.runner.Framework")

    Compile / scalacOptions ++= {
      if (scalaVersion.value.startsWith("2.13.")) Vector("-Xlint")
      else Vector("-Xlint", "-Xfatal-warnings")
    }
  })
  .dependsOn(jarjar)
  .aggregate(jarjar)

lazy val sbtplugin = project
  .enablePlugins(SbtPlugin)
  .dependsOn(core)
  .settings(nocomma {
    name := "sbt-jarjar-abrams"

    Compile / scalacOptions ++= Vector("-Xlint", "-Xfatal-warnings")

    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Vector("-Xmx1024M", "-Dplugin.version=" + version.value)
    }
    pluginCrossBuild / sbtVersion := "1.2.8"
    scriptedBufferLog := false
  })

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/eed3si9n/jarjar-abrams"),
    "scm:git@github.com:eed3si9n/jarjar-abrams.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "eed3si9n",
    name = "Eugene Yokota",
    email = "@eed3si9n",
    url = url("http://eed3si9n.com")
  )
)
ThisBuild / pomIncludeRepository := { _ =>
  false
}
