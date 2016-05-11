name := "ScalaMesos"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += "Mesosphere Repo" at "http://downloads.mesosphere.io/maven"
resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += "Twitter Repository" at "http://maven.twttr.com"


libraryDependencies += "com.typesafe.play" % "play-json_2.11" % "2.5.3"
libraryDependencies += "com.twitter.common.zookeeper" % "lock" % "0.0.40"
libraryDependencies += "org.apache.mesos" % "mesos" % "0.28.1"
libraryDependencies += "log4j" % "log4j" % "1.2.17"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.21"
libraryDependencies += "org.mongodb" % "casbah_2.11" % "3.0.0"


scalacOptions += "-Ylog-classpath"