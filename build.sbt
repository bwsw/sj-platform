import sbt.url

name := "sj"
val sjVersion = "1.1.0"

addCommandAlias("rebuild", ";clean; compile; package")

artifact in(Compile, assembly) := {
  val art = (artifact in(Compile, assembly)).value
  art.copy(`classifier` = Some("assembly"))
}

addArtifact(artifact in(Compile, assembly), assembly)

organization in ThisBuild := "com.bwsw"
version in ThisBuild := sjVersion
scalaVersion in ThisBuild := Dependencies.Versions.scala
concurrentRestrictions in ThisBuild += Tags.limit(Tags.Test, 1)

val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    "-feature"
  ),

  resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/snapshots",
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/service/local/staging/deploy/maven2",
  resolvers += "Twitter Repository" at "http://maven.twttr.com",
  resolvers += "Oracle Maven2 Repo" at "http://download.oracle.com/maven",
  resolvers += "Elasticsearch Releases" at "https://artifacts.elastic.co/maven",

  assemblyMergeStrategy in assembly := {
    case PathList("org", "apache", "commons", "logging", xs@_*) => MergeStrategy.first
    case PathList("io", "netty", xs@_*) => MergeStrategy.first
    case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
    case PathList("com", "sun", "jna", xs@_*) => MergeStrategy.first
    case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.concat
    case "log4j.properties" => MergeStrategy.concat
    case "application.conf" => MergeStrategy.concat
    case "project.clj" => MergeStrategy.concat
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },

  assemblyJarName in assembly := s"${name.value}-${version.value}.jar",
  scalacOptions in(Compile, doc) ++= Seq("-groups", "-implicits"),

  fork in run := true,
  fork in Test := true,

  parallelExecution in Test := false
)

val publishSettings = Seq(
  pomIncludeRepository := { _ => false },

  licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0")),

  homepage := Some(url("http://stream-juggler.com/")),

  scmInfo := Some(
    ScmInfo(
      url("https://github.com/bwsw/sj-platform.git"),
      "scm:git@github.com:bwsw/sj-platform.git"
    )
  ),

  developers := List(
    Developer(
      "bitworks",
      "Bitworks Software, Ltd.",
      "bitworks@bw-sw.com",
      url = url("http://bitworks.software/")
    )
  ),

  publishMavenStyle := true,

  publishTo := version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT")) Some(
      "snapshots" at nexus + "content/repositories/snapshots"
    )
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }.value,

  publishArtifact in Test := false
)

val disabledPublishSettings = Seq(
  publish := {},
  publishTo := Some(Resolver.file("Unused transient repository", file("target/unusedrepo"))),
  publishArtifact := false
)

lazy val sj = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .settings(disabledPublishSettings: _*)
  .settings(updateOptions :=
    updateOptions.value.withCachedResolution(true))
  .aggregate(common,
    engineCore, crudRest,
    inputStreamingEngine, regularStreamingEngine, batchStreamingEngine, outputStreamingEngine,
    framework,
    engineSimulators,
    stubInput, stubRegular, stubBatch, stubESOutput, stubJDBCOutput, stubRestOutput,
    pmOutput, csvInput, regexInput,
    sumBatch
  )

