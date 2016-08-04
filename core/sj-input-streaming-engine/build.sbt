name := "sj-input-streaming-engine"

version := "1.0"

scalaVersion := "2.11.7"

//  UNUSED OR COVERED WITH TSTREAMS OR THIRD PARTY
//  "com.aerospike" % "aerospike-client" % "3.2.1",
//  "io.netty" % "netty-all" % "4.1.0.CR7"

libraryDependencies += "com.hazelcast" % "hazelcast" % "3.6.4"

assemblyMergeStrategy in assembly := {
  case PathList("scala-xml.properties") => MergeStrategy.first
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "log4j", xs@_*) => MergeStrategy.first
  case "library.properties" => MergeStrategy.concat
  case "log4j.properties" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"