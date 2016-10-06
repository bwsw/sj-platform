package com.bwsw.sj.engine.input.task.engine

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.{NumericalCheckpointTaskEngine, TimeCheckpointTaskEngine}
import com.bwsw.sj.engine.input.task.InputTaskManager
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.slf4j.LoggerFactory

import scala.collection.concurrent

/**
 * Factory is in charge of creating of a task engine of input module
 *
 *
 * @param manager Manager of environment of task of input module
 * @param performanceMetrics Set of metrics that characterize performance of a input streaming module
 * @param channelContextQueue Queue for keeping a channel context to process messages (byte buffer) in their turn
 * @param bufferForEachContext Map for keeping a buffer containing incoming bytes with the channel context

 * @author Kseniya Mikhaleva
 */

class InputTaskEngineFactory(manager: InputTaskManager,
                             performanceMetrics: InputStreamingPerformanceMetrics,
                             channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                             bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf]) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Creates InputTaskEngine is in charge of a basic execution logic of task of input module
   * @return Engine of input task
   */
  def createInputTaskEngine(): InputTaskEngine = {
    manager.inputInstance.checkpointMode match {
      case EngineLiterals.timeIntervalCheckpointMode =>
        logger.info(s"Task: ${manager.taskName}. Input module has a '${EngineLiterals.timeIntervalCheckpointMode}' checkpoint mode, create an appropriate task engine\n")
        new InputTaskEngine(manager, performanceMetrics, channelContextQueue, bufferForEachContext) with TimeCheckpointTaskEngine
      case EngineLiterals.everyNthCheckpointMode =>
        logger.info(s"Task: ${manager.taskName}. Input module has an '${EngineLiterals.everyNthCheckpointMode}' checkpoint mode, create an appropriate task engine\n")
        new InputTaskEngine(manager, performanceMetrics, channelContextQueue, bufferForEachContext) with NumericalCheckpointTaskEngine
    }
  }
}