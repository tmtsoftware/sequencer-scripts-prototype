import sbt._

object Libs {
  private val Org     = "com.github.tmtsoftware.esw-prototype"
  private val Version = "9d53637"

  val `sequencer-framework` = Org %% "sequencer-framework" % Version
  val `scalaTest`           = "org.scalatest" %% "scalatest" % "3.0.4" % Test
}

object Akka {
  val Version = "2.5.11"

  val `akka-stream`        = "com.typesafe.akka" %% "akka-stream"        % Version
  val `akka-typed`         = "com.typesafe.akka" %% "akka-actor-typed"   % Version
  val `akka-typed-testkit` = "com.typesafe.akka" %% "akka-testkit-typed" % Version
}
