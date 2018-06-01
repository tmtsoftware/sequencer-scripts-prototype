classpathTypes += "maven-plugin"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.3")
addSbtPlugin("io.get-coursier"  % "sbt-coursier"        % "1.0.2")
addSbtPlugin("org.scoverage"    % "sbt-scoverage"       % "1.5.1")

resolvers ++= Seq(
  "GitBucket Snapshots Repository" at "http://localhost:4000/maven/snapshots",
  "GitBucket Releases Repository" at "http://localhost:4000/maven/releases"
)

// If repository is private, you have to add authentication information
credentials += Credentials("GitBucket Maven Repository", "localhost", "root", "root")
