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
package com.bwsw.sj.engine.regular.benchmark.read_kafka.samza

import java.io.{File, FileWriter}
import java.util.UUID

import com.bwsw.sj.common.utils.benchmark.BenchmarkUtils.retrieveResultFromFile
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.core.testutils.benchmark.read_kafka.regular.KafkaReaderBenchmark

/**
  * Provides methods for testing the speed of reading data by [[http://samza.apache.org Apache Samza]] from Kafka.
  *
  * Topic deletion must be enabled on the Kafka server.
  *
  * @param zooKeeperAddress ZooKeeper server's address. Must point to the ZooKeeper server that used by the Kafka server.
  * @param kafkaAddress     Kafka server's address
  * @param words            list of words that are sent to the Kafka server
  * @author Pavel Tomskikh
  */
class SamzaBenchmark(zooKeeperAddress: String,
                     kafkaAddress: String,
                     words: Array[String])
  extends KafkaReaderBenchmark(zooKeeperAddress, kafkaAddress, words) {

  private val outputFilename = "benchmark-output-" + UUID.randomUUID().toString
  private val outputFile = new File(outputFilename)
  private val propertiesFilename = s"samza-benchmark-${UUID.randomUUID().toString}.properties"
  private val propertiesFile = new File(propertiesFilename)

  prepareProperties()

  /**
    * Closes opened connections, deletes temporary files
    */
  override def close(): Unit = {
    propertiesFile.delete()

    super.close()
  }


  override protected def firstMessage(messageSize: Long, messagesCount: Long): Option[String] =
    Some(s"$messagesCount,${outputFile.getAbsolutePath}")

  override protected def runProcess(messageSize: Long, messagesCount: Long): Process = {
    val arguments = Seq(
      "--config-factory=org.apache.samza.config.factories.PropertiesConfigFactory",
      s"--config-path=${propertiesFile.getAbsolutePath}")

    val jobRunner = new ClassRunner(classOf[JobRunner], arguments = arguments).start()
    println("JobRunner started")

    jobRunner
  }

  override protected def retrieveResult(messageSize: Long, messagesCount: Long): Option[Long] =
    retrieveResultFromFile(outputFile).map(_.toLong)


  private def prepareProperties(): Unit = {
    val properties = Seq(
      "job.factory.class=org.apache.samza.job.local.ThreadJobFactory",
      "job.name=samza-benchmark",
      "job.default.system=kafka",

      s"task.class=${classOf[SamzaBenchmarkStreamTask].getName}",
      s"task.inputs=kafka.$kafkaTopic",

      "serializers.registry.string.class=org.apache.samza.serializers.StringSerdeFactory",

      "systems.kafka.samza.factory=org.apache.samza.system.kafka.KafkaSystemFactory",
      s"systems.kafka.consumer.zookeeper.connect=$zooKeeperAddress/",
      s"systems.kafka.producer.bootstrap.servers=$kafkaAddress",
      "systems.kafka.samza.offset.default=oldest",
      "systems.kafka.default.stream.replication.factor=1",
      "systems.kafka.default.stream.samza.msg.serde=string",

      "job.coordinator.factory=org.apache.samza.zk.ZkJobCoordinatorFactory",
      s"job.coordinator.zk.connect=$zooKeeperAddress",
      "job.coordinator.replication.factor=1")

    val fileWriter = new FileWriter(propertiesFile)
    fileWriter.write(properties.mkString("\n"))
    fileWriter.close()
  }
}
