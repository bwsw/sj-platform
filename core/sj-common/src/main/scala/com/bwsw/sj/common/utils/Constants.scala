/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.utils

import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import org.eclipse.jetty.http.{HttpScheme, HttpVersion}

object EngineLiterals {

  final val httpPrefix = "http://"
  final val queueSize = 1000
  final val batchInstanceBarrierPrefix = "/instance/barriers/"
  final val batchInstanceLeaderPrefix = "/instance/leaders/"
  final val eventWaitTimeout = 1000
  final val producerTransactionBatchSize = 20

  final val inputStreamingType = "input-streaming"
  final val outputStreamingType = "output-streaming"
  final val batchStreamingType = "batch-streaming"
  final val regularStreamingType = "regular-streaming"
  val moduleTypes: Seq[String] = Seq(inputStreamingType, regularStreamingType, batchStreamingType, outputStreamingType)

  val beToFeModulesTypes = Map(
    inputStreamingType -> "Input streaming",
    regularStreamingType -> "Regular streaming",
    batchStreamingType -> "Batch streaming",
    outputStreamingType -> "Output streaming"
  )

  final val everyNthMode = "every-nth"
  final val timeIntervalMode = "time-interval"
  final val transactionIntervalMode = "transaction-interval"
  val checkpointModes: Seq[String] = Seq(everyNthMode, timeIntervalMode)
  val batchFillTypes: Seq[String] = Seq(everyNthMode, timeIntervalMode, transactionIntervalMode)

  final val noneStateMode = "none"
  final val ramStateMode = "ram"
  final val rocksStateMode = "rocks"
  val stateManagementModes: Seq[String] = Seq(noneStateMode, ramStateMode, rocksStateMode)

  final val oldestStartMode = "oldest"
  final val newestStartMode = "newest"
  val startFromModes: Seq[String] = Seq(oldestStartMode, newestStartMode)

  final val noneDefaultEvictionPolicy = "NONE"
  final val lruDefaultEvictionPolicy = "LRU"
  final val lfuDefaultEvictionPolicy = "LFU"
  val defaultEvictionPolicies: Seq[String] = Seq(lruDefaultEvictionPolicy, lfuDefaultEvictionPolicy, noneDefaultEvictionPolicy)

  final val fixTimeEvictionPolicy = "fix-time"
  final val expandedTimeEvictionPolicy = "expanded-time"
  val evictionPolicies: Seq[String] = Seq(fixTimeEvictionPolicy, expandedTimeEvictionPolicy)

  final val ready = "ready"
  final val starting = "starting"
  final val started = "started"
  final val stopping = "stopping"
  final val stopped = "stopped"
  final val deleting = "deleting"
  final val deleted = "deleted"
  final val failed = "failed"
  final val error = "error"
  val instanceStatusModes: Seq[String] = Seq(starting,
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

  final val splitStreamMode = "split"
  final val fullStreamMode = "full"
  val streamModes: Array[String] = Array(splitStreamMode, fullStreamMode)

  final val outputProcessorExecutionContextSize = 32
}

object StreamLiterals {
  final val inputDummy = "input"
  final val tstreamsType = "stream.t-streams"
  final val kafkaType = "stream.apache-kafka"
  final val elasticsearchType = "stream.elasticsearch"
  final val jdbcType = "stream.sql-database"
  final val restType = "stream.restful"
  val types: Seq[String] = Seq(tstreamsType, kafkaType, jdbcType, elasticsearchType, restType)

  val beToFeTypes = Map(
    tstreamsType -> "T-streams",
    kafkaType -> "Apache Kafka",
    jdbcType -> "SQL database",
    elasticsearchType -> "Elasticsearch",
    restType -> "RESTful"
  )

  val outputTypes: Seq[String] = Seq(jdbcType, elasticsearchType, restType)
  val internalTypes: Seq[String] = Seq(tstreamsType, kafkaType)

