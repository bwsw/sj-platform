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

import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.core.testutils.benchmark.loader.kafka.KafkaBenchmarkDataLoaderConfig
import com.bwsw.sj.engine.core.testutils.benchmark.regular.RegularBenchmark
import com.bwsw.sj.engine.regular.benchmark.read_kafka.samza.SamzaBenchmarkLiterals._
import org.apache.samza.job.JobRunner

/**
  * Provides methods for testing the speed of reading data by [[http://samza.apache.org Apache Samza]] from Kafka.
  *
  * Topic deletion must be enabled on the Kafka server.
  *
  * @param senderConfig configuration of Kafka topic
  * @author Pavel Tomskikh
  */
class SamzaBenchmark(senderConfig: KafkaBenchmarkDataLoaderConfig) extends RegularBenchmark {

  private val propertiesFilename = s"samza-benchmark-${UUID.randomUUID().toString}.properties"
  private val propertiesFile = new File(propertiesFilename)

  /**
    * Closes opened connections, deletes temporary files
    */
  override def stop(): Unit = {
    if (propertiesFile.exists())
      propertiesFile.delete()

    super.stop()
  }


  override protected def runProcess(messagesCount: Long): Process = {
    prepareProperties(messagesCount)
    val arguments = Seq(
      "--config-factory=org.apache.samza.config.factories.PropertiesConfigFactory",
      s"--config-path=${propertiesFile.getAbsolutePath}")

    new ClassRunner(classOf[JobRunner], arguments = arguments).start()
  }


  private def prepareProperties(messagesCount: Long): Unit = {
    if (propertiesFile.exists())
      propertiesFile.delete()

    val properties = Seq(
      "job.factory.class=org.apache.samza.job.local.ThreadJobFactory",
      "job.name=samza-benchmark",
      "job.default.system=kafka",

      s"task.class=${classOf[SamzaBenchmarkStreamTask].getName}",
      s"task.inputs=kafka.${senderConfig.topic}",

      "serializers.registry.string.class=org.apache.samza.serializers.StringSerdeFactory",

      "systems.kafka.samza.factory=org.apache.samza.system.kafka.KafkaSystemFactory",
      s"systems.kafka.consumer.zookeeper.connect=${senderConfig.zooKeeperAddress}/",
      s"systems.kafka.producer.bootstrap.servers=${senderConfig.kafkaAddress}",
      "systems.kafka.samza.offset.default=oldest",
      "systems.kafka.default.stream.replication.factor=1",
      "systems.kafka.default.stream.samza.msg.serde=string",

      "job.coordinator.factory=org.apache.samza.zk.ZkJobCoordinatorFactory",
      s"job.coordinator.zk.connect=${senderConfig.zooKeeperAddress}",
      "job.coordinator.replication.factor=1",
      s"$messagesCountConfig=$messagesCount",
      s"$outputFileConfig=${outputFile.getAbsolutePath}")

    val fileWriter = new FileWriter(propertiesFile)
    fileWriter.write(properties.mkString("\n"))
    fileWriter.close()
  }
}
