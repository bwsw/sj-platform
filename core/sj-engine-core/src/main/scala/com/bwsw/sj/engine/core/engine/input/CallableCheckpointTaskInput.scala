package com.bwsw.sj.engine.core.engine.input

import java.util.concurrent.{ArrayBlockingQueue, Callable}

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

abstract class CallableCheckpointTaskInput[T <: Envelope](inputs: scala.collection.mutable.Map[SjStream, Array[Int]]) extends CheckpointTaskInput with Callable[Unit] {
   def close()
}

object CallableCheckpointTaskInput {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply[T <: AnyRef](manager: CommonTaskManager, blockingQueue: ArrayBlockingQueue[Envelope], checkpointGroup: CheckpointGroup) = {
    val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tstreamType)

    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new CallableCompleteCheckpointTaskInput[T](manager, blockingQueue, checkpointGroup)
      case (false, true) => new CallableTStreamCheckpointTaskInput[T](manager, blockingQueue, checkpointGroup)
      case (true, false) => new CallableKafkaCheckpointTaskInput[T](manager, blockingQueue, checkpointGroup)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new Exception("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}