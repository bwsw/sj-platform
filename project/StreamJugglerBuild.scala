import com.typesafe.sbt.SbtNativePackager.Docker
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin.autoImport._
import sbtassembly.PathList

object StreamJugglerBuild extends Build {

  addCommandAlias("rebuild", ";clean; compile; package")

  //////////////////////////////////////////////////////////////////////////////
  // PROJECTS
  //////////////////////////////////////////////////////////////////////////////
  lazy val sj = Project(id = "sj",
    base = file("."),
    settings = commonSettings) aggregate(common,
    stub)


  lazy val common = Project(id = "sj-common",
    base = file("sj-common")).enablePlugins(JavaAppPackaging)

  lazy val stub = Project(id = "sj-stub-module",
    base = file("sj-stub-module")).enablePlugins(JavaAppPackaging).dependsOn(common)

  //////////////////////////////////////////////////////////////////////////////
  // PROJECT INFO
  //////////////////////////////////////////////////////////////////////////////

  val ORGANIZATION    = "com.bwsw"
  val PROJECT_NAME    = "stream-juggler"
  val PROJECT_VERSION = "0.1-SNAPSHOT"
  val SCALA_VERSION   = "2.11.7"


  //////////////////////////////////////////////////////////////////////////////
  // DEPENDENCY VERSIONS
  //////////////////////////////////////////////////////////////////////////////

  val TYPESAFE_CONFIG_VERSION = "1.3.0"
  val SCALATEST_VERSION       = "2.2.4"
  val SLF4J_VERSION           = "1.7.13"
  val SPARK_VERSION = "1.6.1"


  //////////////////////////////////////////////////////////////////////////////
  // SHARED SETTINGS
  //////////////////////////////////////////////////////////////////////////////

  lazy val commonSettings = Project.defaultSettings ++
    basicSettings



  lazy val basicSettings = Seq(
    version := PROJECT_VERSION,
    organization := ORGANIZATION,
    scalaVersion := SCALA_VERSION,

    libraryDependencies ++= Seq(
      "com.typesafe"     % "config"          % TYPESAFE_CONFIG_VERSION,
      "org.slf4j"        % "slf4j-log4j12"   % SLF4J_VERSION,
      "org.scalatest"   %% "scalatest"       % SCALATEST_VERSION % "test"
    ),

    dependencyOverrides ++= Set("com.typesafe.akka" %% "akka-actor" % "2.4.1",
      "org.apache.kafka" % "kafka-clients" % "0.8.2.2",
      "org.apache.kafka" % "kafka_2.11" % "0.8.2.2",
      "org.apache.spark" % "spark-core_2.11" % SPARK_VERSION,
      "org.apache.spark" % "spark-streaming_2.11" % SPARK_VERSION,
      "org.apache.spark" % "spark-streaming-kafka_2.11" % SPARK_VERSION,
      "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.6.3"),

    dependencyOverrides ++= Set(
      "org.scala-lang" %  "scala-library"  % scalaVersion.value,
      "org.scala-lang" %  "scala-reflect"  % scalaVersion.value,
      "org.scala-lang" %  "scala-compiler" % scalaVersion.value
    ),

    assemblyMergeStrategy in assembly := {
      case PathList("com", "google", "common", xs @ _*) => MergeStrategy.first
      case PathList("org", "apache", "commons", xs @ _*) => MergeStrategy.first
      case PathList("org", "apache", "spark", xs @ _*) => MergeStrategy.first
      case PathList("org", "apache", "log4j", xs @ _*) => MergeStrategy.first
      case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.first
      case PathList("org", "apache", "hadoop", xs @ _*) => MergeStrategy.first
      case PathList("io", "netty", xs @ _*) => MergeStrategy.first
      case PathList("io", "dropwizard", xs @ _*) => MergeStrategy.first
      case PathList("com", "codahale", xs @ _*) => MergeStrategy.first
      case PathList("javax", xs @ _*)         => MergeStrategy.first
      case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
      case "application.conf"                            => MergeStrategy.concat
      case "log4j.properties"                            => MergeStrategy.concat
      case "unwanted.txt"                                => MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },


    scalacOptions in Compile ++= Seq(
      "-unchecked",
      "-deprecation",
      "-feature"
    ),

    fork in run := true,

    fork in Test := true,

    parallelExecution in Test := false,

    version in Docker := version.value,

    maintainer in Docker := "bwsw <bwsw@bwsw.com>"

  )
}