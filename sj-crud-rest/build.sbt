name := "sj-crud-rest"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "2.0-M2",
  "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.0-M2",
  "com.google.re2j" % "re2j" % "1.1",
  "org.everit.json" % "org.everit.json.schema" % "1.2.0",
  "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.4"
)

dependencyOverrides ++= Set("com.typesafe.akka" %% "akka-actor" % "2.4.1")

assemblyMergeStrategy in assembly := {
  case PathList("com", "google", "common", xs @ _*) => MergeStrategy.first
  case PathList("org", "apache", "commons", xs @ _*) => MergeStrategy.first
  case PathList("org", "apache", "log4j", xs @ _*) => MergeStrategy.first
  case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.first
  case PathList("org", "apache", "hadoop", xs @ _*) => MergeStrategy.first
  case PathList("javax", xs @ _*)         => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}