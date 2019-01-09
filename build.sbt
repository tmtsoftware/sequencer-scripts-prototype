
lazy val `sequencer-scripts` = project
  .in(file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    inThisBuild(List(
      organization := "com.github.tmtsoftware",
      scalaVersion := "2.12.8",
      version      := "0.1.0-SNAPSHOT"
    )),

    unmanagedSourceDirectories in Compile += (baseDirectory in Compile) (_ / "scripts").value,
    unmanagedSourceDirectories in Test += (baseDirectory in Test) (_ / "tests").value,
    unmanagedResourceDirectories in Compile += (baseDirectory in Compile) (_ / "configs").value,
    
    name := "sequencer-scripts",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      Libs.`ocs-framework`,
      Libs.`ocs-testkit`,
      Libs.`scalaTest`,
    )
  )
