import sbt._

object Dependencies {

  object Versions {
    val scala = "2.11.8"
  }

  lazy val sjCommonDependencies = Def.setting(Seq(
    ("com.bwsw" % "t-streams_2.11" % "1.0-SNAPSHOT")
      .exclude("org.slf4j", "slf4j-simple"),
    "org.mongodb" % "casbah_2.11" % "3.0.0",
    "org.mongodb.morphia" % "morphia" % "1.1.1",
    "org.apache.commons" % "commons-io" % "1.3.2",
    "com.typesafe" % "config" % "1.3.0",
    "org.apache.kafka" % "kafka_2.11" % "0.9.0.1",
    "org.elasticsearch" % "elasticsearch" % "2.3.2",
    "postgresql" % "postgresql" % "9.1-901.jdbc4"
  ))

  lazy val sjEngineCoreDependencies = Def.setting(Seq(
    "net.openhft" % "chronicle-queue" % "4.2.6"
  ))

  lazy val sjRestDependencies = Def.setting(Seq(
    "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.0-M2",
    ("org.everit.json" % "org.everit.json.schema" % "1.2.0")
      .exclude("commons-logging", "commons-logging"),
    ("org.apache.httpcomponents" % "httpclient" % "4.5.2")
      .exclude("commons-logging", "commons-logging"),
    "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.4"
  ))

  lazy val sjInputEngineDependencies = Def.setting(Seq(
    "com.hazelcast" % "hazelcast" % "3.7.1"
  ))

  lazy val sjRegularEngineDependencies = Def.setting(Seq(
    ("org.apache.kafka" % "kafka_2.11" % "0.9.0.1")
      .exclude("org.slf4j", "slf4j-log4j12"),
    "net.openhft" % "chronicle-queue" % "4.2.6"
  ))

  lazy val sjOutputEngineDependencies = Def.setting(Seq(
    "org.elasticsearch" % "elasticsearch" % "2.3.2"
  ))

  lazy val sjFrameworkDependencies = Def.setting(Seq(
    "org.apache.mesos" % "mesos" % "0.28.1",
    "net.databinder" % "unfiltered-filter_2.11" % "0.8.4",
    "net.databinder" % "unfiltered-jetty_2.11" % "0.8.4",
    ("org.apache.httpcomponents" % "httpclient" % "4.5.2")
      .exclude("commons-logging", "commons-logging")

  ))

  lazy val sjSflowProcessDependencies = Def.setting(Seq(
    "com.maxmind.geoip" % "geoip-api" % "1.3.1"
  ))
}
