package com.bwsw.sj.engine.windowed.task.engine.input

import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import org.slf4j.LoggerFactory

class InputFactory(manager: CommonTaskManager) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
  private val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tStreamType)

  def createInputService() = {
    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new CompleteInput(manager)
      case (false, true) => new TStreamInput(manager)
      case (true, false) => new KafkaInput(manager)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new Exception("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}
