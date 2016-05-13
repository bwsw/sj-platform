name := "sj-common"

version := "0.1"

scalaVersion := "2.11.7"

resolvers += "Twitter Repository" at "http://maven.twttr.com"

libraryDependencies ++= Seq(
  "org.mongodb" % "casbah_2.11" % "3.0.0",
  "org.mongodb.morphia" % "morphia" % "1.1.1",
  "com.twitter.common.zookeeper" % "lock" % "0.0.40"
)

libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.7.2"

libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.2"

libraryDependencies += "net.openhft" % "chronicle-queue" % "4.2.6"

libraryDependencies += "org.apache.kafka" % "kafka_2.11" % "0.9.0.1"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
  case PathList("scala-xml.properties") => MergeStrategy.first
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("com", "fasterxml", "jackson", xs@_*) => MergeStrategy.first
  case PathList("com", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "commons", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "spark", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "log4j", xs@_*) => MergeStrategy.first
  case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "hadoop", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case PathList("io", "netty", xs@_*) => MergeStrategy.first
  case PathList("io", "dropwizard", xs@_*) => MergeStrategy.first
  case PathList("com", "codahale", xs@_*) => MergeStrategy.first
  case PathList("javax", xs@_*) => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case "library.properties" => MergeStrategy.concat
  case "log4j.properties" => MergeStrategy.concat
  case "unwanted.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

