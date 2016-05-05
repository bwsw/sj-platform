

name := "ScalaMesos"

version := "1.0"

scalaVersion := "2.10.3"

resolvers += "Mesosphere Repo" at "http://downloads.mesosphere.io/maven"
resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Twitter Repository" at "http://maven.twttr.com"

libraryDependencies ++= Seq(
  "org.apache.mesos" % "mesos" % "0.14.2",
  "mesosphere" % "mesos-utils" % "0.0.6",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "org.mongodb" % "casbah_2.11" % "3.0.0"
)

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.2.1"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.2"
libraryDependencies += "com.twitter.common.zookeeper" % "lock" % "0.0.40"