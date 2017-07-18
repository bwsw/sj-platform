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
package com.bwsw.sj.engine.regular.benchmark.samza

import java.io.{File, FileWriter}
import java.util.UUID

import com.bwsw.common.KafkaClient
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.kafka.data_sender.DataSender
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.samza.job.JobRunner

import scala.collection.JavaConverters._

/**
  * Provides methods for testing speed of reading data by [[http://samza.apache.org Apache Samza]] from Kafka.
  *
  * Topic deletion must be enabled on the Kafka server.
  *
  * @param zooKeeperAddress ZooKeeper server's address. Must point to the ZooKeeper server that used by the Kafka server.
  * @param kafkaAddress     Kafka server's address
  * @param messagesCount    count of messages
  * @param words            list of words that sends to the kafka server
  * @author Pavel Tomskikh
  */
class SamzaBenchmark(zooKeeperAddress: String,
                     kafkaAddress: String,
                     messagesCount: Long,
                     words: Array[String]) {

  private val kafkaTopic = "samza-benchmark-" + UUID.randomUUID().toString
  private val outputKafkaTopic = "samza-benchmark-result-" + UUID.randomUUID().toString
  private val propertiesFilename = s"samza-benchmark-${UUID.randomUUID().toString}.properties"
  private val propertiesFile = new File(propertiesFilename)
  private val kafkaClient = new KafkaClient(Array(zooKeeperAddress))
  private val kafkaSender = new DataSender(kafkaAddress, kafkaTopic, words, " ")
  private val lookupResultTimeout = 5000
  private val taskParameters = s"$messagesCount,$outputKafkaTopic"
  private val kafkaConsumer = createKafkaConsumer()

  prepareProperties()


  /**
    * Sends data into the Kafka server and runs Samza's job
    *
    * @param messageSize size of one message that sends to the Kafka server
    * @return time for which Samza reads messages from Kafka
    */
  def runTest(messageSize: Long): Long = {
    println(s"$messageSize bytes messages")

    kafkaClient.createTopic(kafkaTopic, 1, 1)
    while (!kafkaClient.topicExists(kafkaTopic))
      Thread.sleep(100)

    println(s"Kafka topic $kafkaTopic created")

    kafkaSender.send(messageSize, messagesCount, Some(taskParameters))
    println("Data sent to the Kafka")

    val arguments = Seq(
      "--config-factory=org.apache.samza.config.factories.PropertiesConfigFactory",
      s"--config-path=${propertiesFile.getAbsolutePath}")

    val jobRunner = new ClassRunner(classOf[JobRunner], arguments = arguments).start()
    println("JobRunner started")

    var consumerRecords = kafkaConsumer.poll(lookupResultTimeout)
    while (consumerRecords.isEmpty)
      consumerRecords = kafkaConsumer.poll(lookupResultTimeout)

    jobRunner.destroy()

    kafkaClient.deleteTopic(kafkaTopic)
    while (kafkaClient.topicExists(kafkaTopic))
      Thread.sleep(100)

    println(s"Kafka topic $kafkaTopic deleted")

    consumerRecords.records(outputKafkaTopic).iterator().next().value().toLong
  }

  /**
    * Closes opened connections, deletes temporary files
    */
  def close(): Unit = {
    kafkaConsumer.close()
    kafkaClient.close()
    propertiesFile.delete()
  }


  private def prepareProperties(): Unit = {
    val properties = Seq(
      "job.factory.class=org.apache.samza.job.local.ThreadJobFactory",
      "job.name=samza-benchmark",
      "job.default.system=kafka",

      s"task.class=${classOf[BenchmarkStreamTask].getName}",
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

  private def createKafkaConsumer(): KafkaConsumer[String, String] = {
    if (kafkaClient.topicExists(outputKafkaTopic))
      kafkaClient.deleteTopic(outputKafkaTopic)
    while (kafkaClient.topicExists(outputKafkaTopic))
      Thread.sleep(100)

    val configMap: Map[String, AnyRef] = Map[String, AnyRef](
      "bootstrap.servers" -> kafkaAddress,
      "group.id" -> "samza-benchmark")
    val consumer = new KafkaConsumer(configMap.asJava, new StringDeserializer, new StringDeserializer)
    consumer.subscribe(Seq(outputKafkaTopic).asJava)

    consumer
  }
}