lazy val common = Project(id = "sj-common",
  base = file("./core/sj-common"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjCommonDependencies.value,
    libraryDependencies ++= Dependencies.sjTestDependencies.value
  )

lazy val engineCore = Project(id = "sj-engine-core",
  base = file("./core/sj-engine-core"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    resolvers += "Clojars Repository" at "http://clojars.org/repo/",
    libraryDependencies ++= Dependencies.sjEngineCoreDependencies.value,
    libraryDependencies ++= Dependencies.sjTestDependencies.value
  )
  .dependsOn(common)

lazy val crudRest = Project(id = "sj-crud-rest",
  base = file("./core/sj-crud-rest"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjRestDependencies.value,
    libraryDependencies ++= Dependencies.sjTestDependencies.value
  )
  .dependsOn(common)

lazy val inputStreamingEngine = Project(id = "sj-input-streaming-engine",
  base = file("./core/sj-input-streaming-engine"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjInputEngineDependencies.value,
    libraryDependencies ++= Dependencies.sjTestDependencies.value,
    test in Test := (test in Test).dependsOn((Keys.`package` in Compile) in stubInput).value
  )
  .dependsOn(engineCore)

lazy val regularStreamingEngine = Project(id = "sj-regular-streaming-engine",
  base = file("./core/sj-regular-streaming-engine"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjTestDependencies.value,
    test in Test := (test in Test).dependsOn((Keys.`package` in Compile) in stubRegular).value
  )
  .dependsOn(engineCore)
  .dependsOn(crudRest % "test")

lazy val batchStreamingEngine = Project(id = "sj-batch-streaming-engine",
  base = file("./core/sj-batch-streaming-engine"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjTestDependencies.value,
    test in Test := (test in Test).dependsOn((Keys.`package` in Compile) in stubBatch).value
  )
  .dependsOn(engineCore)
  .dependsOn(crudRest % "test")

lazy val outputStreamingEngine = Project(id = "sj-output-streaming-engine",
  base = file("./core/sj-output-streaming-engine"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjOutputEngineDependencies.value,
    libraryDependencies ++= Dependencies.sjTestDependencies.value,
    test in Test := (test in Test)
      .dependsOn((Keys.`package` in Compile) in stubESOutput)
      .dependsOn((Keys.`package` in Compile) in stubJDBCOutput)
      .dependsOn((Keys.`package` in Compile) in stubRestOutput)
      .value
  )
  .dependsOn(engineCore)

lazy val framework = Project(id = "sj-mesos-framework",
  base = file("./core/sj-mesos-framework"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjFrameworkDependencies.value
  )
  .dependsOn(common)

lazy val engineSimulators = Project(id = "sj-engine-simulators",
  base = file("./core/sj-engine-simulators"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjEngineSimulatorsDependencies.value
  )
  .dependsOn(inputStreamingEngine)


lazy val stubInput = Project(id = "sj-stub-input-streaming",
  base = file("./contrib/stubs/sj-stub-input-streaming"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(engineCore % "provided")

lazy val stubRegular = Project(id = "sj-stub-regular-streaming",
  base = file("./contrib/stubs/sj-stub-regular-streaming"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(engineCore % "provided")

lazy val stubBatch = Project(id = "sj-stub-batch-streaming",
  base = file("./contrib/stubs/sj-stub-batch-streaming"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(engineCore % "provided")

lazy val stubESOutput = Project(id = "sj-stub-es-output-streaming",
  base = file("./contrib/stubs/sj-stub-es-output-streaming"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(engineCore % "provided")

lazy val stubJDBCOutput = Project(id = "sj-stub-jdbc-output-streaming",
  base = file("./contrib/stubs/sj-stub-jdbc-output-streaming"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(engineCore % "provided")

lazy val stubRestOutput = Project(id = "sj-stub-rest-output-streaming",
  base = file("./contrib/stubs/sj-stub-rest-output-streaming"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(engineCore % "provided")

lazy val pmOutput = Project(id = "sj-performance-metrics-output-es",
  base = file("./contrib/sj-platform/sj-performance-metrics-output-es"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(engineCore % "provided")

lazy val sumBatch = Project(id = "sj-batch-sum",
  base = file("./contrib/examples/sj-batch-sum"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .dependsOn(engineCore % "provided")

lazy val csvInput = Project(id = "sj-csv-input",
  base = file("./contrib/sj-platform/sj-csv-input"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjTestDependencies.value
  )
  .dependsOn(engineCore % "provided")
  .dependsOn(engineSimulators % "test")

lazy val regexInput = Project(id = "sj-regex-input",
  base = file("./contrib/sj-platform/sj-regex-input"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.sjTestDependencies.value
  )
  .dependsOn(engineCore % "provided")
  .dependsOn(engineSimulators % "test")

lazy val regularPerformanceBenchmark = Project(id = "sj-regular-performance-benchmark",
  base = file("./contrib/benchmarks/sj-regular-performance-benchmark"))
  .settings(commonSettings: _*)
  .settings(disabledPublishSettings: _*)
  .dependsOn(engineCore % "provided")

lazy val batchPerformanceBenchmark = Project(id = "sj-batch-performance-benchmark",
  base = file("./contrib/benchmarks/sj-batch-performance-benchmark"))
  .settings(commonSettings: _*)
  .settings(disabledPublishSettings: _*)
  .dependsOn(engineCore % "provided")

lazy val flinkBenchmarkTask = Project(id = "flink-benchmark-task",
  base = file("./contrib/benchmarks/flink-benchmark-task"))
  .settings(commonSettings: _*)
  .settings(disabledPublishSettings: _*)
  .settings(
    scalaVersion := "2.11.8",
    libraryDependencies ++= Dependencies.flinkDependencies.value
  )

lazy val flinkBatchBenchmarkTask = Project(id = "flink-batch-benchmark-task",
  base = file("./contrib/benchmarks/flink-batch-benchmark-task"))
  .settings(commonSettings: _*)
  .settings(disabledPublishSettings: _*)
  .settings(
    scalaVersion := "2.11.8",
    libraryDependencies ++= Dependencies.flinkDependencies.value
  )


lazy val testInput = Project(id = "sj-input-test",
  base = file("./tests/pipeline/sj-input-test"))
  .settings(commonSettings: _*)
  .settings(disabledPublishSettings: _*)
  .dependsOn(engineCore)

lazy val testRegular = Project(id = "sj-regular-test",
  base = file("./tests/pipeline/sj-regular-test"))
  .settings(commonSettings: _*)
  .settings(disabledPublishSettings: _*)
  .dependsOn(engineCore)

lazy val testBatch = Project(id = "sj-batch-test",
  base = file("./tests/pipeline/sj-batch-test"))
  .settings(commonSettings: _*)
  .settings(disabledPublishSettings: _*)
  .dependsOn(engineCore)

lazy val testOutputES = Project(id = "sj-output-es-test",
  base = file("./tests/pipeline/sj-output-es-test"))
  .settings(commonSettings: _*)
  .settings(disabledPublishSettings: _*)
  .dependsOn(engineCore)

lazy val testOutputJDBC = Project(id = "sj-output-jdbc-test",
  base = file("./tests/pipeline/sj-output-jdbc-test"))
  .settings(commonSettings: _*)
  .settings(disabledPublishSettings: _*)
  .dependsOn(engineCore)

lazy val testOutputRest = Project(id = "sj-output-rest-test",
  base = file("./tests/pipeline/sj-output-rest-test"))
  .settings(commonSettings: _*)
  .settings(disabledPublishSettings: _*)
  .dependsOn(engineCore)

lazy val pipelineTest = Project(id = "sj-pipeline-test",
  base = file("./tests/pipeline"))
  .settings(disabledPublishSettings: _*)
  .aggregate(testInput, testRegular, testBatch, testOutputES, testOutputJDBC, testOutputRest)
