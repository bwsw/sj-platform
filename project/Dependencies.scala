import sbt._

object Dependencies {

  object Versions {
    val scala = "2.12.1"
  }

  lazy val sjCommonDependencies = Def.setting(Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.22",
    ("com.bwsw" % "t-streams_2.12" % "2.3.1-SNAPSHOT")
      .exclude("org.slf4j", "slf4j-simple")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
      .exclude("io.netty", "netty")
      .exclude("io.netty", "netty-all")
      .exclude("org.scalatest", "scalatest_2.12"),
    ("org.mongodb" %% "casbah" % "3.1.1")
      .exclude("org.slf4j", "slf4j-api"),
    "org.mongodb.morphia" % "morphia" % "1.3.2",
    "org.apache.commons" % "commons-io" % "1.3.2",
    "com.typesafe" % "config" % "1.3.0",
    ("org.apache.kafka" % "kafka_2.12" % "0.10.1.1")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
      .exclude("io.netty", "netty"),
    ("org.apache.curator" % "curator-recipes" % "2.11.1")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
      .exclude("io.netty", "netty"),
    ("org.elasticsearch.client" % "transport" % "5.1.1")
      .exclude("com.fasterxml.jackson.core", "jackson-core"),
    "org.apache.logging.log4j" % "log4j-core" % "2.7",
    "org.apache.logging.log4j" % "log4j-api" % "2.7",
    "com.maxmind.geoip" % "geoip-api" % "1.3.1",
    "io.netty" % "netty-all" % "4.1.7.Final",
    "com.opencsv" % "opencsv" % "3.9",
    ("org.apache.avro" % "avro" % "1.8.1")
      .exclude("org.slf4j", "slf4j-api"),
    "com.aerospike" % "aerospike-client" % "3.3.4",
    "com.datastax.cassandra" % "cassandra-driver-core" % "3.2.0",
    "org.scalatest" % "scalatest_2.12" % "3.0.1" % "test",
    "org.eclipse.jetty" % "jetty-client" % "9.4.3.v20170317",
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % "2.8.8",
    ("org.everit.json" % "org.everit.json.schema" % "1.4.1")
      .exclude("commons-logging", "commons-logging")
  ))

  lazy val sjEngineCoreDependencies = Def.setting(Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.22" % "provided",
    ("org.apache.kafka" % "kafka_2.12" % "0.10.1.1" % "provided")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
      .exclude("io.netty", "netty"),
    "org.apache.commons" % "commons-lang3" % "3.5",
    ("com.mockrunner" % "mockrunner-jdbc" % "1.1.2" % "test")
      .exclude("jakarta-regexp", "jakarta-regexp")
      .exclude("xerces", "xerces"),
    "org.scalatest" % "scalatest_2.12" % "3.0.1" % "provided"
  ))

  lazy val sjRestDependencies = Def.setting(Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.22" % "provided",
    "com.typesafe.akka" %% "akka-http" % "10.0.3",
    ("org.apache.httpcomponents" % "httpclient" % "4.5.2")
      .exclude("commons-logging", "commons-logging"),
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.16"
  ))

  lazy val sjInputEngineDependencies = Def.setting(Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.22" % "provided",
    "com.hazelcast" % "hazelcast" % "3.7.3"
  ))

  lazy val sjRegularEngineDependencies = Def.setting(Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.22" % "provided",
    ("org.apache.kafka" % "kafka_2.12" % "0.10.1.1" % "provided")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
      .exclude("io.netty", "netty")
  ))

  lazy val sjBatchEngineDependencies = Def.setting(Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.22" % "provided",
    ("org.apache.kafka" % "kafka_2.12" % "0.10.1.1" % "provided")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
      .exclude("io.netty", "netty"),
    ("org.apache.curator" % "curator-recipes" % "2.11.1" % "provided")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("log4j", "log4j")
      .exclude("io.netty", "netty")
  ))

  lazy val sjOutputEngineDependencies = Def.setting(Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.22" % "provided",
    "org.elasticsearch.client" % "transport" % "5.1.1" % "provided",
    "org.apache.logging.log4j" % "log4j-core" % "2.7" % "provided",
    "org.apache.logging.log4j" % "log4j-api" % "2.7" % "provided",
    "org.eclipse.jetty" % "jetty-server" % "9.4.3.v20170317" % "test"
  ))

  lazy val sjFrameworkDependencies = Def.setting(Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.22" % "provided",
    "org.apache.mesos" % "mesos" % "0.28.1",
    "org.eclipse.jetty" % "jetty-runner" % "9.4.3.v20170317",
    ("org.apache.httpcomponents" % "httpclient" % "4.5.2")
      .exclude("commons-logging", "commons-logging")
  ))
}
