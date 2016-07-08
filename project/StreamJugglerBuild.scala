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
    settings = commonSettings) aggregate(common, engineCore, crudRest, transactionGenerator, mesos,
    outputStreamingEngine, regularStreamingEngine, windowedStreamingEngine, inputStreamingEngine,
    pmOutput,
    stubRegular, stubOutput,
    sflowOutput, sflowProcess)

  lazy val common = Project(id = "sj-common",
    base = file("./core/sj-common")).enablePlugins(JavaAppPackaging)

  lazy val engineCore = Project(id = "sj-engine-core",
    base = file("./core/sj-engine-core")).enablePlugins(JavaAppPackaging).dependsOn(common)

  lazy val crudRest = Project(id = "sj-crud-rest",
    base = file("./core/sj-crud-rest")).enablePlugins(JavaAppPackaging).dependsOn(common)

  lazy val transactionGenerator = Project(id = "sj-transaction-generator",
    base = file("./core/sj-transaction-generator")).enablePlugins(JavaAppPackaging).dependsOn(common)

  lazy val mesos = Project(id = "sj-mesos-framework",
    base = file("./core/sj-mesos-framework")).enablePlugins(JavaAppPackaging).dependsOn(common)

  lazy val outputStreamingEngine = Project(id = "sj-output-streaming-engine",
    base = file("./core/sj-output-streaming-engine")).enablePlugins(JavaAppPackaging).dependsOn(engineCore)

  lazy val regularStreamingEngine = Project(id = "sj-regular-streaming-engine",
    base = file("./core/sj-regular-streaming-engine")).enablePlugins(JavaAppPackaging).dependsOn(engineCore)

  lazy val windowedStreamingEngine = Project(id = "sj-windowed-streaming-engine",
    base = file("./core/sj-windowed-streaming-engine")).enablePlugins(JavaAppPackaging).dependsOn(engineCore)

  lazy val inputStreamingEngine = Project(id = "sj-input-streaming-engine",
    base = file("./core/sj-input-streaming-engine")).enablePlugins(JavaAppPackaging).dependsOn(engineCore)

  lazy val stubRegular = Project(id = "sj-stub-regular-streaming",
    base = file("./contrib/stubs/sj-stub-regular-streaming")).enablePlugins(JavaAppPackaging).dependsOn(engineCore)

  lazy val stubOutput = Project(id = "sj-stub-output",
    base = file("./contrib/stubs/sj-stub-output")).enablePlugins(JavaAppPackaging).dependsOn(engineCore)

  lazy val sflowProcess = Project(id = "sj-sflow-process",
    base = file("./contrib/examples/sflow/sj-sflow-process")).enablePlugins(JavaAppPackaging).dependsOn(engineCore)

  lazy val pmOutput = Project(id = "sj-performance-metrics-output-es",
    base = file("./contrib/sj-platform/sj-performance-metrics-output-es")).enablePlugins(JavaAppPackaging).dependsOn(engineCore)

  lazy val sflowOutput = Project(id = "sj-sflow-output",
    base = file("./contrib/examples/sflow/sj-sflow-output")).enablePlugins(JavaAppPackaging).dependsOn(engineCore)


  //////////////////////////////////////////////////////////////////////////////
  // PROJECT INFO
  //////////////////////////////////////////////////////////////////////////////

  val ORGANIZATION = "com.bwsw"
  val PROJECT_NAME = "stream-juggler"
  val PROJECT_VERSION = "0.1-SNAPSHOT"
  val SCALA_VERSION = "2.11.7"


  //////////////////////////////////////////////////////////////////////////////
  // DEPENDENCY VERSIONS
  //////////////////////////////////////////////////////////////////////////////

  val TYPESAFE_CONFIG_VERSION = "1.3.0"
  val SCALATEST_VERSION = "2.2.4"
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
    assemblyMergeStrategy in assembly := {
      case PathList("scala", xs@_*) => MergeStrategy.first
      case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
      case "library.properties" => MergeStrategy.concat
      case "log4j.properties" => MergeStrategy.concat
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
