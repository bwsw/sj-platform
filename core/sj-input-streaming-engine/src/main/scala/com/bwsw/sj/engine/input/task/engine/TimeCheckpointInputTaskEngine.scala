package com.bwsw.sj.engine.input.task.engine

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.engine.input.task.InputTaskManager
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext

import scala.collection.concurrent

/**
 * Provides methods are responsible for a basic execution logic of task of input module
 * that has a checkpoint based on time
 *
 * @param manager Manager of environment of task of input module
 * @param performanceMetrics Set of metrics that characterize performance of a input streaming module
 * @param channelContextQueue Queue for keeping a channel context to process messages (byte buffer) in their turn
 * @param bufferForEachContext Map for keeping a buffer containing incoming bytes with the channel context

 */
class TimeCheckpointInputTaskEngine(manager: InputTaskManager,
                                    performanceMetrics: InputStreamingPerformanceMetrics,
                                    channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                                    bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf])
  extends InputTaskEngine(manager, performanceMetrics, channelContextQueue, bufferForEachContext) {

  private val checkpointTimer: Option[SjTimer] = createTimer()
  val isNotOnlyCustomCheckpoint = checkpointTimer.isDefined

  /**
   * Creates a timer for performing checkpoints
   * @return Timer or nothing if instance has no timer and will do checkpoints by manual
   */
  private def createTimer() = {
    if (inputInstance.checkpointInterval > 0) {
      logger.debug(s"Task: ${manager.taskName}. Create a checkpoint timer for input module\n")
      Some(new SjTimer())
    } else {
      logger.debug(s"Task: ${manager.taskName}. Input module has not programmatic checkpoint. Manually only\n")
      None
    }
  }

  /**
   * Sets timer on checkpoint interval
   */
  private def setTimer() = {
    checkpointTimer.get.set(inputInstance.checkpointInterval)
  }

  /**
   * Does group checkpoint of t-streams consumers/producers
   * @param isCheckpointInitiated Flag points whether checkpoint was initiated inside input module (not on the schedule) or not.
   * @param ctx Channel context related with this input envelope to send a message about this event
   */
  def doCheckpoint(isCheckpointInitiated: Boolean, ctx: ChannelHandlerContext) = {
    if (isNotOnlyCustomCheckpoint && checkpointTimer.get.isTime || isCheckpointInitiated) {
      logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
      logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
      checkpointGroup.commit()
      checkpointInitiated(ctx)
      txnsByStreamPartitions = createTxnsStorage(streams)
      resetTimer()
    }
  }

  /**
   * Prepares a timer for next circle, e.i. reset a timer and set again
   */
  private def resetTimer() = {
    if (checkpointTimer.isDefined) {
      logger.debug(s"Task: ${manager.taskName}. Prepare a checkpoint timer for next cycle\n")
      checkpointTimer.get.reset()
      setTimer()
    }
  }
}

