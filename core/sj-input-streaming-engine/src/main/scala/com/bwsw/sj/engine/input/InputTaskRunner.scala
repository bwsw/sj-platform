package com.bwsw.sj.engine.input

import java.util.concurrent._

import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.input.connection.tcp.server.InputStreamingServer
import com.bwsw.sj.engine.input.task.{InputTaskEngine, InputTaskManager}
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.slf4j.LoggerFactory

import scala.collection.convert.decorateAsScala._

/**
 * Object is responsible for running a task of job that launches input module
 *
 *
 * @author Kseniya Mikhaleva
 */

object InputTaskRunner extends {override val threadName = "InputTaskRunner-%d"} with TaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val queueSize = 1000

  def main(args: Array[String]) {
    try {
      val bufferForEachContext = (new ConcurrentHashMap[ChannelHandlerContext, ByteBuf]()).asScala
      val channelContextQueue = new ArrayBlockingQueue[ChannelHandlerContext](queueSize)

      val manager: InputTaskManager = new InputTaskManager()
      logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for an input module\n")

      val performanceMetrics = new InputStreamingPerformanceMetrics(manager)

      val inputTaskEngine = InputTaskEngine(manager, performanceMetrics, channelContextQueue, bufferForEachContext)

      val inputStreamingServer = new InputStreamingServer(
        manager.agentsHost,
        manager.entryPort,
        channelContextQueue,
        bufferForEachContext
      )

      logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")

      executorService.submit(inputTaskEngine)
      executorService.submit(performanceMetrics)
      executorService.submit(inputStreamingServer)

      executorService.take().get()
    } catch {
      case requiringError: IllegalArgumentException => handleException(requiringError)
      case exception: Exception => handleException(exception)
    }
  }
}
