package com.bwsw.sj.engine.core.engine.input

import java.util.Properties

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.model.{KafkaService, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.config.{ConfigLiterals, ConfigurationSettingsUtils}
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.entities.KafkaEnvelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable

trait KafkaTaskInput[T <: AnyRef] {
  protected val manager: CommonTaskManager
  protected val checkpointGroup: CheckpointGroup
  protected val currentThread = Thread.currentThread()
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val offsetSerializer = new ObjectSerializer()
  protected val kafkaSubscriberTimeout = ConfigurationSettingsUtils.getKafkaSubscriberTimeout()
  protected val kafkaInputs = getKafkaInputs()
  protected val streamNamesToTags = kafkaInputs.map(x => (x._1.name, x._1.tags)).toMap
  protected var kafkaOffsetsStorage = mutable.Map[(String, Int), Long]()
  protected val kafkaOffsetsStream = manager.taskName + "_kafka_offsets"
  protected val offsetStream = createOffsetStream()

  protected val offsetProducer = createOffsetProducer()
  addOffsetProducerToCheckpointGroup()

  protected val kafkaConsumer = createSubscribingKafkaConsumer(
    kafkaInputs.map(x => (x._1.name, x._2.toList)).toList,
    kafkaInputs.flatMap(_._1.service.asInstanceOf[KafkaService].provider.hosts).toList,
    chooseOffset()
  )

  protected def getKafkaInputs(): mutable.Map[SjStream, Array[Int]] = {
    manager.inputs.filter(x => x._1.streamType == StreamLiterals.kafkaStreamType)
  }

  /**
    * Creates SJStream is responsible for committing the offsets of last messages
    * that has successfully processed for each topic for each partition
    *
    * @return SJStream is responsible for committing the offsets of last messages
    *         that has successfully processed for each topic for each partition
    */
  protected def createOffsetStream() = {
    logger.debug(s"Task name: ${manager.taskName}. Get stream for keeping kafka offsets.")
    val description = "store kafka offsets of input streams"
    val tags = Array("offsets")
    val partitions = 1

    manager.createTStreamOnCluster(kafkaOffsetsStream, description, partitions)

    manager.getSjStream(kafkaOffsetsStream, description, tags, partitions)
  }


  protected def createOffsetProducer() = {
    logger.debug(s"Task: ${manager.taskName}. Start creating a t-stream producer to record kafka offsets.")
    val streamForOffsets = offsetStream
    val offsetProducer = manager.createProducer(streamForOffsets)
    logger.debug(s"Task: ${manager.taskName}. Creation of t-stream producer is finished.")

    offsetProducer
  }

  protected def addOffsetProducerToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding the t-stream producer to checkpoint group.")
    checkpointGroup.add(offsetProducer)
    logger.debug(s"Task: ${manager.taskName}. The t-stream producer is added to checkpoint group.")
  }

  /**
    * Creates a kafka consumer for all input streams of kafka type.
    * If there was a checkpoint with offsets of last consumed messages for each topic/partition
    * then consumer will fetch from this offsets otherwise in accordance with offset parameter
    *
    * @param topics Set of kafka topic names and range of partitions relatively
    * @param hosts  Addresses of kafka brokers in host:port format
    * @param offset Default policy for kafka consumer (earliest/latest)
    * @return Kafka consumer subscribed to topics
    */
  protected def createSubscribingKafkaConsumer(topics: List[(String, List[Int])], hosts: List[String], offset: String) = {
    logger.debug(s"Task name: ${manager.taskName}. Create kafka consumer for topics (with their partitions): " +
      s"${topics.map(x => s"topic name: ${x._1}, " + s"partitions: ${x._2.mkString(",")}").mkString(",")}.")
    val consumer = createKafkaConsumer(hosts, offset)

    assignKafkaConsumerOnTopics(consumer, topics)

    seekKafkaConsumerOffsets(consumer)

    consumer
  }

  protected def chooseOffset(): String

  protected def createKafkaConsumer(hosts: List[String], offset: String) = {
    logger.debug(s"Task: ${manager.taskName}. Create a kafka consumer.")
    val properties = new Properties()
    properties.put("bootstrap.servers", hosts.mkString(","))
    properties.put("enable.auto.commit", "false")
    properties.put("auto.offset.reset", offset)
    properties.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    properties.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    applyConfigurationSettings(properties)

    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](properties)

    consumer
  }

  protected def assignKafkaConsumerOnTopics(consumer: KafkaConsumer[Array[Byte], Array[Byte]], topics: List[(String, List[Int])]) = {
    logger.debug(s"Task: ${manager.taskName}. Assign a kafka consumer to particular topics.")
    val topicPartitions = topics
      .flatMap(x => (x._2.head to x._2.tail.head)
        .map(y => new TopicPartition(x._1, y))).asJava

    consumer.assign(topicPartitions)
  }

  protected def seekKafkaConsumerOffsets(consumer: KafkaConsumer[Array[Byte], Array[Byte]]) = {
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
      val lastTxn = offsetConsumer.buildTransactionObject(tempTransaction.getPartition(), tempTransaction.getTransactionID(), tempTransaction.getCount()).get //todo fix it next milestone TR1216
      kafkaOffsetsStorage = offsetSerializer.deserialize(lastTxn.next()).asInstanceOf[mutable.Map[(String, Int), Long]]
//      kafkaOffsetsStorage = offsetSerializer.deserialize(lastTxn.getAll().dequeue()).asInstanceOf[mutable.Map[(String, Int), Long]]
      kafkaOffsetsStorage.foreach(x => consumer.seek(new TopicPartition(x._1._1, x._1._2), x._2 + 1))
    }

    offsetConsumer.stop()
  }

  protected def applyConfigurationSettings(properties: Properties) = {
    logger.debug(s"Task name: ${manager.taskName}. Get setting (using a config service) for kafka consumer and apply them.")
    val configService = ConnectionRepository.getConfigService

    val kafkaSettings = configService.getByParameters(Map("domain" -> ConfigLiterals.kafkaDomain))
    kafkaSettings.foreach(x => properties.put(clearConfigurationSettingName(x.domain, x.name), x.value))
  }

  def setConsumerOffset(envelope: KafkaEnvelope[T]) = {
    logger.debug(s"Task: ${manager.taskName}. Change offset for stream: ${envelope.stream} " +
      s"for partition: ${envelope.partition} to ${envelope.id}.")
    kafkaOffsetsStorage((envelope.stream, envelope.partition)) = envelope.id
  }
}
