name := "sj-transaction-generator"

version := "1.0"

scalaVersion := "2.11.7"

//libraryDependencies ++= Seq(
////  UNUSED OR COVERED WITH TSTREAMS OR THIRD PARTY
////  "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.0",
////  "com.twitter.common.zookeeper" % "lock" % "0.0.40",
////  "log4j" % "log4j" % "1.2.17"
////  "org.scalatest" % "scalatest_2.11" % "3.0.0-M15",
//)

//resolvers += "Twitter Repository" at "http://maven.twttr.com"

assemblyMergeStrategy in assembly := {
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case PathList("scala", xs@_*) => MergeStrategy.first
  case "log4j.properties" => MergeStrategy.concat
  case "library.properties" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
