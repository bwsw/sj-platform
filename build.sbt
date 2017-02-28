

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

  assemblyMergeStrategy in assembly := {
    case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.first
    case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.concat
    case "log4j.properties" => MergeStrategy.concat
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
    inputStreamingEngine, regularStreamingEngine, windowedStreamingEngine, outputStreamingEngine,
    framework, transactionGenerator,
    stubInput, stubRegular, stubWindowed, stubESOutput, stubJDBCOutput,
    pmOutput,
    sflowProcess, sflowOutput,
    sumWindowed
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
    libraryDependencies ++= Dependencies.sjRestDependencies.value
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
  .settings(
    libraryDependencies ++= Dependencies.sjWindowedEngineDependencies.value
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

lazy val stubWindowed = Project(id = "sj-stub-windowed-streaming",
  base = file("./contrib/stubs/sj-stub-windowed-streaming"))
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

lazy val pmOutput = Project(id = "sj-performance-metrics-output-es",
  base = file("./contrib/sj-platform/sj-performance-metrics-output-es"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val sflowProcess = Project(id = "sj-sflow-process",
  base = file("./contrib/examples/sflow/sj-sflow-process"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val sflowOutput = Project(id = "sj-sflow-output",
  base = file("./contrib/examples/sflow/sj-sflow-output"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val sumWindowed = Project(id = "sj-windowed-sum",
  base = file("./contrib/examples/sj-windowed-sum"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val sflowDemoInput = Project(id = "sflow-input",
  base = file("./contrib/examples/sj-sflow-demo/sflow-input"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val sflowDemoProcess = Project(id = "sflow-process",
  base = file("./contrib/examples/sj-sflow-demo/sflow-process"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)

lazy val sflowDemoOutput = Project(id = "sflow-output",
  base = file("./contrib/examples/sj-sflow-demo/sflow-output"))
  .settings(commonSettings: _*)
  .dependsOn(engineCore)