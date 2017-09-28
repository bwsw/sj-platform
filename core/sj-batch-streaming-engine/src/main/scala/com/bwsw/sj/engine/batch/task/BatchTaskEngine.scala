/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.engine.batch.task

import com.bwsw.common.LeaderLatch
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.common.engine.core.batch.{BatchStreamingExecutor, BatchStreamingPerformanceMetrics, WindowRepository}
import com.bwsw.sj.common.engine.core.entities._
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.engine.core.state.CommonModuleService
import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.batch.task.input.{EnvelopeFetcher, RetrievableCheckpointTaskInput}
import com.typesafe.scalalogging.Logger
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
  * Class contains methods for running batch module
  *
  * moduleService   provides an executor of batch streaming module and a method to perform checkpoint
  * inputService    accesses to incoming envelopes
  * batchCollector     gathering batches that consist of envelopes
  * instance           set of settings of a batch streaming module
  *
  * @param manager            allows to manage an environment of regular streaming task
  * @param performanceMetrics set of metrics that characterize performance of a batch streaming module
  */
class BatchTaskEngine(manager: CommonTaskManager,
                      performanceMetrics: BatchStreamingPerformanceMetrics,
                      lowWatermark: Int)
                     (implicit injector: Injector)
  extends TaskEngine {

  private val currentThread = Thread.currentThread()
  currentThread.setName(s"batch-task-engine")
  private val logger = Logger(this.getClass)
  private val instance = manager.instance.asInstanceOf[BatchInstance]
  private val inputs = instance.getInputsWithoutStreamMode
  private val batchCollector = manager.getBatchCollector(instance.to, performanceMetrics, inputs)
  private val checkpointGroup = manager.createCheckpointGroup()
  private val moduleService = CommonModuleService(manager, checkpointGroup, performanceMetrics)
  private val executor = moduleService.executor.asInstanceOf[BatchStreamingExecutor[AnyRef]]
  val taskInputService: RetrievableCheckpointTaskInput[Envelope] =
    RetrievableCheckpointTaskInput[AnyRef](
      manager.asInstanceOf[CommonTaskManager],
      checkpointGroup,
      executor
    ).asInstanceOf[RetrievableCheckpointTaskInput[Envelope]]
  private val envelopeFetcher = new EnvelopeFetcher(taskInputService, lowWatermark)
  private var retrievableStreams = instance.getInputsWithoutStreamMode
  private val counterOfBatchesPerStream = createCountersOfBatches()
  private val currentWindowPerStream = createStorageOfWindows()
  private val collectedWindowPerStream = mutable.Map[String, Window]()
  private val windowRepository = new WindowRepository(instance)
  private val barrierMasterNode = EngineLiterals.batchInstanceBarrierPrefix + instance.name
  private val leaderMasterNode = EngineLiterals.batchInstanceLeaderPrefix + instance.name
  private val zkHosts = inject[ConnectionRepository].getServiceRepository
    .get(instance.coordinationService)
    .get
    .asInstanceOf[ZKServiceDomain]
    .provider.hosts.toSet

  private val commonBarrier = new TwoBarriers(
    zkHosts,
    barrierMasterNode,
    instance.executionPlan.tasks.size())
  private val leaderLatch = new LeaderLatch(zkHosts, leaderMasterNode)
  leaderLatch.start()

  private def createCountersOfBatches(): mutable.Map[String, Int] = {
    mutable.Map(inputs.map(x => (x, 0)): _*)
  }

  private def createStorageOfWindows(): mutable.Map[String, Window] = {
    mutable.Map(inputs.map(x => (x, new Window(x))): _*)
  }

  /**
    * It is in charge of running a basic execution logic of batch task engine
    */
  override def call(): Unit = {
    logger.info(s"Run batch task engine in a separate thread of execution service.")
    logger.debug(s"Invoke onInit() handler.")
    executor.onInit()

    while (true) {
      retrieveAndProcessEnvelopes()
    }
  }

  private def retrieveAndProcessEnvelopes(): Unit = {
    retrievableStreams.foreach(stream => {
      logger.debug(s"Retrieve an available envelope from '$stream' stream.")
      envelopeFetcher.get(stream) match {
        case Some(envelope) => batchCollector.onReceive(envelope)
        case None => onIdle()
      }

      processBatches()

      moduleService.onTimer()

      if (allWindowsCollected) {
        onWindow()
      }
    })
  }

  private def processBatches(): Unit = {
    logger.debug(s"Check whether there are batches to collect or not.")
    val batches = batchCollector.getBatchesToCollect().map(batchCollector.collectBatch)
    if (batches.nonEmpty) {
      batches.foreach(batch => {
        registerBatch(batch)

        if (isItTimeToCollectWindow(batch.stream)) {
          collectWindow(batch.stream)
          retrievableStreams = retrievableStreams.filter(_ != batch.stream)
        }
      })
    }
  }

  private def onIdle(): Unit = {
    logger.debug(s"An envelope has not been received.")
    performanceMetrics.increaseTotalIdleTime(instance.eventWaitIdleTime)
    executor.onIdle()
    Thread.sleep(instance.eventWaitIdleTime)
  }

  private def registerBatch(batch: Batch): ListBuffer[Int] = {
    addBatchToWindow(batch)
    performanceMetrics.addBatch(batch)
  }

  private def addBatchToWindow(batch: Batch): Unit = {
    currentWindowPerStream(batch.stream).batches += batch
    increaseBatchCounter(batch.stream)
  }

  private def increaseBatchCounter(stream: String): Unit = {
    counterOfBatchesPerStream(stream) += 1
    logger.debug(s"Increase count of batches of stream: $stream to: ${counterOfBatchesPerStream(stream)}.")
  }

  private def isItTimeToCollectWindow(stream: String): Boolean = {
    counterOfBatchesPerStream(stream) == instance.window
  }

  private def collectWindow(stream: String): Unit = {
    logger.info(s"It's time to collect a window (stream: $stream).")
    val collectedWindow = currentWindowPerStream(stream)
    collectedWindowPerStream(stream) = collectedWindow.copy()
    performanceMetrics.addWindow(collectedWindow)
    slideCurrentWindow(stream)
  }

  private def allWindowsCollected: Boolean = {
    inputs.forall(stream => collectedWindowPerStream.isDefinedAt(stream))
  }

  private def slideCurrentWindow(stream: String): Unit = {
    deleteBatches(stream)
    resetCounter(stream)
  }

  private def deleteBatches(stream: String): Unit = {
    logger.debug(s"Delete batches from windows (for each stream) than shouldn't be repeated (from 0 to" +
      s" ${instance.slidingInterval}).")
    currentWindowPerStream(stream).batches.remove(0, instance.slidingInterval)
  }

  private def resetCounter(stream: String): Unit = {
    logger.debug(s"Reset a counter of batches for each window.")
    counterOfBatchesPerStream(stream) -= instance.slidingInterval
  }

  private def onWindow(): Unit = {
    logger.info(s"Windows have been collected (for streams: ${inputs.mkString(", ")}). Process them.")
    prepareCollectedWindows()
    executor.onWindow(windowRepository)
    commonBarrier.enter()
    val onEnterResult = Try(executor.onEnter())
    commonBarrier.leave()
    onEnterResult.get
    if (leaderLatch.hasLeadership()) executor.onLeaderEnter()
    retrievableStreams = inputs
    doCheckpoint()
  }

  private def prepareCollectedWindows(): Unit = {
    logger.debug(s"Fill a window repository for executor. Clear a collection with collected windows.")
    collectedWindowPerStream.foreach(x => {
      registerBatches(x._2)
      windowRepository.put(x._1, x._2)
    })

    collectedWindowPerStream.clear()
  }

  private def registerBatches(window: Window): Unit = {
    window.batches.slice(0, instance.slidingInterval)
      .foreach(x => x.envelopes.foreach(x => envelopeFetcher.registerEnvelope(x)))
  }

  /**
    * Does group checkpoint of t-streams consumers/producers
    */
  private def doCheckpoint(): Unit = {
    logger.info(s"It's time to checkpoint.")
    logger.debug(s"Invoke onBeforeCheckpoint() handler.")
    executor.onBeforeCheckpoint()
    logger.debug(s"Do group checkpoint.")
    moduleService.doCheckpoint()
    envelopeFetcher.doCheckpoint()
    logger.debug(s"Invoke onAfterCheckpoint() handler.")
  }
}