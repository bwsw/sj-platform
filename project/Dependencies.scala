import sbt._

object Dependencies {

  object Versions {
    val scala = "2.11.8"
  }

  lazy val sjCommonDependencies = Def.setting(Seq(
    ("com.bwsw" % "t-streams_2.11" % "1.0-SNAPSHOT")
      .exclude("org.slf4j", "slf4j-simple")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j"),
    ("org.mongodb" % "casbah_2.11" % "3.1.1")
      .exclude("org.slf4j", "slf4j-api"),
    "org.mongodb.morphia" % "morphia" % "1.3.0",
    "org.apache.commons" % "commons-io" % "1.3.2",
    "com.typesafe" % "config" % "1.3.0",
    ("org.apache.kafka" % "kafka_2.11" % "0.10.1.0")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j"),
    ("org.apache.curator" % "curator-recipes" % "2.11.1")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
      .exclude("io.netty", "netty"),
    ("org.elasticsearch" % "elasticsearch" % "5.1.1")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
      .exclude("io.netty", "netty"),
    "org.elasticsearch.client" % "transport" % "5.1.1",
    "org.apache.logging.log4j" % "log4j-core" % "2.7",
    "org.apache.logging.log4j" % "log4j-api" % "2.7",
    "postgresql" % "postgresql" % "9.1-901.jdbc4",
    "com.maxmind.geoip" % "geoip-api" % "1.3.1"
  ))

  lazy val sjEngineCoreDependencies = Def.setting(Seq(
    ("org.apache.kafka" % "kafka_2.11" % "0.10.1.0")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j"),
    ("net.openhft" % "chronicle-queue" % "4.5.19")
      .exclude("org.slf4j", "slf4j-api")
  ))

  lazy val sjRestDependencies = Def.setting(Seq(
    "com.typesafe.akka" % "akka-http-experimental_2.11" % "2.4.11",
    ("org.everit.json" % "org.everit.json.schema" % "1.4.1")
      .exclude("commons-logging", "commons-logging"),
    ("org.apache.httpcomponents" % "httpclient" % "4.5.2")
      .exclude("commons-logging", "commons-logging"),
    "com.typesafe.akka" % "akka-slf4j_2.11" % "2.4.14"
  ))

  lazy val sjInputEngineDependencies = Def.setting(Seq(
    "com.hazelcast" % "hazelcast" % "3.7.3"
  ))

  lazy val sjRegularEngineDependencies = Def.setting(Seq(
    ("org.apache.kafka" % "kafka_2.11" % "0.10.1.0")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j"),
    "net.openhft" % "chronicle-queue" % "4.5.19"
  ))

  lazy val sjWindowedEngineDependencies = Def.setting(Seq(
    ("org.apache.kafka" % "kafka_2.11" % "0.10.1.0")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j"),
    ("org.apache.curator" % "curator-recipes" % "2.11.1")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
  ))

  lazy val sjOutputEngineDependencies = Def.setting(Seq(
    ("org.elasticsearch" % "elasticsearch" % "5.1.1")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
      .exclude("io.netty", "netty"),
    "org.elasticsearch.client" % "transport" % "5.1.1",
    "org.apache.logging.log4j" % "log4j-core" % "2.7",
    "org.apache.logging.log4j" % "log4j-api" % "2.7"
  ))

  lazy val sjFrameworkDependencies = Def.setting(Seq(
    "org.apache.mesos" % "mesos" % "0.28.1",
    "net.databinder" % "unfiltered-filter_2.11" % "0.8.4",
    "net.databinder" % "unfiltered-jetty_2.11" % "0.8.4",
    ("org.apache.httpcomponents" % "httpclient" % "4.5.2")
      .exclude("commons-logging", "commons-logging")
  ))
}
