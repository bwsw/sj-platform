package com.bwsw.sj.engine.input

import java.util.concurrent._

import com.bwsw.sj.engine.input.connection.tcp.server.InputStreamingServer
import com.bwsw.sj.engine.input.task.InputTaskManager
import com.bwsw.sj.engine.input.task.engine.InputTaskEngineFactory
import com.bwsw.sj.engine.input.task.reporting.InputStreamingPerformanceMetrics
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import org.slf4j.LoggerFactory
import scala.collection.convert.decorateAsScala._

/**
 * Object is responsible for running a task of job that launches input module
 * Created: 07/07/2016
 *
 * @author Kseniya Mikhaleva
 */

object InputTaskRunner {

  val logger = LoggerFactory.getLogger(this.getClass)
  val countOfThreads = 2
  val queueSize = 1000
  val threadFactory = createThreadFactory()
  val threadPool = Executors.newFixedThreadPool(countOfThreads, threadFactory)
  val executorService = new ExecutorCompletionService[Unit](threadPool)

  def main(args: Array[String]) {

    val bufferForEachContext = (new ConcurrentHashMap[ChannelHandlerContext, ByteBuf]()).asScala
    val channelContextQueue = new ArrayBlockingQueue[ChannelHandlerContext](queueSize)

    val manager: InputTaskManager = new InputTaskManager()
    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for input module\n")

    val performanceMetrics = new InputStreamingPerformanceMetrics(manager)

    val inputTaskEngineFactory = new InputTaskEngineFactory(manager, performanceMetrics, channelContextQueue, bufferForEachContext)

    val inputTaskEngine = inputTaskEngineFactory.createInputTaskEngine()

    logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")
    try {
      executorService.submit(inputTaskEngine)
      executorService.submit(performanceMetrics)

      executorService.take().get()
    } catch {
      case exception: Exception => {
        handleExceptionOfExecutorService(exception)
      }
    }

    logger.info(s"Task: ${manager.taskName}. " +
      s"Launch input streaming server on: '${manager.agentsHost}:${manager.entryPort}'\n")
    new InputStreamingServer(
      manager.agentsHost,
      manager.entryPort,
      inputTaskEngine.executor,
      channelContextQueue, bufferForEachContext
    ).run()
  }

  def createThreadFactory() = {
    new ThreadFactoryBuilder()
      .setNameFormat("InputTaskRunner-%d")
      .setDaemon(true)
      .build()
  }

  def handleExceptionOfExecutorService(exception: Exception) = {
    exception.printStackTrace()
    threadPool.shutdownNow()
    System.exit(-1)
  }
}
