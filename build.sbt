

name := "sj"
scalaVersion := Dependencies.Versions.scala
val sjVersion = "1.0-SNAPSHOT"

addCommandAlias("rebuild", ";clean; compile; package")

val commonSettings = Seq(
  version := sjVersion,
  scalaVersion := Dependencies.Versions.scala,
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature"
  ),

  pomExtra := (
    <scm>
      <url>git@github.com:bwsw/sj-platform.git</url>
      <connection>scm:git@github.com:bwsw/sj-platform.git</connection>
    </scm>
      <developers>
        <developer>
          <id>bitworks</id>
          <name>Bitworks Software, Ltd.</name>
          <url>http://bitworks.software/</url>
        </developer>
      </developers>
    ),

  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  resolvers += "Twitter Repository" at "http://maven.twttr.com",
  resolvers += "Oracle Maven2 Repo" at "http://download.oracle.com/maven",

  assemblyMergeStrategy in assembly := {
    case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.first
    case PathList("io", "netty", xs@_*) => MergeStrategy.first
    case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
    case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.concat
    case "log4j.properties" => MergeStrategy.concat
    case "application.conf" => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },

  assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
  scalacOptions in(Compile, doc) ++= Seq("-groups", "-implicits"),

  fork in run := true,
  fork in Test := true,
  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("http://stream-juggler.com/")),
  pomIncludeRepository := { _ => false },
  scalacOptions += "-feature",
  scalacOptions += "-deprecation",
  parallelExecution in Test := false,
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

lazy val sj = (project in file(".")).settings(publish := {})
  .settings(unidocSettings: _*)
  .settings(updateOptions :=
    updateOptions.value.withCachedResolution(true))
  .aggregate(common,
    engineCore, crudRest,
    inputStreamingEngine, regularStreamingEngine, batchStreamingEngine, outputStreamingEngine,
    framework,
    stubInput, stubRegular, stubBatch, stubESOutput, stubJDBCOutput, stubRestOutput,
    pmOutput, csvInput, regexInput,
    sumBatch
  )

lazy val common = Project(id = "sj-common",
  base = file("./core/sj-common"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjCommonDependencies.value,
    libraryDependencies ++= Dependencies.sjTestDependencies.value
  )

lazy val engineCore = Project(id = "sj-engine-core",
  base = file("./core/sj-engine-core"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjEngineCoreDependencies.value,
    libraryDependencies ++= Dependencies.sjTestDependencies.value
  )
  .dependsOn(common)

lazy val crudRest = Project(id = "sj-crud-rest",
  base = file("./core/sj-crud-rest"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjRestDependencies.value,
    libraryDependencies ++= Dependencies.sjTestDependencies.value
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

lazy val batchStreamingEngine = Project(id = "sj-batch-streaming-engine",
  base = file("./core/sj-batch-streaming-engine"))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjBatchEngineDependencies.value
  )
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

lazy val stubInput = Project(id = "sj-stub-input-streaming",
  base = file("./contrib/stubs/sj-stub-input-streaming"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val stubRegular = Project(id = "sj-stub-regular-streaming",
  base = file("./contrib/stubs/sj-stub-regular-streaming"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val stubBatch = Project(id = "sj-stub-batch-streaming",
  base = file("./contrib/stubs/sj-stub-batch-streaming"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val stubESOutput = Project(id = "sj-stub-es-output-streaming",
  base = file("./contrib/stubs/sj-stub-es-output-streaming"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val stubJDBCOutput = Project(id = "sj-stub-jdbc-output-streaming",
  base = file("./contrib/stubs/sj-stub-jdbc-output-streaming"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val stubRestOutput = Project(id = "sj-stub-rest-output-streaming",
  base = file("./contrib/stubs/sj-stub-rest-output-streaming"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val pmOutput = Project(id = "sj-performance-metrics-output-es",
  base = file("./contrib/sj-platform/sj-performance-metrics-output-es"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val sumBatch = Project(id = "sj-batch-sum",
  base = file("./contrib/examples/sj-batch-sum"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val csvInput = Project(id = "sj-csv-input",
  base = file("./contrib/sj-platform/sj-csv-input"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val regexInput = Project(id = "sj-regex-input",
  base = file("./contrib/sj-platform/sj-regex-input"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)
