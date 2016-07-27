package com.bwsw.sj.engine.regular

import java.util.concurrent.{ExecutorService, Executors}

import com.bwsw.sj.common.ModuleConstants
import com.bwsw.sj.engine.core.PersistentBlockingQueue
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.engine.RegularTaskEngineFactory
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory

/**
 * Object is responsible for running a task of job that launches regular module
 * Created: 13/04/2016
 *
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val threadFactory = createThreadFactory()
  private val executorService = Executors.newFixedThreadPool(3, threadFactory)

  private val blockingQueue: PersistentBlockingQueue = new PersistentBlockingQueue(ModuleConstants.persistentBlockingQueue)

  def main(args: Array[String]) {

    val manager = new RegularTaskManager()
    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for regular module\n")

    val performanceMetrics = new RegularStreamingPerformanceMetrics(manager)

    val regularTaskEngineFactory = new RegularTaskEngineFactory(manager, performanceMetrics, blockingQueue)

    val regularTaskEngine = regularTaskEngineFactory.createInputTaskEngine()

    val regularTaskInputService = regularTaskEngine.regularTaskInputService

    logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")
    try {
      executorService.execute(regularTaskInputService)
      executorService.execute(regularTaskEngine)
      executorService.execute(performanceMetrics)
    } catch {
      case exception: Exception => {
        handleExceptionOfExecutorService(exception, executorService)
      }
    }
  }


  def createThreadFactory() = {
    new ThreadFactoryBuilder()
      .setNameFormat("RegularTaskRunner-%d")
      .setDaemon(true)
      .build()
  }

  def handleExceptionOfExecutorService(exception: Exception, executorService: ExecutorService) = {
    exception.printStackTrace()
    executorService.shutdownNow()
    System.exit(-1)
  } //todo подумать над правильностью обработки ошибок в ExecutorService
}