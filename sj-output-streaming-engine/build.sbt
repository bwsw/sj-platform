name := "sj-output-streaming-engine"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.elasticsearch" % "elasticsearch" % "2.3.2",
  "com.google.guava" % "guava" % "18.0"
)

dependencyOverrides ++= Set(
  "com.google.guava" % "guava" % "18.0"
)