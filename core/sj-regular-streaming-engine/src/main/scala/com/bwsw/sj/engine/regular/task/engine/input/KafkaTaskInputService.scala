package com.bwsw.sj.engine.regular.task.engine.input

import java.util.Properties

import com.bwsw.common.{JsonSerializer, ObjectSerializer}
import com.bwsw.sj.common.ConfigConstants._
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.DAL.model.{KafkaService, SjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.StreamConstants
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.core.engine.input.TaskInputService
import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope}
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.tstreams.agents.consumer.Offsets.Newest
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewTransactionProducerPolicy
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
 * Class is responsible for launching kafka consumers
 * that put consumed message, which are wrapped in envelope, into a common queue
 * and handling producers to save offsets for further recovering after fails
 * Created: 27/04/2016
 *
 * @author Kseniya Mikhaleva
 *
 * @param manager Manager of environment of task of regular module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 * @param checkpointGroup Group of t-stream agents that have to make a checkpoint at the same time
 */
class KafkaTaskInputService(manager: RegularTaskManager,
                                   blockingQueue: PersistentBlockingQueue,
                                   checkpointGroup: CheckpointGroup)
  extends TaskInputService {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"regular-task-${manager.taskName}-kafka-consumer")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val offsetSerializer = new ObjectSerializer()
  private val regularInstance = manager.getInstance.asInstanceOf[RegularInstance]
  private val configService = ConnectionRepository.getConfigService
  private val kafkaSubscriberTimeout = configService.get(kafkaSubscriberTimeoutTag).value.toInt
  private val kafkaInputs = getKafkaInputs()
  private var kafkaOffsetsStorage = mutable.Map[(String, Int), Long]()
  private val kafkaOffsetsStream = manager.taskName + "_kafka_offsets"
  private val offsetStream = createOffsetStream()

  private val offsetProducer = createOffsetProducer()
  addOffsetProducerToCheckpointGroup()

  val kafkaConsumer: KafkaConsumer[Array[Byte], Array[Byte]] = createSubscribingKafkaConsumer(
    kafkaInputs.map(x => (x._1.name, x._2.toList)).toList,
    kafkaInputs.flatMap(_._1.service.asInstanceOf[KafkaService].provider.hosts).toList,
    chooseOffset()
  )
  
  private def getKafkaInputs(): mutable.Map[SjStream, Array[Int]] = {
    manager.inputs.filter(x => x._1.streamType == StreamConstants.kafkaStreamType)
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
  private def createSubscribingKafkaConsumer(topics: List[(String, List[Int])], hosts: List[String], offset: String): KafkaConsumer[Array[Byte], Array[Byte]] = {
    logger.debug(s"Task name: ${manager.taskName}. Create kafka consumer for topics (with their partitions): " +
      s"${topics.map(x => s"topic name: ${x._1}, " + s"partitions: ${x._2.mkString(",")}").mkString(",")}\n")
    val consumer: KafkaConsumer[Array[Byte], Array[Byte]] = createKafkaConsumer(hosts, offset)

    assignKafkaConsumerOnTopics(consumer, topics)

    seekKafkaConsumerOffsets(consumer)

    consumer
  }

  private def createKafkaConsumer(hosts: List[String], offset: String) = {
    val props = new Properties()
    props.put("bootstrap.servers", hosts.mkString(","))
    props.put("enable.auto.commit", "false")
    props.put("auto.offset.reset", offset)
    props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    configService.getByParameters(Map("domain" -> "kafka")).foreach(x => props.put(x.name.replace(x.domain + ".", ""), x.value))

    val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)

    consumer
  }

  private def assignKafkaConsumerOnTopics(consumer: KafkaConsumer[Array[Byte], Array[Byte]], topics: List[(String, List[Int])]) = {
    val topicPartitions = topics.flatMap(x => {
      (x._2.head to x._2.tail.head).map(y => new TopicPartition(x._1, y))
    }).asJava

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

    val lastTxn = offsetConsumer.getLastTransaction(partition)

    if (lastTxn.isDefined) {
      logger.debug(s"Task name: ${manager.taskName}. Get saved offsets for kafka consumer and apply their\n")
      kafkaOffsetsStorage = offsetSerializer.deserialize(lastTxn.get.next()).asInstanceOf[mutable.Map[(String, Int), Long]]
      kafkaOffsetsStorage.foreach(x => consumer.seek(new TopicPartition(x._1._1, x._1._2), x._2 + 1))
    }

    offsetConsumer.stop()
  }

  private def chooseOffset() = {
    regularInstance.startFrom match {
      case "oldest" => "earliest"
      case _ => "latest"
    }
  }

  override def call() = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run a kafka consumer for regular task in a separate thread of execution service\n")

    val envelopeSerializer = new JsonSerializer()
    val streamNameToTags = kafkaInputs.map(x => (x._1.name, x._1.tags)).toMap

    while (true) {
      logger.debug(s"Task: ${manager.taskName}. Waiting for records that consumed from kafka for $kafkaSubscriberTimeout milliseconds\n")
      val records = kafkaConsumer.poll(kafkaSubscriberTimeout)
      records.asScala.foreach(x => {

        blockingQueue.put(envelopeSerializer.serialize({
          val envelope = new KafkaEnvelope()
          envelope.stream = x.topic()
          envelope.partition = x.partition()
          envelope.data = x.value()
          envelope.offset = x.offset()
          envelope.tags = streamNameToTags(x.topic())
          envelope
        }))
      })
    }
  }

  def registerEnvelope(envelope: Envelope, performanceMetrics: PerformanceMetrics) = {
    logger.info(s"Task: ${manager.taskName}. Kafka envelope is received\n")
    val kafkaEnvelope = envelope.asInstanceOf[KafkaEnvelope]
    logger.debug(s"Task: ${manager.taskName}. Change offset for stream: ${kafkaEnvelope.stream} " +
      s"for partition: ${kafkaEnvelope.partition} to ${kafkaEnvelope.offset}\n")
    kafkaOffsetsStorage((kafkaEnvelope.stream, kafkaEnvelope.partition)) = kafkaEnvelope.offset
    performanceMetrics.addEnvelopeToInputStream(
      kafkaEnvelope.stream,
      List(kafkaEnvelope.data.length)
    )
  }

  override def doCheckpoint() = {
    logger.debug(s"Task: ${manager.taskName}. Save kafka offsets for each kafka input\n")
    offsetProducer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
      .send(offsetSerializer.serialize(kafkaOffsetsStorage))
  }
}