name := "sj-stub-module"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-streaming_2.11" % "1.6.1",
  "org.apache.spark" % "spark-streaming-kafka_2.11" % "1.6.1"
)

assemblyMergeStrategy in assembly := {
  case PathList("com", "google", "common", xs @ _*) => MergeStrategy.first
  case PathList("org", "apache", "commons", xs @ _*) => MergeStrategy.first
  case PathList("org", "apache", "spark", xs @ _*) => MergeStrategy.first
  case PathList("org", "apache", "log4j", xs @ _*) => MergeStrategy.first
  case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.first
  case PathList("org", "apache", "hadoop", xs @ _*) => MergeStrategy.first
  case PathList("io", "netty", xs @ _*) => MergeStrategy.first
  case PathList("io", "dropwizard", xs @ _*) => MergeStrategy.first
  case PathList("com", "codahale", xs @ _*) => MergeStrategy.first
  case PathList("javax", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case "log4j.properties"                            => MergeStrategy.concat
  case "unwanted.txt"                                => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}