name := "sj-common"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.mongodb" % "casbah_2.11" % "3.0.0"
)

libraryDependencies += "org.redisson" % "redisson" % "2.2.11"

libraryDependencies += "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.7.2"

libraryDependencies += "org.apache.commons" % "commons-io" % "1.3.2"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"
