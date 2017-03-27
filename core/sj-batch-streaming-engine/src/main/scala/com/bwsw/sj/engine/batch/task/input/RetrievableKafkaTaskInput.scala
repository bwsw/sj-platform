package com.bwsw.sj.engine.batch.task.input

import com.bwsw.sj.common.DAL.model.module.BatchInstance
import com.bwsw.sj.common.engine.EnvelopeDataSerializer
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.input.KafkaTaskInput
import com.bwsw.sj.engine.core.entities.KafkaEnvelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewTransactionProducerPolicy
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.collection.JavaConverters._

/**
  * Class is responsible for launching kafka consumers
  * that allow to fetching messages, which are wrapped in envelope
  * and handling producers to save offsets for further recovering after fails
  *
  * @author Kseniya Mikhaleva
  */
class RetrievableKafkaTaskInput[T <: AnyRef](override val manager: CommonTaskManager,
                                            override val checkpointGroup: CheckpointGroup = new CheckpointGroup())
  extends RetrievableTaskInput[KafkaEnvelope[T]](manager.inputs) with KafkaTaskInput[T] {
  currentThread.setName(s"batch-task-${manager.taskName}-kafka-consumer")

  private val envelopeDataSerializer = manager.envelopeDataSerializer.asInstanceOf[EnvelopeDataSerializer[T]]

  override def chooseOffset() = {
    val instance = manager.instance.asInstanceOf[BatchInstance]
    instance.startFrom match {
      case EngineLiterals.oldestStartMode => "earliest"
      case _ => "latest"
    }
  }

  override def get() = {
    logger.debug(s"Task: ${manager.taskName}. Waiting for records that consumed from kafka for $kafkaSubscriberTimeout milliseconds.")
    val records = kafkaConsumer.poll(kafkaSubscriberTimeout)
    records.asScala.map(consumerRecordToEnvelope)
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
    super.setConsumerOffsetToLastEnvelope()
    offsetProducer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
      .send(offsetSerializer.serialize(kafkaOffsetsStorage))
  }
}