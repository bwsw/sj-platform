name := "sj-engine-core"

version := "1.0"

scalaVersion := "2.11.7"

assemblyMergeStrategy in assembly := {
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case "library.properties" => MergeStrategy.concat
  case "log4j.properties" => MergeStrategy.concat
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

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"