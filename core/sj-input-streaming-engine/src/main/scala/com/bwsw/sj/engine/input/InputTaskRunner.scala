package com.bwsw.sj.engine.input

import java.util.concurrent._

import com.bwsw.sj.engine.core.engine.{InstanceStatusObserver, TaskRunner}
import com.bwsw.sj.engine.input.connection.tcp.server.InputStreamingServer
import com.bwsw.sj.engine.input.task.{InputTaskEngine, InputTaskManager}
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
  * Class is responsible for launching input engine execution logic.
  * First, there are created all services needed to start engine. All of those services implement Callable interface
  * Next, each service are launched as a separate task using [[ExecutorCompletionService]]
  * Finally, handle a case if some task will fail and stop the execution. In other case the execution will go on indefinitely
  *
  * @author Kseniya Mikhaleva
  */
object InputTaskRunner extends {
  override val threadName = "InputTaskRunner-%d"
} with TaskRunner {

  import com.bwsw.sj.common.SjModule._

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val queueSize = 1000

  def main(args: Array[String]) {
    val bufferForEachContext = new ConcurrentHashMap[ChannelHandlerContext, ByteBuf]().asScala
    val channelContextQueue = new ArrayBlockingQueue[ChannelHandlerContext](queueSize)

    val manager: InputTaskManager = new InputTaskManager()
    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for an input module.")

    val performanceMetrics = new InputStreamingPerformanceMetrics(manager)

    val inputTaskEngine = InputTaskEngine(manager, performanceMetrics, channelContextQueue, bufferForEachContext)

    val inputStreamingServer = new InputStreamingServer(
      manager.agentsHost,
      manager.entryPort,
      channelContextQueue,
      bufferForEachContext
    )

    val instanceStatusObserver = new InstanceStatusObserver(manager.instanceName)

    logger.info(s"Task: ${manager.taskName}. The preparation finished. Launch a task.")

    executorService.submit(inputTaskEngine)
    executorService.submit(performanceMetrics)
    executorService.submit(inputStreamingServer)
    executorService.submit(instanceStatusObserver)

    waitForCompletion()
  }
}
