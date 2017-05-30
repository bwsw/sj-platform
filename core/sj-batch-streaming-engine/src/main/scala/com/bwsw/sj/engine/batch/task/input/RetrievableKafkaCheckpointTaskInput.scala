package com.bwsw.sj.engine.batch.task.input

import com.bwsw.sj.common.engine.EnvelopeDataSerializer
import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.input.KafkaTaskInput
import com.bwsw.sj.engine.core.entities.KafkaEnvelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewProducerTransactionPolicy
import org.apache.kafka.clients.consumer.ConsumerRecord
import scaldi.Injector

import scala.collection.JavaConverters._

/**
  * Class is responsible for launching kafka consumers
  * that allow to fetch messages, which are wrapped in envelopes
  * and handle producers to save offsets for further recovering after fails
  *
  * @param manager         allows to manage an environment of batch streaming task
  * @param checkpointGroup group of t-stream agents that have to make a checkpoint at the same time
  * @author Kseniya Mikhaleva
  */
class RetrievableKafkaCheckpointTaskInput[T <: AnyRef](override val manager: CommonTaskManager,
                                                       override val checkpointGroup: CheckpointGroup)
                                                      (override implicit val injector: Injector)
  extends RetrievableCheckpointTaskInput[KafkaEnvelope[T]](manager.inputs) with KafkaTaskInput[T] {
  currentThread.setName(s"batch-task-${manager.taskName}-kafka-consumer")

  private val envelopeDataSerializer = manager.envelopeDataSerializer.asInstanceOf[EnvelopeDataSerializer[T]]

  override def chooseOffset(): String = {
    val instance = manager.instance.asInstanceOf[BatchInstance]
    instance.startFrom match {
      case EngineLiterals.oldestStartMode => "earliest"
      case _ => "latest"
    }
  }

  override def get(): Iterable[KafkaEnvelope[T]] = {
    logger.debug(s"Task: ${manager.taskName}. Waiting for records that consumed from kafka for $kafkaSubscriberTimeout milliseconds.")
    val records = kafkaConsumer.poll(kafkaSubscriberTimeout)
    records.asScala.map(consumerRecordToEnvelope)
  }

  private def consumerRecordToEnvelope(consumerRecord: ConsumerRecord[Array[Byte], Array[Byte]]): KafkaEnvelope[T] = {
    logger.debug(s"Task name: ${manager.taskName}. Convert a consumed kafka record to kafka envelope.")
    val data = envelopeDataSerializer.deserialize(consumerRecord.value())
    val envelope = new KafkaEnvelope(data)
    envelope.stream = consumerRecord.topic()
    envelope.partition = consumerRecord.partition()
    envelope.tags = streamNamesToTags(consumerRecord.topic())
    envelope.id = consumerRecord.offset()

    envelope
  }

  override def setConsumerOffsetToLastEnvelope(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Send a transaction containing kafka consumer offsets for all streams.")
    super.setConsumerOffsetToLastEnvelope()
    offsetProducer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened)
      .send(offsetSerializer.serialize(kafkaOffsetsStorage))
  }
}