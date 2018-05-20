
lazy val scripts = project
  .enablePlugins(JavaAppPackaging, ScoverageSbtPlugin)
  .settings(
    inThisBuild(List(
      organization := "org.tmt",
      scalaVersion := "2.12.4",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "sequencer-scripts",
    libraryDependencies ++= Seq(
      Libs.`sequencer-framework`,
      Libs.`scalaTest`,
    ),
    publishTo := {
      val base = "http://localhost:4000/maven/"
      if (version.value.endsWith("SNAPSHOT")) Some("snapshots" at base + "snapshots")
      else Some("releases" at base + "releases")
    }
  )
