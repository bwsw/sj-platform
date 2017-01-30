package com.bwsw.sj.common.utils

import java.util.UUID

import com.bwsw.tstreams.env.{TSF_Dictionary, TStreamsFactory}

object EngineLiterals {
  val persistentQueuePath = UUID.randomUUID().toString

  final val queueSize = 1000
  final val persistentBlockingQueue = "persistentBlockingQueue"
  final val windowedInstanceBarrierPrefix = "/instance/barriers/"
  final val windowedInstanceLeaderPrefix = "/instance/leaders/"
  final val eventWaitTimeout = 1000

  final val inputStreamingType = "input-streaming"
  final val outputStreamingType = "output-streaming"
  final val windowedStreamingType = "windowed-streaming"
  final val regularStreamingType = "regular-streaming"
  val moduleTypes = Seq(windowedStreamingType, regularStreamingType, outputStreamingType, inputStreamingType)

  final val everyNthMode = "every-nth"
  final val timeIntervalMode = "time-interval"
  final val transactionIntervalMode = "transaction-interval"
  val checkpointModes = Seq(everyNthMode, timeIntervalMode)
  val batchFillTypes = Seq(everyNthMode, timeIntervalMode, transactionIntervalMode)

  final val noneStateMode = "none"
  final val ramStateMode = "ram"
  final val rocksStateMode = "rocks"
  val stateManagementModes = Seq(noneStateMode, ramStateMode, rocksStateMode)

  final val oldestStartMode = "oldest"
  final val newestStartMode = "newest"
  val startFromModes = Seq(oldestStartMode, newestStartMode)

  final val noneDefaultEvictionPolicy = "NONE"
  final val lruDefaultEvictionPolicy = "LRU"
  final val lfuDefaultEvictionPolicy = "LFU"
  val defaultEvictionPolicies = Seq(lruDefaultEvictionPolicy, lfuDefaultEvictionPolicy, noneDefaultEvictionPolicy)

  final val fixTimeEvictionPolicy = "fix-time"
  final val expandedTimeEvictionPolicy = "expanded-time"
  val evictionPolicies = Seq(fixTimeEvictionPolicy, expandedTimeEvictionPolicy)

  final val ready = "ready"
  final val starting = "starting"
  final val started = "started"
  final val stopping = "stopping"
  final val stopped = "stopped"
  final val deleting = "deleting"
  final val deleted = "deleted"
  final val failed = "failed"
  final val error = "error"
  val instanceStatusModes = Seq(starting,
    started,
    stopping,
    stopped,
    ready,
    deleting,
    deleted,
    failed,
    error
  )

  val toHandle = "to-handle"
  val generatorStatusModes = Seq(starting, started, failed, toHandle)

  final val splitStreamMode = "split"
  final val fullStreamMode = "full"
  val streamModes = Array(splitStreamMode, fullStreamMode)
}

object StreamLiterals {
  final val inputDummy = "input"
  final val tstreamType = "stream.t-stream"
  final val kafkaStreamType = "stream.kafka"
  final val esOutputType = "elasticsearch-output"
  final val jdbcOutputType = "jdbc-output"
  val types = Seq(tstreamType, kafkaStreamType, jdbcOutputType, esOutputType)

  private val tstreamFactory = new TStreamsFactory()
  final val ttl = tstreamFactory.getProperty(TSF_Dictionary.Stream.TTL).asInstanceOf[Int]
}

object GeneratorLiterals {
  final val localType = "local"
  final val globalType = "global"
  final val perStreamType = "per-stream"
  val types = Seq(globalType, localType, perStreamType)

  val scale: Int = 10000
  val masterDirectory = "/master"
  val globalDirectory = "/global"
  val messageForServer = "get"
}

object ServiceLiterals {
  final val cassandraType = "CassDB"
  final val elasticsearchType = "ESInd"
  final val kafkaType = "KfkQ"
  final val tstreamsType = "TstrQ"
  final val zookeeperType = "ZKCoord"
  final val aerospikeType = "ArspkDB"
  final val jdbcType = "JDBC"

  val types = Seq(
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
  val types = Seq(cassandraType, aerospikeType, zookeeperType, kafkaType, elasticsearchType, jdbcType)
}

object JdbcLiterals {
  final val postgresqlDriverName = "postgresql"
  final val oracleDriverName = "oracle"
  final val mysqlDriverName = "mysql"
  final val validDrivers = List(postgresqlDriverName, oracleDriverName, mysqlDriverName)

  final val postgresqlDriver = "org.postgresql.Driver"
  final val oracleDriver = "oracle.jdbc.driver.OracleDriver"
  final val mysqlDriver = "com.mysql.jdbc.Driver"

  final val postgresqlDriverPrefix = "jdbc:postgresql"
  final val oracleDriverPrefix = "jdbc:oracle:thin"
  final val mysqlDriverPrefix = "jdbc:mysql"
}

object FrameworkLiterals {
  val instanceIdLabel = "INSTANCE_ID"
  val frameworkIdLabel = "FRAMEWORK_ID"
  val mesosMasterLabel = "MESOS_MASTER"
}