  private val tstreamFactory = new TStreamsFactory()
  final val ttl: Int = tstreamFactory.getProperty(ConfigurationOptions.Stream.ttlSec).asInstanceOf[Int]

  val typeToServiceType: Map[String, String] = Map(
    elasticsearchType -> ServiceLiterals.elasticsearchType,
    kafkaType -> ServiceLiterals.kafkaType,
    jdbcType -> ServiceLiterals.jdbcType,
    restType -> ServiceLiterals.restType,
    tstreamsType -> ServiceLiterals.tstreamsType
  )
}

object ServiceLiterals {
  final val elasticsearchType = "service.elasticsearch"
  final val kafkaType = "service.apache-kafka"
  final val tstreamsType = "service.t-streams"
  final val zookeeperType = "service.apache-zookeeper"
  final val jdbcType = "service.sql-database"
  final val restType = "service.restful"

  val types: Seq[String] = Seq(
    elasticsearchType,
    kafkaType,
    tstreamsType,
    zookeeperType,
    jdbcType,
    restType
  )

  val beToFeTypes = Map(
    elasticsearchType -> "Elasticsearch",
    kafkaType -> "Apache Kafka",
    tstreamsType -> "T-streams",
    zookeeperType -> "Apache Zookeeper",
    jdbcType -> "SQL database",
    restType -> "RESTful"
  )

  val typeToProviderType: Map[String, String] = Map(
    elasticsearchType -> ProviderLiterals.elasticsearchType,
    kafkaType -> ProviderLiterals.kafkaType,
    zookeeperType -> ProviderLiterals.zookeeperType,
    jdbcType -> ProviderLiterals.jdbcType,
    restType -> ProviderLiterals.restType,
    tstreamsType -> ProviderLiterals.zookeeperType
  )
}

object ProviderLiterals {
  final val zookeeperType = "provider.apache-zookeeper"
  final val kafkaType = "provider.apache-kafka"
  final val elasticsearchType = "provider.elasticsearch"
  final val jdbcType = "provider.sql-database"
  final val restType = "provider.restful"

  val withAuth = Seq(elasticsearchType)

  val types: Seq[String] = Seq(
    zookeeperType,
    kafkaType,
    elasticsearchType,
    jdbcType,
    restType
  )

  val beToFeTypes = Map(
    zookeeperType -> "Apache Zookeeper",
    kafkaType -> "Apache Kafka",
    elasticsearchType -> "Elasticsearch",
    jdbcType -> "SQL database",
    restType -> "RESTful"
  )

  val connectTimeoutMillis = 10000
}

object RestLiterals {
  final val http_1_0 = "1.0"
  final val http_1_1 = "1.1"
  final val http_2 = "2"

  final val httpVersions: Seq[String] = Seq(http_1_0, http_1_1, http_2)

  final val httpVersionFromString: Map[String, HttpVersion] = Map(
    http_1_0 -> HttpVersion.HTTP_1_0,
    http_1_1 -> HttpVersion.HTTP_1_1,
    http_2 -> HttpVersion.HTTP_2
  )

  final val httpVersionToString: Map[HttpVersion, String] = Map(
    HttpVersion.HTTP_1_0 -> http_1_0,
    HttpVersion.HTTP_1_1 -> http_1_1,
    HttpVersion.HTTP_2 -> http_2
  )

  final val http: String = HttpScheme.HTTP.toString
  final val https: String = HttpScheme.HTTPS.toString

  final val httpSchemes: Seq[String] = Seq(http, https)

  final val httpSchemeFromString: Map[String, HttpScheme] = Map(
    http -> HttpScheme.HTTP,
    https -> HttpScheme.HTTPS
  )

