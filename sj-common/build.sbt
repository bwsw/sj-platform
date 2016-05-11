name := "sj-common"

version := "0.1"

scalaVersion := "2.11.7"

resolvers += "Twitter Repository" at "http://maven.twttr.com"

libraryDependencies ++= Seq(
  "org.mongodb" % "casbah_2.11" % "3.0.0",
  "org.mongodb.morphia" % "morphia" % "1.1.1",
  "com.twitter.common.zookeeper" % "lock" % "0.0.40"
)

libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.7.2"

libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.2"

libraryDependencies += "net.openhft" % "chronicle-queue" % "4.2.6"

libraryDependencies += "org.apache.kafka" % "kafka_2.11" % "0.9.0.1"



