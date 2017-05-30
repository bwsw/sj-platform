package com.bwsw.sj.engine.batch.task.input

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.engine.input.CheckpointTaskInput
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory
import scaldi.Injector

/**
  * Class is responsible for handling an input streams of specific type,
  * i.e. for consuming and processing the incoming envelopes
  *
  * @author Kseniya Mikhaleva
  */
abstract class RetrievableCheckpointTaskInput[T <: Envelope](val inputs: scala.collection.mutable.Map[StreamDomain, Array[Int]]) extends CheckpointTaskInput[T](inputs) {
  def get(): Iterable[T]
}

object RetrievableCheckpointTaskInput {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply[T <: AnyRef](manager: CommonTaskManager, checkpointGroup: CheckpointGroup)
                        (implicit injector: Injector): RetrievableCheckpointTaskInput[_ <: Envelope] = {
    val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tstreamType)

    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new RetrievableCompleteCheckpointTaskInput[T](manager, checkpointGroup)
      case (false, true) => new RetrievableTStreamCheckpointTaskInput[T](manager, checkpointGroup)
      case (true, false) => new RetrievableKafkaCheckpointTaskInput[T](manager, checkpointGroup)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new RuntimeException("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}