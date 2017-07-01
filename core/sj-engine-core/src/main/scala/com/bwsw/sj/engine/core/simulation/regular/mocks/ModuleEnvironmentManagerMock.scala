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
package com.bwsw.sj.engine.core.simulation.regular.mocks

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.engine.core.environment.{ModuleEnvironmentManager, ModuleOutput}
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.tstreams.agents.producer.Producer

import scala.collection.{Map, mutable}

/**
  * Mock for [[com.bwsw.sj.common.engine.core.environment.ModuleEnvironmentManager]]. Creates [[PartitionedOutputMock]]
  * instead [[com.bwsw.sj.common.engine.core.environment.PartitionedOutput]] and [[RoundRobinOutputMock]] instead
  * [[com.bwsw.sj.common.engine.core.environment.RoundRobinOutput]].
  *
  * @inheritdoc
  * @author Pavel Tomskikh
  */
class ModuleEnvironmentManagerMock(options: String,
                                   producers: Map[String, Producer],
                                   outputs: Array[StreamDomain],
                                   val producerPolicyByOutput: mutable.Map[String, (String, ModuleOutput)],
                                   moduleTimer: SjTimer,
                                   performanceMetrics: PerformanceMetrics)
  extends ModuleEnvironmentManager(
    options, producers, outputs, producerPolicyByOutput, moduleTimer, performanceMetrics) {

  override def getPartitionedOutput(streamName: String)
                                   (implicit serialize: (AnyRef) => Array[Byte]): PartitionedOutputMock =
    super.getPartitionedOutput(streamName).asInstanceOf[PartitionedOutputMock]

  override def getRoundRobinOutput(streamName: String)
                                  (implicit serialize: (AnyRef) => Array[Byte]): RoundRobinOutputMock =
    super.getRoundRobinOutput(streamName).asInstanceOf[RoundRobinOutputMock]

  override protected def createPartitionedOutput(producer: Producer)
                                                (implicit serialize: AnyRef => Array[Byte]): PartitionedOutputMock =
    new PartitionedOutputMock(producer, performanceMetrics)

  override protected def createRoundRobinOutput(producer: Producer)
                                               (implicit serialize: AnyRef => Array[Byte]): RoundRobinOutputMock =
    new RoundRobinOutputMock(producer, performanceMetrics)
}
