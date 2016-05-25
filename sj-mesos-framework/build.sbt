import sbtassembly.PathList

name := "sj-mesos-framework"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Mesosphere Repo" at "http://downloads.mesosphere.io/maven"
resolvers += "Twitter Repository" at "http://maven.twttr.com"

libraryDependencies += "com.twitter.common.zookeeper" % "lock" % "0.0.40"
libraryDependencies += "org.apache.mesos" % "mesos" % "0.28.1"
libraryDependencies += "log4j" % "log4j" % "1.2.17"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
libraryDependencies += "com.typesafe.play" % "play_2.11" % "2.5.3"
libraryDependencies += "net.databinder" % "unfiltered-jetty_2.11" % "0.8.4"
libraryDependencies += "net.databinder" % "unfiltered-filter_2.11" % "0.8.4"
libraryDependencies += "org.json4s" % "json4s-native_2.11" % "3.3.0"


scalacOptions += "-Ylog-classpath"

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

