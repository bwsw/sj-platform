name := "ScalaMesos"

version := "1.0"

scalaVersion := "2.10.3"

resolvers += "Mesosphere Repo" at "http://downloads.mesosphere.io/maven"

libraryDependencies ++= Seq(
  "org.apache.mesos" % "mesos" % "0.14.2",
  "mesosphere" % "mesos-utils" % "0.0.6",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "org.mongodb" % "casbah_2.11" % "3.0.0"
)
