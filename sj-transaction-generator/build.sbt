name := "sj-transaction-generator"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Twitter Repository" at "http://maven.twttr.com"

libraryDependencies ++= Seq("com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0",
  "org.scalatest" % "scalatest_2.11" % "3.0.0-M15",
  "com.twitter.common.zookeeper" % "lock" % "0.0.40"
)

assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", "log4j", xs @ _*) => MergeStrategy.first
  case PathList("javax", xs @ _*)         => MergeStrategy.first
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
  case PathList("io", "netty", xs@_*) => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case "application.conf"                            => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}