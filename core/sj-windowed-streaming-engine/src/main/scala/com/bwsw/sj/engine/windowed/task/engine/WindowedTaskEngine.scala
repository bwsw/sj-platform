package com.bwsw.sj.engine.windowed.task.engine

import java.util.concurrent.{TimeUnit, Callable, ArrayBlockingQueue}

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager
import com.bwsw.sj.engine.windowed.task.engine.entities.{Window, Batch}
import com.bwsw.sj.engine.windowed.task.engine.state.{StatefulWindowedTaskEngineService, StatelessWindowedTaskEngineService}
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.Producer
import org.slf4j.LoggerFactory

class WindowedTaskEngine(protected val manager: WindowedTaskManager,
                      batchQueue: ArrayBlockingQueue[Batch],
                      performanceMetrics: WindowedStreamingPerformanceMetrics) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"windowed-task-${manager.taskName}-engine")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val producers: Map[String, Producer[Array[Byte]]] = manager.outputProducers
  private val checkpointGroup = new CheckpointGroup()
  private val instance = manager.instance.asInstanceOf[WindowedInstance]
  private val windowedTaskEngineService = createWindowedTaskEngineService()
  private val executor = windowedTaskEngineService.executor
  private val moduleTimer = windowedTaskEngineService.moduleTimer
  private val countersOfBatches = createBatchCounters()
  private val windowPerStream = createStorageOfWindows()
  private val windows = createStorageOfWindows()
  private var lastBatchId = ""

  addProducersToCheckpointGroup()

  protected def createWindowedTaskEngineService() = {
    instance.stateManagement match {
      case EngineLiterals.noneStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of windowed module without state\n")
        new StatelessWindowedTaskEngineService(manager, performanceMetrics)
      case EngineLiterals.ramStateMode =>
        new StatefulWindowedTaskEngineService(manager, checkpointGroup, performanceMetrics)
    }
  }

  private def createBatchCounters() = {
    manager.inputs.map(x => (x._1.name, 0))
  }

  private def createStorageOfWindows() = {
    manager.inputs.map(x => (x._1.name, new Window(instance.slidingInterval)))
  }

  private def addProducersToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group\n")
    producers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group\n")
  }

  /**
   * It is in charge of running a basic execution logic of windowed task engine
   */
  override def call(): Unit = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run windowed task engine in a separate thread of execution service\n")
    logger.debug(s"Task: ${manager.taskName}. Invoke onInit() handler\n")
    executor.onInit()

    while (true) {
      val maybeBatch = Option(batchQueue.poll(instance.eventWaitTime, TimeUnit.MILLISECONDS))

      maybeBatch match {
        case Some(batch) => {
          println("batch: " + batch.stream + ":" + batch.transactions.size)
          addBatchToWindow(batch)

          if (isItTimeToCollectWindow()) {
            collectWindow() //todo либо класть в очередь батчей сначала все батчи для завивимых потоков, потом для основного
            //todo либо кол-чо батчей не будет совпадать у главного окна и зависимых окон
            executor.onWindow() //todo сделать доступ к windows
            println("onWindow() " + windows.map(x => (x._1, x._2.batches.flatMap(x=> x.transactions.map(_.id)))))
          }
        }
        case None => {
          performanceMetrics.increaseTotalIdleTime(instance.eventWaitTime)
          executor.onIdle()
        }
      }

      if (moduleTimer.isTime) {
        logger.debug(s"Task: ${manager.taskName}. Invoke onTimer() handler\n")
        executor.onTimer(System.currentTimeMillis() - moduleTimer.responseTime)
        moduleTimer.reset()
      }
    }
  }

  private def addBatchToWindow(batch: Batch) = {
    lastBatchId = batch.stream
    windowPerStream(lastBatchId).batches += batch
    afterReceivingBatch()
  }

  private def isItTimeToCollectWindow(): Boolean = {
    countersOfBatches(lastBatchId) == instance.window
  }

  private def collectWindow() = {
    logger.info(s"Task: ${manager.taskName}. It's time to collect batch\n")
    windows(lastBatchId) = windowPerStream(lastBatchId).copy() //todo сделать доступным в onWindow()
    prepareWindowForNextCollecting()
  }

  private def afterReceivingBatch() = {
    increaseCounter()
  }

  private def increaseCounter() = {
    logger.debug(s"Increase count of batches\n")
    countersOfBatches(lastBatchId) += 1
  }

  private def prepareWindowForNextCollecting() = {
    windowPerStream(lastBatchId).batches.clear()
    resetCounter()
  }

  private def resetCounter() = {
    logger.debug(s"Reset a counter of batches to 0\n")
    countersOfBatches(lastBatchId) = 0
  }
}
