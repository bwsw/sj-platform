name := "sj-regular-streaming-engine"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka_2.11" % "0.9.0.1"
)

assemblyMergeStrategy in assembly := {
  //
  ////  case PathList("com", "google", "guava", xs@_*) => MergeStrategy.last
  ////
  ////  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
  case PathList("scala-xml.properties") => MergeStrategy.first
  case PathList("scala", xs@_*) => MergeStrategy.first
  //  case PathList("com", "fasterxml", "jackson", xs@_*) => MergeStrategy.first
  //  case PathList("com", "thoughtworks", xs@_*) => MergeStrategy.first
  //  case PathList("org", "apache", "commons", xs@_*) => MergeStrategy.first
  //////  case PathList("org", "apache", "spark", xs@_*) => MergeStrategy.first
  //  case PathList("org", "apache", "log4j", xs@_*) => MergeStrategy.first
  //////  case PathList("com", "esotericsoftware", xs@_*) => MergeStrategy.first
  //////  case PathList("org", "apache", "hadoop", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "log4j", xs@_*) => MergeStrategy.first
  ////  case PathList("io", "netty", xs@_*) => MergeStrategy.first
  //////  case PathList("io", "dropwizard", xs@_*) => MergeStrategy.first
  //////  case PathList("com", "codahale", xs@_*) => MergeStrategy.first
  //////  case PathList("javax", xs@_*) => MergeStrategy.first
  //////  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  //////  case "application.conf" => MergeStrategy.concat
  case "library.properties" => MergeStrategy.concat
  case "log4j.properties" => MergeStrategy.concat
  //////  case "unwanted.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
