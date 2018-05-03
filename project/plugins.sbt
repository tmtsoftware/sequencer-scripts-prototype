addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.3")
resolvers ++= Seq(
  "GitBucket Snapshots Repository" at "http://localhost:4000/maven/snapshots",
  "GitBucket Releases Repository" at "http://localhost:4000/maven/releases"
)

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// If repository is private, you have to add authentication information
credentials += Credentials("GitBucket Maven Repository", "localhost", "root", "root")
