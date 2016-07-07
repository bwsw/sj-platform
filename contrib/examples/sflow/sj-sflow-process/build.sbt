name := "sj-sflow-process"

version := "0.1"

scalaVersion := "2.11.7"

assemblyMergeStrategy in assembly := {
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case "library.properties" => MergeStrategy.concat
  case "log4j.properties" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

libraryDependencies += "com.maxmind.geoip" % "geoip-api" % "1.3.1"

assemblyJarName in assembly := "sj-sflow-process.jar"
