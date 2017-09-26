/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import sbt._

object Dependencies {

  object Versions {
    val scala = "2.12.2"
  }

  lazy val sjCommonDependencies = Def.setting(Seq(
    "org.slf4j" % "slf4j-log4j12" % "1.7.24",
    ("com.bwsw" % "t-streams_2.12" % "3.0.5-SNAPSHOT")
      .exclude("org.slf4j", "slf4j-simple")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("org.slf4j", "slf4j-log4j12")
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
    ("org.elasticsearch.client" % "transport" % "5.5.1")
      .exclude("com.fasterxml.jackson.core", "jackson-core"),
    "org.apache.logging.log4j" % "log4j-core" % "2.8.2",
    "org.apache.logging.log4j" % "log4j-api" % "2.8.2",
    "com.maxmind.geoip" % "geoip-api" % "1.3.1",
    "io.netty" % "netty-all" % "4.1.7.Final",
    "com.opencsv" % "opencsv" % "3.9",
    ("org.apache.avro" % "avro" % "1.8.1")
      .exclude("org.slf4j", "slf4j-api"),
    "org.eclipse.jetty" % "jetty-client" % "9.4.3.v20170317",
    "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % "2.8.8",
    ("org.everit.json" % "org.everit.json.schema" % "1.4.1")
      .exclude("commons-logging", "commons-logging"),
    "org.scaldi" %% "scaldi" % "0.5.8",
    "org.mock-server" % "mockserver-netty" % "3.10.8" % "test",
    "com.carrotsearch" % "java-sizeof" % "0.0.5",
    ("de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "2.0.0")
      .exclude("org.slf4j", "slf4j-api")
  ))

  lazy val sjEngineCoreDependencies = Def.setting(Seq(
    "org.apache.commons" % "commons-lang3" % "3.5",
    ("com.mockrunner" % "mockrunner-jdbc" % "1.1.2" % "test")
      .exclude("jakarta-regexp", "jakarta-regexp")
      .exclude("xerces", "xerces"),
    ("org.apache.samza" % "samza-kafka_2.12" % "0.13.0" % "test")
      .exclude("org.apache.kafka", "kafka-clients")
      .exclude("org.apache.kafka", "kafka_2.12")
      .exclude("org.eclipse.jetty", "jetty-webapp"),
    "org.eclipse.jetty" % "jetty-webapp" % "9.4.3.v20170317" % "test",
    ("org.apache.storm" % "storm-core" % "1.1.0")
      .exclude("log4j", "log4j")
      .exclude("org.slf4j", "slf4j-log4j12")
      .exclude("org.slf4j", "log4j-over-slf4j")
      .exclude("org.slf4j", "slf4j-api")
      .exclude("org.apache.logging.log4j", "log4j-slf4j-impl"),
    "org.apache.storm" % "storm-kafka" % "1.1.0" % "test"
  ))

  lazy val sjRestDependencies = Def.setting(Seq(
    "com.typesafe.akka" %% "akka-http" % "10.0.3",
    ("org.apache.httpcomponents" % "httpclient" % "4.5.2")
      .exclude("commons-logging", "commons-logging"),
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.16"
  ))

  lazy val sjOutputEngineDependencies = Def.setting(Seq(
    "org.eclipse.jetty" % "jetty-server" % "9.4.3.v20170317" % "test"
  ))

  lazy val sjInputEngineDependencies = Def.setting(Seq(
    "com.hazelcast" % "hazelcast" % "3.7.3"
  ))

  lazy val sjFrameworkDependencies = Def.setting(Seq(
    "org.apache.mesos" % "mesos" % "0.28.1",
    "org.eclipse.jetty" % "jetty-runner" % "9.4.3.v20170317",
    ("org.apache.httpcomponents" % "httpclient" % "4.5.2")
      .exclude("commons-logging", "commons-logging")
  ))

  lazy val sjTestDependencies = Def.setting(Seq(
    "org.scalatest" % "scalatest_2.12" % "3.0.1" % "test",
    "org.mockito" % "mockito-core" % "2.8.9" % "test"
  ))

  lazy val sjEngineSimulatorsDependencies = Def.setting(Seq(
    "org.mockito" % "mockito-core" % "2.8.9"
  ))

  lazy val flinkDependencies = Def.setting(Seq(
    "org.apache.flink" % "flink-streaming-scala_2.11" % "1.3.1",
    "org.apache.flink" % "flink-connector-kafka-0.10_2.11" % "1.3.1"
  ))
}
