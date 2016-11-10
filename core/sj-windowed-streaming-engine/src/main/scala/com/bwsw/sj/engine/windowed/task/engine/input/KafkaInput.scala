package com.bwsw.sj.engine.windowed.task.engine.input

import java.util.Properties

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.DAL.model.{KafkaService, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ConfigurationSettingsUtils._
import com.bwsw.sj.common.utils.{ConfigLiterals, ConfigSettingsUtils, EngineLiterals, StreamLiterals}
import com.bwsw.sj.engine.core.entities.KafkaEnvelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.consumer.Offset.Newest
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewTransactionProducerPolicy
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Class is responsible for launching kafka consumers
 * that allow to fetching messages, which are wrapped in envelope
 * and handling producers to save offsets for further recovering after fails
 *
 *
 * @author Kseniya Mikhaleva
 */
class KafkaInput(manager: CommonTaskManager,
                 override val checkpointGroup: CheckpointGroup = new CheckpointGroup())
  extends Input[KafkaEnvelope](manager.inputs) {
  private val currentThread = Thread.currentThread()
  currentThread.setName(s"windowed-task-${manager.taskName}-kafka-consumer")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val offsetSerializer = new ObjectSerializer()
  private val instance = manager.instance.asInstanceOf[WindowedInstance]
  private val kafkaSubscriberTimeout = ConfigSettingsUtils.getKafkaSubscriberTimeout()
  private val kafkaInputs = getKafkaInputs()
  private var kafkaOffsetsStorage = mutable.Map[(String, Int), Long]()
  private val kafkaOffsetsStream = manager.taskName + "_kafka_offsets"
  private val offsetStream = createOffsetStream()

  private val offsetProducer = createOffsetProducer()
  addOffsetProducerToCheckpointGroup()

  private val kafkaConsumer: KafkaConsumer[Array[Byte], Array[Byte]] = createSubscribingKafkaConsumer(
    kafkaInputs.map(x => (x._1.name, x._2.toList)).toList,
    kafkaInputs.flatMap(_._1.service.asInstanceOf[KafkaService].provider.hosts).toList,
    chooseOffset()
  )

  private val streamNamesToTags = kafkaInputs.map(x => (x._1.name, x._1.tags)).toMap

  private def getKafkaInputs(): mutable.Map[SjStream, Array[Int]] = {
    manager.inputs.filter(x => x._1.streamType == StreamLiterals.kafkaStreamType)
  }

  /**
   * Creates SJStream is responsible for committing the offsets of last messages
   * that has successfully processed for each topic for each partition
   *
   * @return SJStream is responsible for committing the offsets of last messages
   *         that has successfully processed for each topic for each partition
   */
  private def createOffsetStream() = {
    logger.debug(s"Task name: ${manager.taskName}. " +
      s"Get stream for keeping kafka offsets\n")
    val description = "store kafka offsets of input streams"
    val tags = Array("offsets")
    val partitions = 1

    manager.createTStreamOnCluster(kafkaOffsetsStream, description, partitions)

    manager.getSjStream(kafkaOffsetsStream, description, tags, partitions)
  }


  private def createOffsetProducer() = {
    logger.debug(s"Task: ${manager.taskName}. Start creating a t-stream producer to record kafka offsets\n")
    val streamForOffsets = offsetStream
    val offsetProducer = manager.createProducer(streamForOffsets)
    logger.debug(s"Task: ${manager.taskName}. Creation of t-stream producer is finished\n")

    offsetProducer
  }

  private def addOffsetProducerToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding the t-stream producer to checkpoint group\n")
    checkpointGroup.add(offsetProducer)
    logger.debug(s"Task: ${manager.taskName}. The t-stream producer is added to checkpoint group\n")
  }

  /**
   * Creates a kafka consumer for all input streams of kafka type.
   * If there was a checkpoint with offsets of last consumed messages for each topic/partition
   * then consumer will fetch from this offsets otherwise in accordance with offset parameter
   *
   * @param topics Set of kafka topic names and range of partitions relatively
   * @param hosts Addresses of kafka brokers in host:port format
   * @param offset Default policy for kafka consumer (earliest/latest)
   * @return Kafka consumer subscribed to topics
   */
  private def createSubscribingKafkaConsumer(topics: List[(String, List[Int])], hosts: List[String], offset: String) = {
    logger.debug(s"Task name: ${manager.taskName}. Create kafka consumer for topics (with their partitions): " +
      s"${topics.map(x => s"topic name: ${x._1}, " + s"partitions: ${x._2.mkString(",")}").mkString(",")}\n")
    val consumer: KafkaConsumer[Array[Byte], Array[Byte]] = createKafkaConsumer(hosts, offset)

    assignKafkaConsumerOnTopics(consumer, topics)

    seekKafkaConsumerOffsets(consumer)

    consumer
  }

  private def chooseOffset() = {
    instance.startFrom match {
      case EngineLiterals.oldestStartMode => "earliest"
      case _ => "latest"
    }
  }

  private def createKafkaConsumer(hosts: List[String], offset: String) = {
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

  private def assignKafkaConsumerOnTopics(consumer: KafkaConsumer[Array[Byte], Array[Byte]], topics: List[(String, List[Int])]) = {
    val topicPartitions = topics
      .flatMap(x => (x._2.head to x._2.tail.head)
      .map(y => new TopicPartition(x._1, y))).asJava

    consumer.assign(topicPartitions)
  }

  private def seekKafkaConsumerOffsets(consumer: KafkaConsumer[Array[Byte], Array[Byte]]) = {
    val partition = 0
    val partitionsRange = List(partition, partition)

    val offsetConsumer = manager.createConsumer(
      offsetStream,
      partitionsRange,
      Newest
    )

    offsetConsumer.start()

    val maybeTxn = offsetConsumer.getLastTransaction(partition)

    if (maybeTxn.isDefined) {
      val lastTxn = maybeTxn.get
      logger.debug(s"Task name: ${manager.taskName}. Get saved offsets for kafka consumer and apply their\n")
      kafkaOffsetsStorage = offsetSerializer.deserialize(lastTxn.next()).asInstanceOf[mutable.Map[(String, Int), Long]]
      kafkaOffsetsStorage.foreach(x => consumer.seek(new TopicPartition(x._1._1, x._1._2), x._2 + 1))
    }

    offsetConsumer.stop()
  }

  private def applyConfigurationSettings(properties: Properties) = {
    val configService = ConnectionRepository.getConfigService

    val kafkaSettings = configService.getByParameters(Map("domain" -> ConfigLiterals.kafkaDomain))
    kafkaSettings.foreach(x => properties.put(clearConfigurationSettingName(x.domain, x.name), x.value))
  }

  override def get() = {
    logger.debug(s"Task: ${manager.taskName}. Waiting for records that consumed from kafka for $kafkaSubscriberTimeout milliseconds\n")
    val records = kafkaConsumer.poll(kafkaSubscriberTimeout)
    records.asScala.map(consumerRecordToEnvelope)
  }

  private def consumerRecordToEnvelope(consumerRecord: ConsumerRecord[Array[Byte], Array[Byte]]) = {
    val envelope = new KafkaEnvelope()
    envelope.stream = consumerRecord.topic()
    envelope.partition = consumerRecord.partition()
    envelope.data = consumerRecord.value()
    envelope.offset = consumerRecord.offset()
    envelope.tags = streamNamesToTags(consumerRecord.topic())

    envelope
  }

  override def setConsumerOffset(envelope: KafkaEnvelope) = {
    logger.debug(s"Task: ${manager.taskName}. Change offset for stream: ${envelope.stream} " +
      s"for partition: ${envelope.partition} to ${envelope.offset}\n")
    kafkaOffsetsStorage((envelope.stream, envelope.partition)) = envelope.offset
  }

  override def setConsumerOffsetToLastEnvelope() = {
    super.setConsumerOffsetToLastEnvelope()
    offsetProducer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
      .send(offsetSerializer.serialize(kafkaOffsetsStorage))
  }
}