package com.bwsw.sj.engine.core.engine.input

import java.util.concurrent.Callable

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.engine.PersistentBlockingQueue
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import org.slf4j.LoggerFactory

abstract class CallableTaskInput[T <: Envelope](inputs: scala.collection.mutable.Map[SjStream, Array[Int]]) extends TaskInput[T](inputs) with Callable[Unit]

object CallableTaskInput {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(manager: CommonTaskManager, blockingQueue: PersistentBlockingQueue) = {
    val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tstreamType)

    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new CallableCompleteTaskInput(manager, blockingQueue)
      case (false, true) => new CallableTStreamTaskInput(manager, blockingQueue)
      case (true, false) => new CallableKafkaTaskInput(manager, blockingQueue)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new Exception("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}