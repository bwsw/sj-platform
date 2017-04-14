package com.bwsw.sj.engine.core.engine.input

import java.util.Date
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.DAL.model.TStreamSjStream
import com.bwsw.sj.common.DAL.model.module.{OutputInstance, RegularInstance, BatchInstance}
import com.bwsw.sj.common.engine.EnvelopeDataSerializer
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import com.bwsw.sj.engine.core.entities.{Envelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.tstreams.agents.consumer.Offset.{DateTime, IOffset, Newest, Oldest}
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

/**
  * Class is responsible for launching t-stream subscribing consumers
  * that put consumed message, which are wrapped in envelope, into a common queue,
  * and processing the envelopes
  *
  * @param manager       Manager of environment of task
  * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
  *                      which will be retrieved into a module
  * @author Kseniya Mikhaleva
  *
  */
class CallableTStreamTaskInput[T <: AnyRef](manager: TaskManager,
                                            blockingQueue: ArrayBlockingQueue[Envelope],
                                            override val checkpointGroup: CheckpointGroup = new CheckpointGroup())
  extends CallableTaskInput[TStreamEnvelope[T]](manager.inputs) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val consumers = createSubscribingConsumers()

  private def createSubscribingConsumers() = {
    logger.debug(s"Task: ${manager.taskName}. Start creating subscribing consumers.")
    val inputs = manager.inputs
    val offset = getOffset()
    val callback = new ConsumerCallback[T](manager.envelopeDataSerializer.asInstanceOf[EnvelopeDataSerializer[T]], blockingQueue)

    val consumers = inputs.filter(x => x._1.streamType == StreamLiterals.tstreamType)
      .map(x => (x._1.asInstanceOf[TStreamSjStream], x._2.toList))
      .map(x => manager.createSubscribingConsumer(x._1, x._2, offset, callback))
      .map(x => (x.name, x)).toMap
    logger.debug(s"Task: ${manager.taskName}. Creation of subscribing consumers is finished.")

    consumers
  }

  private def getOffset() = {
    logger.debug(s"Task: ${manager.taskName}. Get an offset parameter from instance.")
    val instance = manager.instance
    val offset = instance match {
      case instance: RegularInstance => instance.startFrom
      case instance: BatchInstance => instance.startFrom
      case instance: OutputInstance => instance.startFrom
      case badInstance =>
        logger.error(s"Task: ${manager.taskName}. Instance type isn't supported.")
        throw new TypeNotPresentException(badInstance.getClass.getName,
          new Throwable("Instance type isn't supported"))
    }

    chooseOffset(offset)
  }

  /**
    * Chooses offset policy for t-streams consumers
    *
    * @param startFrom Offset policy name or specific date
    * @return Offset
    */
  private def chooseOffset(startFrom: String): IOffset = {
    logger.debug(s"Choose offset policy for t-streams consumer.")
    startFrom match {
      case EngineLiterals.oldestStartMode => Oldest
      case EngineLiterals.newestStartMode => Newest
      case time => DateTime(new Date(time.toLong * 1000))
    }
  }

  def call() = {
    addConsumersToCheckpointGroup()
    launchConsumers()
  }

  private def addConsumersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding subscribing consumers to checkpoint group.")
    consumers.foreach(x => checkpointGroup.add(x._2.getConsumer()))
    logger.debug(s"Task: ${manager.taskName}. Adding subscribing consumers to checkpoint group is finished.")
  }

  private def launchConsumers() = {
    logger.debug(s"Task: ${manager.taskName}. Launch subscribing consumers.")
    consumers.foreach(_._2.start())
    logger.debug(s"Task: ${manager.taskName}. Subscribing consumers are launched.")
  }

  override def setConsumerOffset(envelope: TStreamEnvelope[T]) = {
    logger.debug(s"Task: ${manager.taskName}. " +
      s"Change local offset of consumer: ${envelope.consumerName} (partition: ${envelope.partition}) to txn: ${envelope.id}.")
    consumers(envelope.consumerName).getConsumer().setStreamPartitionOffset(envelope.partition, envelope.id)
  }

  override def close(): Unit = {
    consumers.foreach(_._2.stop())
  }
}
