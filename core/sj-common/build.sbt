name := "sj-common"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.mongodb" % "casbah_2.11" % "3.0.0",
  "org.mongodb.morphia" % "morphia" % "1.1.1",
  "org.apache.commons" % "commons-io" % "1.3.2",
  "com.typesafe" % "config" % "1.3.0"
//  UNUSED OR COVERED WITH TSTREAMS OR THIRD PARTY
//  "log4j" % "log4j" % "1.2.17",
//  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.1"
//  "com.twitter.common.zookeeper" % "lock" % "0.0.40",
//  "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.7.2",
//  "org.slf4j" % "slf4j-api" % "1.7.21"
//  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
//  "net.openhft" % "chronicle-queue" % "4.2.6",
//  "org.apache.kafka" % "kafka_2.11" % "0.9.0.1"
)

//resolvers += "Twitter Repository" at "http://maven.twttr.com"

//dependencyOverrides ++= Set(
//  "com.google.guava" % "guava" % "18.0"
//)

assemblyMergeStrategy in assembly := {
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case "library.properties" => MergeStrategy.concat
//  case PathList("com", "google", "guava", xs@_*) => MergeStrategy.last
//  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
//  case PathList("scala-xml.properties") => MergeStrategy.first
//  case PathList("com", "fasterxml", "jackson", xs@_*) => MergeStrategy.first
//  case PathList("com", "thoughtworks", xs@_*) => MergeStrategy.first
//  case PathList("org", "apache", "commons", xs@_*) => MergeStrategy.first
//  case PathList("org", "apache", "log4j", xs@_*) => MergeStrategy.first
//  case PathList("org", "apache", "hadoop", xs@_*) => MergeStrategy.first
//  case PathList("io", "netty", xs@_*) => MergeStrategy.first
//  case "application.conf" => MergeStrategy.concat
//  case "log4j.properties" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}


unmanagedJars in Compile += file("lib/t-streams.jar")