  final val defaultDescription = "No description"
  final val defaultRestAddress = ""
}

object JdbcLiterals {
  final val postgresqlDriverPrefix = "jdbc:postgresql"
  final val oracleDriverPrefix = "jdbc:oracle:thin"
  final val mysqlDriverPrefix = "jdbc:mysql"
  final val validPrefixes: List[String] = List(postgresqlDriverPrefix, oracleDriverPrefix, mysqlDriverPrefix)
}

object FrameworkLiterals {
  val instanceIdLabel = "INSTANCE_ID"
  val frameworkIdLabel = "FRAMEWORK_ID"
  val mesosMasterLabel = "MESOS_MASTER"
  val zookeeperHostLabel = "ZOOKEEPER_HOST"
  val zookeeperPortLabel = "ZOOKEEPER_PORT"

  val framework = "mesos-framework"
  val common = "sj-common"
  val frameworkId = framework + ".id"
  val instance = framework + ".instance"
  val instanceId = instance + ".id"
  val mesosMaster = framework + ".mesos.master"
  val zookeeperHost = common + ".zookeeper.host"
  val zookeeperPort = common + ".zookeeper.port"

  val defaultBackoffSeconds = 7
  val defaultBackoffFactor = 7.0
  val defaultMaxLaunchDelaySeconds = 600

  def createCommandToLaunch(frameworkJarName: String): String = {
    "java -jar " + frameworkJarName + " $PORT"
  }

  def createZookepeerAddress(zookeeperServer: String): String = {
    s"zk://$zookeeperServer/"
  }

  val initialStageDuration = 0
}

/**
  * Names of configurations for application config
  */
object CommonAppConfigNames {
  val sjCommon = "sj-common"

  val mongo = sjCommon + ".mongo"
  val mongoHosts = mongo + ".hosts"
  val mongoUser = mongo + ".user"
  val mongoPassword = mongo + ".password"
  val mongoDbName = mongo + ".database-name"

  val zookeeper = sjCommon + ".zookeeper"
  val zooKeeperHost = zookeeper + ".host"
  val zooKeeperPort = zookeeper + ".port"
}


object BenchmarkConfigNames {
  val performanceBenchmarkConfig = "sj-benchmark.performance"

  val messageConfig = performanceBenchmarkConfig + ".message"
  val messageSizesConfig = messageConfig + ".sizes"
  val messagesCountsConfig = messageConfig + ".counts"

  val kafkaAddressConfig = performanceBenchmarkConfig + ".kafka.address"
  val mongoPortConfig = performanceBenchmarkConfig + ".mongo.port"
  val outputFileConfig = performanceBenchmarkConfig + ".output-file"
  val wordsConfig = performanceBenchmarkConfig + ".words"
  val zooKeeperAddressConfig = performanceBenchmarkConfig + ".zookeeper.address"

  val repetitionsConfig = performanceBenchmarkConfig + ".repetitions"

  val batchConfig = performanceBenchmarkConfig + ".batch"
  val batchSizesConfig = batchConfig + ".sizes"
  val windowSizesConfig = batchConfig + ".window.sizes"
  val slidingIntervalsConfig = batchConfig + ".sliding.intervals"

  val tStreamConfig = performanceBenchmarkConfig + ".tstreams"
  val tStreamPrefixConfig = tStreamConfig + ".prefix"
  val tStreamTokenConfig = tStreamConfig + ".token"
  val tStreamSizePerTransaction = tStreamConfig + ".transactions.sizes"
}

object BenchmarkLiterals {

  object Regular {
    val sjDefaultOutputFile = "sj-regular-benchmark-output"
    val sjTStreamsDefaultOutputFile = "sj-t-streams-regular-benchmark-output"
    val samzaDefaultOutputFile = "samza-regular-benchmark-output"
    val flinkDefaultOutputFile = "flink-regular-benchmark-output"
    val stormDefaultOutputFile = "storm-regular-benchmark-output"
  }

  object Batch {
    val sjDefaultOutputFile = "sj-batch-benchmark-output"
    val flinkDefaultOutputFile = "flink-batch-benchmark-output"
    val stormDefaultOutputFile = "storm-batch-benchmark-output"
  }

}
