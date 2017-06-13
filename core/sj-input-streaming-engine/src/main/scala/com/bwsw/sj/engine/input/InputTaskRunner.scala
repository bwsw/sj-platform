package com.bwsw.sj.engine.input

import java.io.Closeable
import java.util.concurrent._

import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.engine.core.engine.TaskRunner
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.input.connection.tcp.server.InputStreamingServer
import com.bwsw.sj.engine.input.task.{InputTaskEngine, InputTaskManager}
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext

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

  private val queueSize = 1000
  private val bufferForEachContext = new ConcurrentHashMap[ChannelHandlerContext, ByteBuf]().asScala
  private val channelContextQueue = new ArrayBlockingQueue[ChannelHandlerContext](queueSize)

  override protected def createTaskManager(): TaskManager = new InputTaskManager()

  override protected def createPerformanceMetrics(manager: TaskManager): PerformanceMetrics = {
    new InputStreamingPerformanceMetrics(manager.asInstanceOf[InputTaskManager])
  }

  override protected def createTaskEngine(manager: TaskManager, performanceMetrics: PerformanceMetrics): TaskEngine = {
    InputTaskEngine(
      manager.asInstanceOf[InputTaskManager],
      performanceMetrics.asInstanceOf[InputStreamingPerformanceMetrics],
      channelContextQueue,
      bufferForEachContext
    )
  }

  override protected def createTaskInputService(manager: TaskManager, taskEngine: TaskEngine): Closeable = {
    new InputStreamingServer(
      manager.agentsHost,
      manager.asInstanceOf[InputTaskManager].entryPort,
      channelContextQueue,
      bufferForEachContext
    )
  }
}
