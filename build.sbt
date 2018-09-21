
lazy val `sequencer-scripts` = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    inThisBuild(List(
      organization := "org.tmt",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    scalaSource in Compile := { (baseDirectory in Compile)(_ / "scripts") }.value,
    scalaSource in Test := { (baseDirectory in Test)(_ / "tests") }.value,
    resourceDirectory in Compile := { (baseDirectory in Compile)(_ / "configs") }.value,
    name := "sequencer-scripts",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      Libs.`ocs-framework`,
      Libs.`ocs-test-kit`,
      Libs.`scalaTest`,
    ),
    publishTo := {
      val base = "http://localhost:4000/maven/"
      if (version.value.endsWith("SNAPSHOT")) Some("snapshots" at base + "snapshots")
      else Some("releases" at base + "releases")
    }
  )
