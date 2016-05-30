name := "sj-regular-streaming-engine"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka_2.11" % "0.9.0.1"
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
    exclude("org.slf4j", "slf4j-simple"),
  "net.openhft" % "chronicle-queue" % "4.2.6"
//  UNUSED OR COVERED WITH TSTREAMS OR THIRD PARTY
//  "com.aerospike" % "aerospike-client" % "3.2.1"
)

assemblyMergeStrategy in assembly := {
  case PathList("scala-xml.properties") => MergeStrategy.first
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "log4j", xs@_*) => MergeStrategy.first
  case "library.properties" => MergeStrategy.concat
  case "log4j.properties" => MergeStrategy.concat
//  case PathList("com", "google", "guava", xs@_*) => MergeStrategy.last
//  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
//  case PathList("com", "fasterxml", "jackson", xs@_*) => MergeStrategy.first
//  case PathList("com", "thoughtworks", xs@_*) => MergeStrategy.first
//  case PathList("org", "apache", "commons", xs@_*) => MergeStrategy.first
//  case PathList("org", "apache", "log4j", xs@_*) => MergeStrategy.first
//  case PathList("org", "apache", "hadoop", xs@_*) => MergeStrategy.first
//  case PathList("io", "netty", xs@_*) => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
