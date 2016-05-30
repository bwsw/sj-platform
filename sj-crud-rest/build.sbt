name := "sj-crud-rest"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.0-M2",
  "org.everit.json" % "org.everit.json.schema" % "1.2.0",
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "org.elasticsearch" % "elasticsearch" % "2.3.2",
  "org.apache.kafka" % "kafka_2.11" % "0.9.0.1"
      exclude("javax.jms", "jms")
      exclude("com.sun.jdmk", "jmxtools")
      exclude("com.sun.jmx", "jmxri")
      exclude("org.slf4j", "slf4j-simple"),
  "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.4"
//  UNUSED OR COVERED WITH TSTREAMS OR THIRD PARTY
//  "com.twitter.common.zookeeper" % "lock" % "0.0.40",
//  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.1",
//  "com.aerospike" % "aerospike-client" % "3.2.1",
//  "org.apache.zookeeper" % "zookeeper" % "3.4.6",
//  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "2.0-M2",
//  "com.google.re2j" % "re2j" % "1.1",
)

//resolvers += "Twitter Repository" at "http://maven.twttr.com"

dependencyOverrides ++= Set(
  "com.typesafe.akka" %% "akka-actor" % "2.4.1",
  "com.google.guava" % "guava" % "18.0"
)

assemblyMergeStrategy in assembly := {
  case PathList("scala-xml.properties") => MergeStrategy.first
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("com", "fasterxml", "jackson", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "commons", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "log4j", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case PathList("io", "netty", xs@_*) => MergeStrategy.first
  case PathList("org", "jboss", xs@_*) => MergeStrategy.first
  case PathList("org", "joda", xs@_*) => MergeStrategy.first
  case "log4j.properties" => MergeStrategy.concat
  case "library.properties" => MergeStrategy.concat
//  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
//  case PathList("com", "datastax", xs@_*) => MergeStrategy.first
//  case PathList("com", "google", "common", xs@_*) => MergeStrategy.first
//  case PathList("com", "google", "thirdparty", xs@_*) => MergeStrategy.first
//  case PathList("com", "thoughtworks", xs@_*) => MergeStrategy.first
//  case PathList("org", "luaj", xs@_*) => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
