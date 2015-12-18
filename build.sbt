name := "microsoft-translator-scala-api"
organization := "com.smartelk"
version := "1.0"

scalaVersion := "2.11.7"
scalacOptions := Seq("-deprecation", "-feature")

resolvers ++= Seq(
  "Maven central" at "http://repo1.maven.org/maven2"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %%  "akka-actor"    % "2.3.9",
  "org.scalaj" %% "scalaj-http" % "2.2.0",
  "org.json4s" % "json4s-native_2.11" % "3.3.0",
  "org.scalatest" %%  "scalatest"   % "2.2.1" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test"
)