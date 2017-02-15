package com.bwsw.sj.engine.windowed.task.input

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.input.KafkaTaskInput
import com.bwsw.sj.engine.core.entities.KafkaEnvelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewTransactionProducerPolicy

import scala.collection.JavaConverters._

/**
 * Class is responsible for launching kafka consumers
 * that allow to fetching messages, which are wrapped in envelope
 * and handling producers to save offsets for further recovering after fails
 *
 *
 * @author Kseniya Mikhaleva
 */
class RetrievableKafkaTaskInput[T](override val manager: CommonTaskManager,
                                override val checkpointGroup: CheckpointGroup = new CheckpointGroup())
  extends RetrievableTaskInput[KafkaEnvelope[T]](manager.inputs) with KafkaTaskInput[T] {
  currentThread.setName(s"windowed-task-${manager.taskName}-kafka-consumer")

  override def chooseOffset() = {
    val instance = manager.instance.asInstanceOf[WindowedInstance]
    instance.startFrom match {
      case EngineLiterals.oldestStartMode => "earliest"
      case _ => "latest"
    }
  }

  override def get() = {
    logger.debug(s"Task: ${manager.taskName}. Waiting for records that consumed from kafka for $kafkaSubscriberTimeout milliseconds.")
    val records = kafkaConsumer.poll(kafkaSubscriberTimeout)
    records.asScala.map(consumerRecordToEnvelope).map(_.asInstanceOf[KafkaEnvelope[T]]) //todo deserialize
  }

  override def setConsumerOffsetToLastEnvelope() = {
    super.setConsumerOffsetToLastEnvelope()
    offsetProducer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
      .send(offsetSerializer.serialize(kafkaOffsetsStorage))
  }
}