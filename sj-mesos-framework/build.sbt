import sbtassembly.PathList

name := "sj-mesos-framework"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.mesos" % "mesos" % "0.28.1",
  "net.databinder" % "unfiltered-filter_2.11" % "0.8.4",
  "net.databinder" % "unfiltered-jetty_2.11" % "0.8.4"
//  UNUSED OR COVERED WITH TSTREAMS OR THIRD PARTY
//  "org.json4s" % "json4s-native_2.11" % "3.3.0"
//  "com.typesafe.play" % "play_2.11" % "2.5.3",
//  "com.twitter.common.zookeeper" % "lock" % "0.0.40",
//  "log4j" % "log4j" % "1.2.17",
//  "org.slf4j" % "slf4j-api" % "1.7.21"
)

resolvers += "Mesosphere Repo" at "http://downloads.mesosphere.io/maven"
//resolvers += "Twitter Repository" at "http://maven.twttr.com"

scalacOptions += "-Ylog-classpath"

assemblyMergeStrategy in assembly := {
  case PathList("scala-xml.properties") => MergeStrategy.first
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "commons", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case "library.properties" => MergeStrategy.concat
  case "log4j.properties" => MergeStrategy.concat
//  case PathList("com", "fasterxml", "jackson", xs@_*) => MergeStrategy.first
//  case PathList("com", "thoughtworks", xs@_*) => MergeStrategy.first
//  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
//  case PathList("org", "apache", "log4j", xs@_*) => MergeStrategy.first
//  case PathList("org", "apache", "hadoop", xs@_*) => MergeStrategy.first
//  case PathList("io", "netty", xs@_*) => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

