package com.bwsw.sj.engine.batch.task.input

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.engine.input.TaskInput
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import org.slf4j.LoggerFactory

/**
 * Class is responsible for handling an input streams of specific type(types),
 * i.e. for consuming, processing and sending the input envelopes
 *
 * @author Kseniya Mikhaleva
 */
abstract class RetrievableTaskInput[T <: Envelope](val inputs: scala.collection.mutable.Map[SjStream, Array[Int]]) extends TaskInput[T](inputs) {
  def get(): Iterable[T]
}

object RetrievableTaskInput {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply[T <: AnyRef](manager: CommonTaskManager) = {
    val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tstreamType)

    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new RetrievableCompleteTaskInput[T](manager)
      case (false, true) => new RetrievableTStreamTaskInput[T](manager)
      case (true, false) => new RetrievableKafkaTaskInput[T](manager)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new RuntimeException("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}