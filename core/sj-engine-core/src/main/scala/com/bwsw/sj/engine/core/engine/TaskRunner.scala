package com.bwsw.sj.engine.core.engine

import java.util.concurrent.{ExecutorCompletionService, ExecutorService, Executors, ThreadFactory}

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory

/**
  * Prepare a thread factory,
  * an executor service (for launching a task engine of specific type, a performance metrics and a specific input service for consuming incoming messages)
  * and a blocking queue (for keeping incoming messages excluding the input task runner) for a task runner
  *
  * Provides methods that can be used to handle exceptions
  *
  * @author Kseniya Mikhaleva
  */
trait TaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)
  protected val threadName: String
  private val countOfThreads = 4
  private val threadPool: ExecutorService = createThreadPool(threadName)
  protected val executorService = new ExecutorCompletionService[Unit](threadPool)

  private def createThreadPool(factoryName: String): ExecutorService = {
    logger.debug(s"Create a thread pool with $countOfThreads threads for task.")
    val threadFactory = createThreadFactory(factoryName)

    Executors.newFixedThreadPool(countOfThreads, threadFactory)
  }

  private def createThreadFactory(name: String): ThreadFactory = {
    logger.debug("Create a thread factory.")
    new ThreadFactoryBuilder()
      .setNameFormat(name)
      .build()
  }

  private def handleException(exception: Throwable): Unit = {
    logger.error("Runtime exception", exception)
    exception.printStackTrace()
    threadPool.shutdownNow()
    System.exit(-1)
  }

  def waitForCompletion(): Unit = {
    var i = 0
    try {
      while (i < countOfThreads) {
        executorService.take().get()
        i += 1
      }

      threadPool.shutdownNow()
      System.exit(-1)
    } catch {
      case requiringError: IllegalArgumentException => handleException(requiringError)
      case exception: Exception => handleException(exception)
    }
  }
}
