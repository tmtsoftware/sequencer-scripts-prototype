import sbt._

object Libs {
  private val Org = "com.github.tmtsoftware.esw-prototype"
//  private val Version = "7754822"
  private val Version = "0.1.0-SNAPSHOT"

  val `ocs-framework` = Org             %% "ocs-framework" % Version
  val `ocs-testkit`   = Org             %% "ocs-testkit"   % Version
  val `scalaTest`     = "org.scalatest" %% "scalatest"     % "3.0.4" % Test
}
