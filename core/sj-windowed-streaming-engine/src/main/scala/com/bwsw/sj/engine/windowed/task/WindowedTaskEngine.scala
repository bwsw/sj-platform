package com.bwsw.sj.engine.windowed.task

import java.util.concurrent.{ArrayBlockingQueue, Callable, TimeUnit}

import com.bwsw.common.LeaderLatch
import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.{EngineLiterals, SjStreamUtils}
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.state.{StatefulCommonModuleService, StatelessCommonModuleService}
import com.bwsw.sj.engine.core.windowed.{WindowRepository, WindowedStreamingExecutor}
import com.bwsw.sj.engine.windowed.task.input.RetrievableTaskInput
import com.bwsw.sj.engine.windowed.task.reporting.WindowedStreamingPerformanceMetrics
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.barriers.DistributedDoubleBarrier
import org.apache.curator.retry.ExponentialBackoffRetry
import org.slf4j.LoggerFactory

class WindowedTaskEngine(protected val manager: CommonTaskManager,
                         inputService: RetrievableTaskInput[_ >: TStreamEnvelope with KafkaEnvelope <: Envelope],
                         batchQueue: ArrayBlockingQueue[Batch],
                         performanceMetrics: WindowedStreamingPerformanceMetrics) extends Callable[Unit] {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"windowed-task-${manager.taskName}-engine")
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val instance = manager.instance.asInstanceOf[WindowedInstance]
  private val mainStream = SjStreamUtils.clearStreamFromMode(instance.mainStream)
  private val moduleService = createWindowedModuleService()
  private val executor = moduleService.executor.asInstanceOf[WindowedStreamingExecutor[manager._type.type]]
  private val moduleTimer = moduleService.moduleTimer
  private var counterOfBatches = 0
  private val windowPerStream = createStorageOfWindows()
  private val windowRepository = new WindowRepository(instance, manager.inputs)
  private val barrierMasterNode = EngineLiterals.windowedInstanceBarrierPrefix + instance.name
  private val leaderMasterNode = EngineLiterals.windowedInstanceLeaderPrefix + instance.name
  private val zkHosts = instance.coordinationService.provider.hosts.toSet
  private val curatorClient = createCuratorClient()
  private val barrier = new DistributedDoubleBarrier(curatorClient, barrierMasterNode, instance.executionPlan.tasks.size())
  private val leaderLatch = new LeaderLatch(zkHosts, leaderMasterNode)
  leaderLatch.start()

  protected def createWindowedModuleService() = {
    instance.stateManagement match {
      case EngineLiterals.noneStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of windowed module without a state.")
        new StatelessCommonModuleService(manager, inputService.checkpointGroup, performanceMetrics)
      case EngineLiterals.ramStateMode =>
        logger.debug(s"Task: ${manager.taskName}. Start preparing of windowed module with a state.")
        new StatefulCommonModuleService(manager, inputService.checkpointGroup, performanceMetrics)
    }
  }

  private def createStorageOfWindows() = {
    manager.inputs.map(x => (x._1.name, new Window(x._1.name)))
  }

  private def createCuratorClient() = {
    val curatorClient = CuratorFrameworkFactory.newClient(zkHosts.mkString(","), new ExponentialBackoffRetry(1000, 3))
    curatorClient.start()
    curatorClient.getZookeeperClient.blockUntilConnectedOrTimedOut()

    curatorClient
  }

  /**
   * It is in charge of running a basic execution logic of windowed task engine
   */
  override def call(): Unit = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run windowed task engine in a separate thread of execution service.")
    logger.debug(s"Task: ${manager.taskName}. Invoke onInit() handler.")
    executor.onInit()

    while (true) {
      val maybeBatch = Option(batchQueue.poll(instance.eventWaitIdleTime, TimeUnit.MILLISECONDS))

      maybeBatch match {
        case Some(batch) => {
          registerBatch(batch)

          if (isItTimeToCollectWindow()) {
            collectWindow()
            registerWindow()
            executor.onWindow(windowRepository)
            barrier.enter()
            executor.onEnter()
            if (leaderLatch.hasLeadership()) executor.onLeaderEnter()
            barrier.leave()
            executor.onLeave()
            if (leaderLatch.hasLeadership()) executor.onLeaderLeave()
            slideWindow()
            doCheckpoint()
          }
        }
        case None => {
          performanceMetrics.increaseTotalIdleTime(instance.eventWaitIdleTime)
          executor.onIdle()
        }
      }

      if (moduleTimer.isTime) {
        logger.debug(s"Task: ${manager.taskName}. Invoke onTimer() handler.")
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
    counterOfBatches == instance.window
  }

  private def collectWindow() = {
    logger.info(s"Task: ${manager.taskName}. It's time to collect batch.")
    windowPerStream.foreach(x => windowRepository.put(x._1, x._2.copy()))
  }

  private def registerWindow() = {
    windowPerStream.foreach(x => performanceMetrics.addWindow(x._2))
  }

  private def increaseBatchCounter() = {
    logger.debug(s"Increase count of batches.")
    counterOfBatches += 1
  }

  private def slideWindow() = {
    registerBatches()
    deleteBatches()
    resetCounter()
  }

  private def registerBatches() = {
    windowPerStream.foreach(x => {
      x._2.batches.slice(0, instance.slidingInterval)
        .foreach(x => x.envelopes.foreach(x => inputService.registerEnvelope(x)))
    })
  }

  private def deleteBatches() = {
    windowPerStream.foreach(x => x._2.batches.remove(0, instance.slidingInterval))
  }

  private def resetCounter() = {
    logger.debug(s"Reset a counter of batches to 0.")
    counterOfBatches -= instance.slidingInterval
  }

  /**
   * Does group checkpoint of t-streams consumers/producers
   */
  private def doCheckpoint() = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint.")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeCheckpoint() handler.")
    executor.onBeforeCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Do group checkpoint.")
    moduleService.doCheckpoint()
    inputService.doCheckpoint()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterCheckpoint() handler.")
    executor.onAfterCheckpoint()
  }
}