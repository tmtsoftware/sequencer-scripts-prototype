import sbt._

object Libs {
  val version               = "0.1.0-SNAPSHOT"
  val `sequencer-framework` = "org.tmt" %% "sequencer-framework" % version
  val `scalaTest`           = "org.scalatest" %% "scalatest" % "3.0.4" % Test
  val `mockito-core`        = "org.mockito" % "mockito-core" % "2.16.0" % Test //MIT License
}

object Akka {
  val Version = "2.5.11"

  val `akka-stream`        = "com.typesafe.akka" %% "akka-stream"        % Version
  val `akka-typed`         = "com.typesafe.akka" %% "akka-actor-typed"   % Version
  val `akka-typed-testkit` = "com.typesafe.akka" %% "akka-testkit-typed" % Version
}
