name := "sj-output-streaming-engine"

version := "1.0"

scalaVersion := "2.11.7"

assemblyMergeStrategy in assembly := {
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case PathList("scala", xs@_*) => MergeStrategy.first
  case "log4j.properties" => MergeStrategy.concat
  case "library.properties" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
