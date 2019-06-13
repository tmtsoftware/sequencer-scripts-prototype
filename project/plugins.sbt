classpathTypes += "maven-plugin"

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.3")
addSbtPlugin("io.get-coursier"  % "sbt-coursier"        % "1.0.2")
addSbtPlugin("org.scoverage"    % "sbt-scoverage"       % "1.5.1")
addSbtPlugin("io.spray"         % "sbt-revolver"        % "0.9.1")
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.3.2")