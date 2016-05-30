import com.typesafe.sbt.SbtNativePackager.Docker
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.Keys._
import sbt._

object StreamJugglerBuild extends Build {

  addCommandAlias("rebuild", ";clean; compile; package")

  //////////////////////////////////////////////////////////////////////////////
  // PROJECTS
  //////////////////////////////////////////////////////////////////////////////
  lazy val sj = Project(id = "sj",
    base = file("."),
    settings = commonSettings) aggregate(common,
    stub, crudRest, transactionGenerator, mesos,
    outputStreamingEngine, regularStreamingEngine, windowedStreamingEngine)


  lazy val common = Project(id = "sj-common",
    base = file("sj-common")).enablePlugins(JavaAppPackaging)

  lazy val stub = Project(id = "sj-stub-module",
    base = file("sj-stub-module")).enablePlugins(JavaAppPackaging).dependsOn(common)

  lazy val crudRest = Project(id = "sj-crud-rest",
    base = file("sj-crud-rest")).enablePlugins(JavaAppPackaging).dependsOn(common)

  lazy val transactionGenerator = Project(id = "sj-transaction-generator",
    base = file("sj-transaction-generator")).enablePlugins(JavaAppPackaging)

  lazy val mesos = Project(id = "sj-mesos-framework",
    base = file("sj-mesos-framework")).enablePlugins(JavaAppPackaging).dependsOn(common)

  lazy val outputStreamingEngine = Project(id = "sj-output-streaming-engine",
    base = file("sj-output-streaming-engine")).enablePlugins(JavaAppPackaging).dependsOn(common)

  lazy val regularStreamingEngine = Project(id = "sj-regular-streaming-engine",
    base = file("sj-regular-streaming-engine")).enablePlugins(JavaAppPackaging).dependsOn(common)

  lazy val windowedStreamingEngine = Project(id = "sj-windowed-streaming-engine",
    base = file("sj-windowed-streaming-engine")).enablePlugins(JavaAppPackaging).dependsOn(common)

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

//    libraryDependencies ++= Seq(
//      "com.typesafe"     % "config"          % TYPESAFE_CONFIG_VERSION,
//      "org.scalatest"   %% "scalatest"       % SCALATEST_VERSION % "test"
//    ),
//
//    dependencyOverrides ++= Set(
//      "com.typesafe.akka" %% "akka-actor" % "2.4.1",
//      "org.apache.kafka" % "kafka-clients" % "0.8.2.2",
//      "org.apache.kafka" % "kafka_2.11" % "0.8.2.2",
//      "com.fasterxml.jackson.module" % "jackson-module-scala_2.11" % "2.6.3"
//    ),
//
//    dependencyOverrides ++= Set(
//      "org.scala-lang" %  "scala-library"  % scalaVersion.value,
//      "org.scala-lang" %  "scala-reflect"  % scalaVersion.value,
//      "org.scala-lang" %  "scala-compiler" % scalaVersion.value
//    ),
//
//    assemblyMergeStrategy in assembly := {
//      case x =>
//        val oldStrategy = (assemblyMergeStrategy in assembly).value
//        oldStrategy(x)
//    },


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
