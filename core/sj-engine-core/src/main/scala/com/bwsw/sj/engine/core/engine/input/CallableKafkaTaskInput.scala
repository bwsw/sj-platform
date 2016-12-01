package com.bwsw.sj.engine.core.engine.input

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.DAL.model.module.RegularInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.engine.input.KafkaTaskInput
import com.bwsw.sj.engine.core.entities.KafkaEnvelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewTransactionProducerPolicy

import scala.collection.JavaConverters._

/**
 * Class is responsible for launching kafka consumers
 * that put consumed message, which are wrapped in envelope, into a common queue
 * and handling producers to save offsets for further recovering after fails
 *
 *
 * @author Kseniya Mikhaleva
 *
 * @param manager Manager of environment of task of regular module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module
 */
class CallableKafkaTaskInput(override val manager: CommonTaskManager,
                            blockingQueue: PersistentBlockingQueue,
                            override val checkpointGroup: CheckpointGroup = new CheckpointGroup())
  extends CallableTaskInput[KafkaEnvelope](manager.inputs) with KafkaTaskInput {
  currentThread.setName(s"regular-task-${manager.taskName}-kafka-consumer")

  def chooseOffset() = {
    val instance = manager.instance.asInstanceOf[RegularInstance]
    instance.startFrom match {
      case EngineLiterals.oldestStartMode => "earliest"
      case _ => "latest"
    }
  }

  override def call() = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run a kafka consumer for regular task in a separate thread of execution service\n")

    val envelopeSerializer = new JsonSerializer()

    while (true) {
      logger.debug(s"Task: ${manager.taskName}. Waiting for records that consumed from kafka for $kafkaSubscriberTimeout milliseconds\n")
      val records = kafkaConsumer.poll(kafkaSubscriberTimeout)
      records.asScala.foreach(x => blockingQueue.put(envelopeSerializer.serialize(consumerRecordToEnvelope(x))))
    }
  }

  override def setConsumerOffsetToLastEnvelope() = {
    super.setConsumerOffsetToLastEnvelope()
    offsetProducer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
      .send(offsetSerializer.serialize(kafkaOffsetsStorage))
  }
}

