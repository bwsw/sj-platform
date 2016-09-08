package com.bwsw.sj.common.utils

import java.util.UUID

object EngineConstants {
  def persistentQueuePath = UUID.randomUUID().toString //todo: yet t-streams can't remove persistent queue
  val persistentBlockingQueue = "persistentBlockingQueue"
  val eventWaitTimeout = 10

  val inputStreamingType = "input-streaming"
  val outputStreamingType = "output-streaming"
  val windowedStreamingType = "windowed-streaming"
  val regularStreamingType = "regular-streaming"

  val moduleTypes = Set(windowedStreamingType, regularStreamingType, outputStreamingType, inputStreamingType)
  val checkpointModes = Set("every-nth", "time-interval")
  val stateManagementModes = Set("none", "ram", "rocks")
  val startFromModes = Set("oldest", "newest")
  val defaultEvictionPolicies = Set("LRU", "LFU", "NONE")
  val evictionPolicies = Set("fix-time", "expanded-time")

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
  final val inputDummy = "input"
  final val tStreamType = "stream.t-stream"
  final val kafkaStreamType = "stream.kafka"
  final val esOutputType = "elasticsearch-output"
  final val jdbcOutputType = "jdbc-output"
  val streamTypes = Set(tStreamType, kafkaStreamType, esOutputType, jdbcOutputType)
}

object GeneratorConstants {
  val generatorTypes = Set("global", "local", "per-stream")
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

object ConfigConstants {
  val domains = Array("system", "t-streams", "kafka", "es", "zk", "jdbc")
  val transactionGeneratorTag = "system.current-transaction-generator"
  val frameworkTag = "system.current-framework"
  val hostOfCrudRestTag = "system.crud-rest-host"
  val portOfCrudRestTag = "system.crud-rest-port"
  val marathonTag = "system.marathon-connect"
  val marathonTimeoutTag = "system.marathon-connect-timeout"
  val zkSessionTimeoutTag= "zk.session-timeout"

  val jdbcTimeoutTag = "jdbc.timeout"
  val tgClientRetryPeriodTag = "system.transaction-generator-client-retry-period"
  val tgServerRetryPeriodTag = "system.transaction-generator-server-retry-period"
  val tgRetryCountTag = "system.transaction-generator-retry-count"
  val kafkaSubscriberTimeoutTag = "system.kafka-subscriber-timeout"
  val geoIpAsNum = "system.geo-ip-as-num"
  val geoIpAsNumv6 = "system.geo-ip-as-num-v6"
}
