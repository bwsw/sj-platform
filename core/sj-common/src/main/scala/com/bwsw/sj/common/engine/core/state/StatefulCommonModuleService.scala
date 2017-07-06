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
package com.bwsw.sj.common.engine.core.state

import com.bwsw.sj.common.dal.model.stream.{StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.engine.core.environment.StatefulModuleEnvironmentManager
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.common.engine.{StateHandlers, StreamingExecutor, TimerHandlers}
import com.bwsw.sj.common.si.model.instance.{BatchInstance, RegularInstance}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.Producer
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Class is in charge of creating [[StatefulModuleEnvironmentManager]] (and executor [[StreamingExecutor]]
  * with [[StateHandlers]]) and saving of state
  *
  * @param manager            manager of environment of task of [[EngineLiterals.regularStreamingType]]
  *                           or [[EngineLiterals.batchStreamingType]] module
  * @param checkpointGroup    group of t-stream agents that have to make a checkpoint at the same time
  * @param performanceMetrics set of metrics that characterize performance of [[EngineLiterals.regularStreamingType]]
  *                           or [[EngineLiterals.batchStreamingType]] module
  */
class StatefulCommonModuleService(manager: CommonTaskManager,
                                  checkpointGroup: CheckpointGroup,
                                  performanceMetrics: PerformanceMetrics)
                                 (implicit injector: Injector)
  extends CommonModuleService(manager.instance, manager.outputProducers, checkpointGroup) {

  private val streamService: GenericMongoRepository[StreamDomain] = inject[ConnectionRepository].getStreamRepository
  private var countOfCheckpoints: Int = 1
  private val stateService: RAMStateService = createStateService()
  private val stateFullCheckpoint: Int = {
    instance match {
      case regularInstance: RegularInstance => regularInstance.stateFullCheckpoint
      case batchInstance: BatchInstance => batchInstance.stateFullCheckpoint
    }
  }

  private def createStateService() = {
    val stateStream = createStateStream()
    val stateProducer = createStateProducer(stateStream)
    val stateConsumer = createStateConsumer(stateStream)
    val stateLoader = new StateLoader(stateConsumer)
    val stateSaver = new StateSaver(stateProducer)

    new RAMStateService(stateSaver, stateLoader)
  }

  val environmentManager = new StatefulModuleEnvironmentManager(
    new StateStorage(stateService),
    instance.options,
    outputProducers,
    instance.outputs.flatMap(x => streamService.get(x)),
    producerPolicyByOutput,
    moduleTimer,
    performanceMetrics)

  val executor: StreamingExecutor with TimerHandlers = manager.getExecutor(environmentManager).asInstanceOf[StreamingExecutor with TimerHandlers]

  private val statefulExecutor: StateHandlers = executor.asInstanceOf[StateHandlers]

  /**
    * Does group checkpoint of t-streams state consumers/producers
    */
  override def doCheckpoint(): Unit = {
    if (countOfCheckpoints != stateFullCheckpoint) {
      doCheckpointOfPartOfState()
    } else {
      doCheckpointOfFullState()
    }
    super.doCheckpoint()
  }

  /**
    * Saves a partial state changes
    */
  private def doCheckpointOfPartOfState(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of a part of state.")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler.")
    statefulExecutor.onBeforeStateSave(false)
    stateService.savePartialState()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler.")
    countOfCheckpoints += 1
  }

  /**
    * Saves a state
    */
  private def doCheckpointOfFullState(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of full state.")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler.")
    statefulExecutor.onBeforeStateSave(true)
    stateService.saveFullState()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler.")
    countOfCheckpoints = 1
  }

  /**
    * Creates [[TStreamStreamDomain]] to keep a module state
    */
  private def createStateStream(): TStreamStreamDomain = {
    logger.debug(s"Task name: ${manager.taskName} " +
      s"Get stream for keeping state of module.")

    val description = "store state of module"
    val tags = Array("state")
    val partitions = 1
    val stateStreamName = manager.taskName + "_state"

    manager.createStorageStream(stateStreamName, description, partitions)
    manager.getStream(stateStreamName, description, tags, partitions)
  }

  /**
    * Create a state producer and adds it to checkpoint group
    */
  private def createStateProducer(stateStream: TStreamStreamDomain): Producer = {
    val stateProducer = manager.createProducer(stateStream)
    checkpointGroup.add(stateProducer)

    stateProducer
  }

  /**
    * Create a state consumer and adds it to checkpoint group
    */
  private def createStateConsumer(stateStream: TStreamStreamDomain): Consumer = {
    val partition = 0
    val stateConsumer = manager.createConsumer(stateStream, List(partition, partition), Oldest)
    stateConsumer.start()

    checkpointGroup.add(stateConsumer)

    stateConsumer
  }
}