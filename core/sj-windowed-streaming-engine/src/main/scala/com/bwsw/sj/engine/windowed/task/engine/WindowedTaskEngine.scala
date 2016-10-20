package com.bwsw.sj.engine.windowed.task.engine

import java.util.concurrent.{ArrayBlockingQueue, Callable, TimeUnit}

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.entities.{Batch, Window}
import com.bwsw.sj.engine.core.windowed.WindowRepository
import com.bwsw.sj.engine.windowed.task.WindowedTaskManager
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
  private val windowRepository = new WindowRepository(instance, manager.inputs)
  private var currentWindowId = ""

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
           // println("before sliding " + windowPerStream.map(x => (x._1, x._2.batches.map(x => x.transactions.map(_.id)))))
            executor.onWindow(batch.stream, windowRepository)
            slideWindow()
            //println("after sliding " + windowPerStream.map(x => (x._1, x._2.batches.map(x => x.transactions.map(_.id)))))
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
    currentWindowId = batch.stream
    windowPerStream(currentWindowId).batches += batch
    afterReceivingBatch()
  }

  private def isItTimeToCollectWindow(): Boolean = {
    countersOfBatches(currentWindowId) == instance.window
  }

  private def collectWindow() = {
    logger.info(s"Task: ${manager.taskName}. It's time to collect batch\n")
    windowRepository.put(currentWindowId, windowPerStream(currentWindowId).copy())
  }

  private def afterReceivingBatch() = {
    increaseCounter()
  }

  private def increaseCounter() = {
    logger.debug(s"Increase count of batches\n")
    countersOfBatches(currentWindowId) += 1
  }

  private def slideWindow() = {
    deleteBatches()
    increaseBatchesCounterOfAppearing()
    resetCounter()
  }

  private def deleteBatches() = {
    windowPerStream(currentWindowId).batches.remove(0, instance.slidingInterval)
  }

  private def increaseBatchesCounterOfAppearing() = {
    windowPerStream(currentWindowId).batches.foreach(x => x.countOfAppearing += 1)
  }

  private def resetCounter() = {
    logger.debug(s"Reset a counter of batches to 0\n")
    countersOfBatches(currentWindowId) -= instance.slidingInterval
  }
}