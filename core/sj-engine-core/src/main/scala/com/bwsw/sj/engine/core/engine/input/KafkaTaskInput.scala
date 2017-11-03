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
package com.bwsw.sj.engine.core.engine.input

import java.util.Properties

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.SjInjector
import com.bwsw.sj.common.config.{ConfigLiterals, SettingsUtils}
import com.bwsw.sj.common.dal.model.stream.{StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.core.entities.KafkaEnvelope
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.Producer
import com.typesafe.scalalogging.Logger
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import scaldi.Injectable.inject

import scala.collection.JavaConverters._
import scala.collection.mutable

trait KafkaTaskInput[T <: AnyRef] extends SjInjector {
  protected val manager: CommonTaskManager
  protected val checkpointGroup: CheckpointGroup
  protected val maxPollRecords: Int = 500
  protected val currentThread: Thread = Thread.currentThread()
  protected val logger: Logger = Logger(this.getClass)
  protected val offsetSerializer = new ObjectSerializer()
  protected val settingsUtils: SettingsUtils = inject[SettingsUtils]
  protected val kafkaSubscriberTimeout: Int = settingsUtils.getKafkaSubscriberTimeout()
  protected val kafkaInputs: mutable.Map[StreamDomain, Array[Int]] = getKafkaInputs()
  val streamNamesToTags: Map[String, Array[String]] = kafkaInputs.map(x => (x._1.name, x._1.tags)).toMap
  protected var kafkaOffsetsStorage: mutable.Map[(String, Int), Long] = mutable.Map[(String, Int), Long]()
  protected val kafkaOffsetsStream: String = manager.taskName + "_kafka_offsets"
  protected val offsetStream: TStreamStreamDomain = createOffsetStream()

  protected val offsetProducer: Producer = createOffsetProducer()
  addOffsetProducerToCheckpointGroup()

  protected val kafkaConsumer: KafkaConsumerWrapper = createKafkaConsumer()

  protected def getKafkaInputs(): mutable.Map[StreamDomain, Array[Int]] = {
    manager.inputs.filter(x => x._1.streamType == StreamLiterals.kafkaType)
  }

  /**
    * Creates [[com.bwsw.sj.common.dal.model.stream.KafkaStreamDomain]] is responsible for committing the offsets of last messages
    * that has successfully processed for each topic for each partition
    */
  protected def createOffsetStream(): TStreamStreamDomain = {
    logger.debug(s"Task name: ${manager.taskName}. Get stream for keeping kafka offsets.")
    val description = "store kafka offsets of input streams"
    val tags = Array("offsets")
    val partitions = 1

    manager.createStorageStream(kafkaOffsetsStream, description, partitions)

    manager.getStream(kafkaOffsetsStream, description, tags, partitions)
  }


  protected def createOffsetProducer(): Producer = {
    logger.debug(s"Task: ${manager.taskName}. Start creating a t-stream producer to record kafka offsets.")
    val streamForOffsets = offsetStream
    val offsetProducer = manager.createProducer(streamForOffsets)
    logger.debug(s"Task: ${manager.taskName}. Creation of t-stream producer is finished.")

    offsetProducer
  }

  protected def addOffsetProducerToCheckpointGroup(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Start adding the t-stream producer to checkpoint group.")
    checkpointGroup.add(offsetProducer)
    logger.debug(s"Task: ${manager.taskName}. The t-stream producer is added to checkpoint group.")
  }

  /**
    * Creates a kafka consumer for all input streams of kafka type.
    * If there was a checkpoint with offsets of last consumed messages for each topic/partition
    * then consumer will fetch from this offsets otherwise in accordance with offset parameter
    *
    * @param topics set of kafka topic names and range of partitions relatively
    * @param hosts  addresses of kafka brokers in host:port format
    * @param offset default policy for kafka consumer (earliest/latest)
    */
  protected def createSubscribingKafkaConsumer(topics: List[(String, List[Int])],
                                               hosts: List[String],
                                               offset: String): KafkaConsumer[Array[Byte], Array[Byte]] = {
    logger.debug(s"Task name: ${manager.taskName}. Create kafka consumer for topics (with their partitions): " +
      s"${topics.map(x => s"topic name: ${x._1}, " + s"partitions: ${x._2.mkString(",")}").mkString(",")}.")
    val consumer = createKafkaConsumer(hosts, offset)

    assignKafkaConsumerOnTopics(consumer, topics)

    seekKafkaConsumerOffsets(consumer)

    consumer
  }

  protected def chooseOffset(): String

  protected def createKafkaConsumer(hosts: List[String], offset: String): KafkaConsumer[Array[Byte], Array[Byte]] = {
    logger.debug(s"Task: ${manager.taskName}. Create a kafka consumer.")
    val properties = new Properties()
    properties.put("bootstrap.servers", hosts.mkString(","))
    properties.put("enable.auto.commit", "false")
    properties.put("auto.offset.reset", offset)
    properties.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    properties.put("max.poll.records", Int.box(maxPollRecords))
    applyConfigurationSettings(properties)

    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](properties)

    consumer
  }

  protected def assignKafkaConsumerOnTopics(consumer: KafkaConsumer[Array[Byte], Array[Byte]], topics: List[(String, List[Int])]): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Assign a kafka consumer to particular topics.")
    val topicPartitions = topics
      .flatMap(x => (x._2.head to x._2.tail.head)
        .map(y => new TopicPartition(x._1, y))).asJava

    consumer.assign(topicPartitions)
  }

  protected def seekKafkaConsumerOffsets(consumer: KafkaConsumer[Array[Byte], Array[Byte]]): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Seek a kafka consumer offset.")
    val partition = 0
    val partitionsRange = List(partition, partition)

    val offsetConsumer = manager.createConsumer(
      offsetStream,
      partitionsRange,
      Oldest
    )

    offsetConsumer.start()

    val maybeTxn = offsetConsumer.getLastTransaction(partition)

    if (maybeTxn.isDefined) {
      val tempTransaction = maybeTxn.get
      logger.debug(s"Task name: ${manager.taskName}. Get saved offsets for kafka consumer and apply them.")
      val lastTxn = offsetConsumer.buildTransactionObject(tempTransaction.getPartition, tempTransaction.getTransactionID, tempTransaction.getState, tempTransaction.getCount).get //todo fix it next milestone TR1216
      kafkaOffsetsStorage = offsetSerializer.deserialize(lastTxn.next()).asInstanceOf[mutable.Map[(String, Int), Long]]
      val consumerTopics = consumer.assignment().asScala.map(_.topic())
      kafkaOffsetsStorage
        .filterKeys { case (topic, _) => consumerTopics.contains(topic) }
        .foreach(x => consumer.seek(new TopicPartition(x._1._1, x._1._2), x._2 + 1))
    }

    offsetConsumer.stop()
  }

  protected def applyConfigurationSettings(properties: Properties): Unit = {
    logger.debug(s"Task name: ${manager.taskName}. Get setting (using a config service) for kafka consumer and apply them.")
    val configService = inject[ConnectionRepository].getConfigRepository

    val kafkaSettings = configService.getByParameters(Map("domain" -> ConfigLiterals.kafkaDomain))
    kafkaSettings.foreach(x => properties.put(ConfigurationSetting.clearConfigurationSettingName(x.domain, x.name), x.value))
  }

  def setConsumerOffset(envelope: KafkaEnvelope[T]): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Change offset for stream: ${envelope.stream} " +
      s"for partition: ${envelope.partition} to ${envelope.id}.")
    kafkaOffsetsStorage((envelope.stream, envelope.partition)) = envelope.id
  }

  def close(): Unit = {
    kafkaConsumer.close()
  }

  protected def createKafkaConsumer(): KafkaConsumerWrapper
}
