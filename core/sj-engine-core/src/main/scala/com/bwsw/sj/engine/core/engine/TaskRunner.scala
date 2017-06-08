package com.bwsw.sj.engine.core.engine

import java.io.Closeable
import java.util.concurrent._

import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import scaldi.Injector

import scala.util.{Failure, Success, Try}

/**
  * Prepare a thread factory,
  * an executor service (for launching a task engine of specific type, a performance metrics and a specific input service for consuming incoming messages)
  * for a task runner
  *
  * Provides method that can be used to wait until some task will fail
  *
  * @author Kseniya Mikhaleva
  */
trait TaskRunner {
  implicit val injector: Injector = com.bwsw.sj.common.SjModule.injector
  private val logger = LoggerFactory.getLogger(this.getClass)
  protected val threadName: String
  private val countOfThreads = 4
  private val threadPool: ExecutorService = createThreadPool(threadName)
  private val executorService: ExecutorCompletionService[Unit] = new ExecutorCompletionService[Unit](threadPool)

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

  private def waitForCompletion(closeableTaskInput: Closeable): Unit = {
    var i = 0
    Try {
      while (i < countOfThreads) {
        executorService.take().get()
        i += 1
      }

      closeableTaskInput.close()
      threadPool.shutdownNow()
      System.exit(-1)
    } match {
      case Success(_) =>
      case Failure(requiringError: IllegalArgumentException) => handleException(requiringError)
      case Failure(exception) => handleException(exception)
    }
  }

  def main(args: Array[String]): Unit = {
    val manager = createTaskManager()

    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for '${manager.instance.moduleType}' module\n")

    val performanceMetrics = createPerformanceMetrics(manager)

    val taskEngine = createTaskEngine(manager, performanceMetrics)

    val taskInputService = createTaskInputService(manager, taskEngine)

    val instanceStatusObserver = new InstanceStatusObserver(manager.instanceName)

    logger.info(s"Task: ${manager.taskName}. The preparation finished. Launch a task\n")

    taskInputService match {
      case callable: Callable[Unit@unchecked] => executorService.submit(callable)
      case _ =>
    }
    executorService.submit(taskEngine)
    executorService.submit(performanceMetrics)
    executorService.submit(instanceStatusObserver)

    waitForCompletion(taskInputService)
  }

  protected def createTaskManager(): TaskManager

  protected def createPerformanceMetrics(manager: TaskManager): PerformanceMetrics

  protected def createTaskEngine(manager: TaskManager, performanceMetrics: PerformanceMetrics): TaskEngine

  protected def createTaskInputService(manager: TaskManager, taskEngine: TaskEngine): Closeable
}
