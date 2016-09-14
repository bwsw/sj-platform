package com.bwsw.sj.common.utils

import java.util.UUID

import com.bwsw.tstreams.env.{TSF_Dictionary, TStreamsFactory}

object EngineLiterals {
  def persistentQueuePath = UUID.randomUUID().toString //todo: yet t-streams can't remove persistent queue
  final val persistentBlockingQueue = "persistentBlockingQueue"
  final val eventWaitTimeout = 1000

  final val inputStreamingType = "input-streaming"
  final val outputStreamingType = "output-streaming"
  final val windowedStreamingType = "windowed-streaming"
  final val regularStreamingType = "regular-streaming"
  val moduleTypes = Set(windowedStreamingType, regularStreamingType, outputStreamingType, inputStreamingType)

  final val everyNthCheckpointMode = "every-nth"
  final val timeIntervalCheckpointMode = "time-interval"
  val checkpointModes = Set(everyNthCheckpointMode, timeIntervalCheckpointMode)

  final val noneStateMode = "none"
  final val ramStateMode = "ram"
  final val rocksStateMode = "rocks"
  val stateManagementModes = Set(noneStateMode, ramStateMode, rocksStateMode)

  final val oldestStartMode = "oldest"
  final val newestStartMode = "newest"
  val startFromModes = Set(oldestStartMode, newestStartMode)

  final val noneDefaultEvictionPolicy = "NONE"
  final val lruDefaultEvictionPolicy = "LRU"
  final val lfuDefaultEvictionPolicy = "LFU"
  val defaultEvictionPolicies = Set(lruDefaultEvictionPolicy, lfuDefaultEvictionPolicy, noneDefaultEvictionPolicy)

  final val fixTimeEvictionPolicy = "fix-time"
  final val expandedTimeEvictionPolicy = "expanded-time"
  val evictionPolicies = Set(fixTimeEvictionPolicy, expandedTimeEvictionPolicy)

  final val ready = "ready"
  final val starting = "starting"
  final val started = "started"
  final val stopping = "stopping"
  final val stopped = "stopped"
  final val deleting = "deleting"
  final val deleted = "deleted"
  final val failed = "failed"
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

  final val splitStreamMode = "split"
  final val fullStreamMode = "full"
  val streamModes = Array(splitStreamMode, fullStreamMode)
}

object StreamLiterals {
  final val inputDummy = "input"
  final val tStreamType = "stream.t-stream"
  final val kafkaStreamType = "stream.kafka"
  final val esOutputType = "elasticsearch-output"
  final val jdbcOutputType = "jdbc-output"
  val types = Set(tStreamType, kafkaStreamType, esOutputType, jdbcOutputType)

  private val tstreamFactory = new TStreamsFactory()
  final val ttl = tstreamFactory.getProperty(TSF_Dictionary.Stream.TTL).asInstanceOf[Int]
}

object GeneratorLiterals {
  final val localType = "local"
  final val globalType = "global"
  final val perStreamType = "per-stream"
  val types = Set(globalType, localType, perStreamType)
}

object ServiceLiterals {
  final val cassandraType = "CassDB"
  final val elasticsearchType = "ESInd"
  final val kafkaType = "KfkQ"
  final val tstreamsType = "TstrQ"
  final val zookeeperType = "ZKCoord"
  final val aerospikeType = "ArspkDB"
  final val jdbcType = "JDBC"

  val types = Set(
    cassandraType,
    elasticsearchType,
    kafkaType,
    tstreamsType,
    zookeeperType,
    aerospikeType,
    jdbcType
  )
  val typeToProviderType = Map(
    cassandraType -> ProviderLiterals.cassandraType,
    elasticsearchType -> ProviderLiterals.elasticsearchType,
    kafkaType -> ProviderLiterals.kafkaType,
    zookeeperType -> ProviderLiterals.zookeeperType,
    aerospikeType -> ProviderLiterals.aerospikeType,
    jdbcType -> ProviderLiterals.jdbcType
  )
}

object ProviderLiterals {
  final val cassandraType = "cassandra"
  final val aerospikeType = "aerospike"
  final val zookeeperType = "zookeeper"
  final val kafkaType = "kafka"
  final val elasticsearchType = "ES"
  final val jdbcType = "JDBC"
  val providerTypes = Set(cassandraType,aerospikeType, zookeeperType, kafkaType, elasticsearchType, jdbcType)
}

object ConfigLiterals {
  final val systemDomain = "system"
  final val tstreamsDomain = "t-streams"
  final val kafkaDomain = "kafka"
  final val elasticsearchDomain = "es"
  final val zookeeperDomain = "zk"
  final val jdbcDomain = "jdbc"
  val domains = Array(systemDomain, tstreamsDomain, kafkaDomain, elasticsearchDomain, zookeeperDomain, jdbcDomain)
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
