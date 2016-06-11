name := "sj-transaction-generator"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0",
  "com.twitter.common.zookeeper" % "lock" % "0.0.40",
  "log4j" % "log4j" % "1.2.17"
//  UNUSED OR COVERED WITH TSTREAMS OR THIRD PARTY
//  "org.scalatest" % "scalatest_2.11" % "3.0.0-M15",
)

resolvers += "Twitter" at "http://maven.twttr.com"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
