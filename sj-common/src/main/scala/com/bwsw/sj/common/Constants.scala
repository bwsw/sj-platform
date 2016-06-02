package com.bwsw.sj.common

import java.util.UUID

/**
 * Object with constants
 */
object ModuleConstants {
  def persistentQueuePath = UUID.randomUUID().toString //todo: until t-streams can't remove persistent queue
  val persistentBlockingQueue = "persistentBlockingQueue"

  val outputStreamingType = "output-streaming"
  val windowedStreamingType = "windowed-streaming"
  val regularStreamingType = "regular-streaming"

  val moduleTypes = Set(windowedStreamingType, regularStreamingType, outputStreamingType)
  val checkpointModes = Set("every-nth", "time-interval")
  val stateManagementModes = Set("none", "ram", "rocks")
  val oldest = "oldest"
  val newest = "newest"
  val startFromModes = Set(oldest, newest)

  val ready = "ready"
  val starting = "starting"
  val started = "started"
  val stopping = "stopping"
  val stopped = "stopped"
  val deleting = "deleting"
  val deleted = "deleted"
  val failed = "failed"
  val instanceStatusModes = Set(starting,
    started,
    stopping,
    stopped,
    ready,
    deleting,
    deleted,
    failed
  )

  val toHandle = "to-handle"
  val generatorStatusModes = Set(starting, started, failed, toHandle)
  val streamModes = Array("split", "full")
}

object StreamConstants {
  final val tStream = "stream.t-stream"
  final val kafka = "stream.kafka"
  final val esOutput = "elasticsearch-output"
  final val jdbcOutput = "jdbc-output"
  val streamTypes = Set(tStream, kafka, esOutput, jdbcOutput)
}

object GeneratorConstants {
  val generatorTypes = Set("global", "local", "per-stream")
  val generatorTypesWithService = Set("global", "per-stream")
}

object ServiceConstants {
  val serviceTypes = Set("CassDB", "ESInd", "KfkQ", "TstrQ", "ZKCoord", "RdsCoord", "ArspkDB", "JDBC")
  val serviceTypesWithProvider = Set("CassDB", "ESInd", "KfkQ", "ZKCoord", "RdsCoord", "ArspkDB", "JDBC")
  val serviceTypeProviders = Map(
    "CassDB" -> "cassandra",
    "ESInd" -> "ES",
    "KfkQ" -> "kafka",
    "ZKCoord" -> "zookeeper",
    "RdsCoord" -> "redis",
    "ArspkDB" -> "aerospike",
    "JDBC" -> "JDBC"
  )
}

object ProviderConstants {
  val providerTypes = Set("cassandra", "aerospike", "zookeeper", "kafka", "ES", "redis", "JDBC")
}

object JarConstants {
  val transactionGeneratorJar = "sj-transaction-generator-assembly-1.0.jar"
  val frameworkJar = "ScalaMesos-assembly-1.0.jar"
  val taskRunnerJar = ""
}
