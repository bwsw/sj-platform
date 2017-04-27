package com.bwsw.sj.engine.core.engine.input


import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.engine.EnvelopeDataSerializer
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewTransactionProducerPolicy
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.collection.JavaConverters._

/**
  * Class is responsible for launching kafka consumers
  * that put consumed message, which are wrapped in envelope, into a common queue
  * and handling producers to save offsets for further recovering after fails
  *
  * @author Kseniya Mikhaleva
  * @param manager       Manager of environment of task of regular module
  * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
  *                      which will be retrieved into a module
  */
class CallableKafkaCheckpointTaskInput[T <: AnyRef](override val manager: CommonTaskManager,
                                                    blockingQueue: ArrayBlockingQueue[Envelope],
                                                    override val checkpointGroup: CheckpointGroup = new CheckpointGroup())
  extends CallableCheckpointTaskInput[KafkaEnvelope[T]](manager.inputs) with KafkaTaskInput[T] {
  currentThread.setName(s"regular-task-${manager.taskName}-kafka-consumer")
  private val envelopeDataSerializer = manager.envelopeDataSerializer.asInstanceOf[EnvelopeDataSerializer[T]]

  def chooseOffset() = {
    logger.debug(s"Task: ${manager.taskName}. Get a start-from parameter from instance.")
    val instance = manager.instance.asInstanceOf[RegularInstance]
    instance.startFrom match {
      case EngineLiterals.oldestStartMode => "earliest"
      case _ => "latest"
    }
  }

  override def call() = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run a kafka consumer for regular task in a separate thread of execution service.")

    while (true) {
      logger.debug(s"Task: ${manager.taskName}. Waiting for records that consumed from kafka for $kafkaSubscriberTimeout milliseconds.")
      val records = kafkaConsumer.poll(kafkaSubscriberTimeout)
      records.asScala.foreach(x => blockingQueue.put(consumerRecordToEnvelope(x)))
    }
  }

  private def consumerRecordToEnvelope(consumerRecord: ConsumerRecord[Array[Byte], Array[Byte]]) = {
    logger.debug(s"Task name: ${manager.taskName}. Convert a consumed kafka record to kafka envelope.")
    val data = envelopeDataSerializer.deserialize(consumerRecord.value())
    val envelope = new KafkaEnvelope(data)
    envelope.stream = consumerRecord.topic()
    envelope.partition = consumerRecord.partition()
    envelope.tags = streamNamesToTags(consumerRecord.topic())
    envelope.id = consumerRecord.offset()

    envelope
  }

  override def setConsumerOffsetToLastEnvelope() = {
    logger.debug(s"Task: ${manager.taskName}. Set a consumer offset to last envelope for all streams.")
    super.setConsumerOffsetToLastEnvelope()
    offsetProducer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
      .send(offsetSerializer.serialize(kafkaOffsetsStorage))
  }

  override def close(): Unit = {
    kafkaConsumer.close()
  }
}