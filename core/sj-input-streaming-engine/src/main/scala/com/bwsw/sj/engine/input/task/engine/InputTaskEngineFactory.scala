package com.bwsw.sj.engine.input.task.engine

import com.bwsw.sj.engine.input.task.InputTaskManager
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import io.netty.buffer.ByteBuf
import org.slf4j.LoggerFactory

/**
 * Factory is in charge of creating of a task engine of input module
 * Created: 18/07/2016
 *
 * @param manager Manager of environment of task of input module
 * @author Kseniya Mikhaleva
 */

class InputTaskEngineFactory(manager: InputTaskManager, performanceMetrics: InputStreamingPerformanceMetrics, buffer: ByteBuf) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Input instance is a metadata for running a task of input module
   */
  private val inputInstanceMetadata = manager.getInstanceMetadata

  /**
   * Creates InputTaskEngine is in charge of a basic execution logic of task of input module
   * @return Engine of input task
   */
  def createInputTaskEngine() = {
    inputInstanceMetadata.checkpointMode match {
      case "time-interval" =>
        logger.info(s"Task: ${manager.taskName}. Input module has a 'time-interval' checkpoint mode, create an appropriate task engine\n")
        logger.debug(s"Task: ${manager.taskName}. Create TimeCheckpointInputTaskEngine()\n")
        new TimeCheckpointInputTaskEngine(manager, performanceMetrics, buffer)
      case "every-nth" =>
        logger.info(s"Task: ${manager.taskName}. Input module has an 'every-nth' checkpoint mode, create an appropriate task engine\n")
        new NumericalCheckpointInputTaskEngine(manager, performanceMetrics, buffer)

    }
  }
}
