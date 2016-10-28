package com.bwsw.sj.engine.windowed.task.engine

import java.util.concurrent.{ArrayBlockingQueue, Callable, TimeUnit}

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.{EngineLiterals, SjStreamUtils}
import com.bwsw.sj.engine.core.engine.input.TaskInputService
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.state.{StatefulCommonModuleService, StatelessCommonModuleService}
import com.bwsw.sj.engine.core.windowed.{WindowRepository, WindowedStreamingExecutor}
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

class WindowedTaskEngine(protected val manager: CommonTaskManager,
                         taskInputService: TaskInputService[_ >: TStreamEnvelope with KafkaEnvelope <: Envelope],
                         batchQueue: ArrayBlockingQueue[Batch],
                         performanceMetrics: WindowedStreamingPerformanceMetrics) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"windowed-task-${manager.taskName}-engine")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val instance = manager.instance.asInstanceOf[WindowedInstance]
  private val mainStream = SjStreamUtils.clearStreamFromMode(instance.mainStream)
  private val moduleService = createWindowedModuleService()
  private val executor = moduleService.executor.asInstanceOf[WindowedStreamingExecutor]
  private val moduleTimer = moduleService.moduleTimer
  private var countersOfBatches = 0
  private val windowPerStream = createStorageOfWindows()
  private val windowRepository = new WindowRepository(instance, manager.inputs)

  protected def createWindowedModuleService() = {
    instance.stateManagement match {
      case EngineLiterals.noneStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of windowed module without state\n")
        new StatelessCommonModuleService(manager, taskInputService.checkpointGroup, performanceMetrics)
      case EngineLiterals.ramStateMode =>
        new StatefulCommonModuleService(manager, taskInputService.checkpointGroup, performanceMetrics)
    }
  }

  private def createStorageOfWindows() = {
    manager.inputs.map(x => (x._1.name, new Window(x._1.name)))
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
          println("batch: " + batch.stream + ":" + batch.envelopes.size) //todo
          registerBatch(batch)

          if (isItTimeToCollectWindow()) {
            collectWindow()
            registerWindow()
            executor.onWindow(windowRepository)
            slideWindow()
            doCheckpoint()
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

  private def registerBatch(batch: Batch) = {
    addBatchToWindow(batch)
    performanceMetrics.addBatch(batch)
  }

  private def addBatchToWindow(batch: Batch) = {
    windowPerStream(batch.stream).batches += batch
    if (batch.stream == mainStream) increaseBatchCounter()
  }

  private def isItTimeToCollectWindow(): Boolean = {
    countersOfBatches == instance.window
  }

  private def collectWindow() = {
    logger.info(s"Task: ${manager.taskName}. It's time to collect batch\n")
    windowPerStream.foreach(x => windowRepository.put(x._1, x._2.copy()))
  }

  private def registerWindow() = {
    windowPerStream.foreach(x => performanceMetrics.addWindow(x._2))
  }

  private def increaseBatchCounter() = {
    logger.debug(s"Increase count of batches\n")
    countersOfBatches += 1
  }

  private def slideWindow() = {
    registerBatches()
    deleteBatches()
    increaseBatchesCounterOfAppearing()
    resetCounter()
  }

  private def registerBatches() = {
    windowPerStream.foreach(x => {
      x._2.batches.slice(0, instance.slidingInterval)
        .foreach(x => x.envelopes.foreach(x => taskInputService.registerEnvelope(x)))
    })
  }

  private def deleteBatches() = {
    windowPerStream.foreach(x => x._2.batches.remove(0, instance.slidingInterval))
  }

  private def increaseBatchesCounterOfAppearing() = {
    windowPerStream.foreach(x => x._2.batches.foreach(x => x.countOfAppearing += 1))
  }

  private def resetCounter() = {
    logger.debug(s"Reset a counter of batches to 0\n")
    countersOfBatches -= instance.slidingInterval
  }

  /**
   * Does group checkpoint of t-streams consumers/producers
   */
  private def doCheckpoint() = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint\n")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeCheckpoint() handler\n")
    executor.onBeforeCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint\n")
    moduleService.doCheckpoint()
    taskInputService.doCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler\n")
    executor.onAfterCheckpoint()
  }
}