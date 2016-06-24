import sbtassembly.PathList

name := "sj-mesos-framework"
version := "1.0"
scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.mesos" % "mesos" % "0.28.1",
  "net.databinder" % "unfiltered-filter_2.11" % "0.8.4",
  "net.databinder" % "unfiltered-jetty_2.11" % "0.8.4"
)

libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "org.clapper" %% "grizzled-slf4j" % "1.0.2")

resolvers += "Mesosphere Repo" at "http://downloads.mesosphere.io/maven"

scalacOptions += "-Ylog-classpath"

assemblyMergeStrategy in assembly := {
  case PathList("scala-xml.properties") => MergeStrategy.first
  case PathList("scala", xs@_*) => MergeStrategy.first
  case PathList("org", "apache", "commons", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case "library.properties" => MergeStrategy.concat
  case "log4j.properties" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

assemblyJarName in assembly := "mesos-framework.jar"
target in assembly := file("/home/diryavkin_dn/toserve")
