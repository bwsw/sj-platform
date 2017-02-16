package com.bwsw.sj.engine.core.engine.input

import java.util.concurrent.{ArrayBlockingQueue, Callable}

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

abstract class CallableTaskInput[T <: Envelope](inputs: scala.collection.mutable.Map[SjStream, Array[Int]]) extends TaskInput[T](inputs) with Callable[Unit] {
  def close()
}

object CallableTaskInput {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply[T](manager: CommonTaskManager, blockingQueue: ArrayBlockingQueue[Envelope], checkpointGroup: CheckpointGroup) = {
    val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tstreamType)

    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new CallableCompleteTaskInput[T](manager, blockingQueue, checkpointGroup)
      case (false, true) => new CallableTStreamTaskInput[T](manager, blockingQueue, checkpointGroup)
      case (true, false) => new CallableKafkaTaskInput[T](manager, blockingQueue, checkpointGroup)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new Exception("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}