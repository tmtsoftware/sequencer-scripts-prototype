import sbt._

object Libs {
  //  private val Org     = "org.tmt"
  //  private val Version = "0.1.0-SNAPSHOT"

  private val Org     = "com.github.tmtsoftware.esw-prototype"
  private val Version = "4a31fc8"

  val `ocs-framework` = Org             %% "ocs-framework" % Version
  val `ocs-test-kit` = Org             %% "ocs-test-kit" % Version
  val `scalaTest`           = "org.scalatest" %% "scalatest"           % "3.0.4" % Test
}
