name := "sj"

addCommandAlias("rebuild", ";clean; compile; package")

val commonSettings = Seq(
  version := "1.0",
  scalaVersion := Dependencies.Versions.scala,
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature"
  ),
  resolvers +=
    "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",

  resolvers += "Twitter Repository" at "http://maven.twttr.com",

  libraryDependencies ++= Seq(
    ("com.bwsw" % "t-streams_2.11" % "1.0-SNAPSHOT")
      .exclude("org.slf4j", "slf4j-simple"),
    "org.slf4j" % "slf4j-log4j12" % "1.7.21"
  ),

  assemblyMergeStrategy in assembly := {
    case PathList("scala", xs@_*) => MergeStrategy.first
    case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
    case PathList("io", "netty", xs@_*) => MergeStrategy.first
    case PathList("org", "joda", xs@_*) => MergeStrategy.first
    case "library.properties" => MergeStrategy.concat
    case "log4j.properties" => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },

  assemblyJarName in assembly := s"${name.value}-${version.value}.jar",

  fork in run := true,
  fork in Test := true,
  parallelExecution in Test := false,
  version := "1.0-SNAPSHOT",
  organization := "com.bwsw",
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false
)

lazy val sj = (project in file(".")) settings (publish := {}) aggregate(common,
  engineCore, crudRest,
  inputStreamingEngine, regularStreamingEngine, windowedStreamingEngine, outputStreamingEngine,
  framework, transactionGenerator,
  stubInput, stubRegular, stubOutput,
  pmOutput,
  sflowProcess, sflowOutput
  )

lazy val common = Project(id = "sj-common",
  base = file("./core/sj-common"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjCommonDependencies.value
  )

lazy val engineCore = Project(id = "sj-engine-core",
  base = file("./core/sj-engine-core"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjEngineCoreDependencies.value
  )
  .dependsOn(common)

lazy val crudRest = Project(id = "sj-crud-rest",
  base = file("./core/sj-crud-rest"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjRestDependencies.value,
    assemblyJarName in assembly := s"${name.value}-1.0.jar"
  )
  .dependsOn(common)

lazy val inputStreamingEngine = Project(id = "sj-input-streaming-engine",
  base = file("./core/sj-input-streaming-engine"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjInputEngineDependencies.value
  )
  .dependsOn(engineCore)

lazy val regularStreamingEngine = Project(id = "sj-regular-streaming-engine",
  base = file("./core/sj-regular-streaming-engine"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjRegularEngineDependencies.value
  )
  .dependsOn(engineCore)

lazy val windowedStreamingEngine = Project(id = "sj-windowed-streaming-engine",
  base = file("./core/sj-windowed-streaming-engine"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val outputStreamingEngine = Project(id = "sj-output-streaming-engine",
  base = file("./core/sj-output-streaming-engine"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjOutputEngineDependencies.value
  )
  .dependsOn(engineCore)

lazy val framework = Project(id = "sj-mesos-framework",
  base = file("./core/sj-mesos-framework"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjFrameworkDependencies.value
  )
  .dependsOn(common)

lazy val transactionGenerator = Project(id = "sj-transaction-generator",
  base = file("./core/sj-transaction-generator"))
  .settings(commonSettings: _*)
  .dependsOn(common)

lazy val stubInput = Project(id = "sj-stub-input-streaming",
  base = file("./contrib/stubs/sj-stub-input-streaming"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val stubRegular = Project(id = "sj-stub-regular-streaming",
  base = file("./contrib/stubs/sj-stub-regular-streaming"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val stubOutput = Project(id = "sj-stub-output",
  base = file("./contrib/stubs/sj-stub-output"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val pmOutput = Project(id = "sj-performance-metrics-output-es",
  base = file("./contrib/sj-platform/sj-performance-metrics-output-es"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val sflowProcess = Project(id = "sj-sflow-process",
  base = file("./contrib/examples/sflow/sj-sflow-process"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjSflowProcessDependencies.value
  )
  .dependsOn(engineCore)

lazy val sflowOutput = Project(id = "sj-sflow-output",
  base = file("./contrib/examples/sflow/sj-sflow-output"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)
