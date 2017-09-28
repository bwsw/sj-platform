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
package com.bwsw.sj.common.engine.core.environment

import com.bwsw.common.file.utils.FileStorage
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.engine.core.environment.EnvironmentLiterals.{partitionedOutput, roundRobinOutput}
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetricsProxy
import com.bwsw.sj.common.engine.core.state.StateStorage
import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.tstreams.agents.producer.Producer

import scala.collection._

/**
  * Provides for user methods that can be used in [[com.bwsw.sj.common.utils.EngineLiterals.regularStreamingType]]
  * or [[com.bwsw.sj.common.utils.EngineLiterals.batchStreamingType]] module
  *
  * @param producers              t-streams producers for each output stream from instance
  *                               [[com.bwsw.sj.common.dal.model.instance.InstanceDomain.outputs]]
  * @param options                user defined options from instance
  *                               [[com.bwsw.sj.common.dal.model.instance.InstanceDomain.options]]
  * @param outputs                set of output streams [[com.bwsw.sj.common.dal.model.stream.StreamDomain]] from instance
  *                               [[com.bwsw.sj.common.dal.model.instance.InstanceDomain.outputs]]
  * @param producerPolicyByOutput keeps a tag (partitioned or round-robin output) corresponding to the output for each
  *                               output stream
  * @param moduleTimer            provides a possibility to set a timer inside a module
  * @param performanceMetrics     set of metrics that characterize performance of
  *                               [[com.bwsw.sj.common.utils.EngineLiterals.regularStreamingType]] or
  *                               [[com.bwsw.sj.common.utils.EngineLiterals.batchStreamingType]] module
  * @param fileStorage            file storage
  * @author Kseniya Mikhaleva
  */

class ModuleEnvironmentManager(options: String,
                               producers: Map[String, Producer],
                               outputs: Array[StreamDomain],
                               producerPolicyByOutput: mutable.Map[String, (String, ModuleOutput)],
                               moduleTimer: SjTimer,
                               performanceMetrics: PerformanceMetricsProxy,
                               fileStorage: FileStorage)
  extends EnvironmentManager(options, outputs, fileStorage) {

  /**
    * Allows getting partitioned output for specific output stream
    *
    * @param streamName Name of output stream
    * @return Partitioned output that wrapping output stream
    */
  def getPartitionedOutput(streamName: String)(implicit serialize: AnyRef => Array[Byte]): PartitionedOutput = {
    logger.info(s"Get partitioned output for stream: $streamName\n")
    producers.get(streamName) match {
      case Some(producer) =>
        producerPolicyByOutput.get(streamName) match {
          case Some((`partitionedOutput`, moduleOutput: PartitionedOutput)) =>
            moduleOutput

          case Some(_) =>
            logger.error(s"For output stream: $streamName partitioned output is set")
            throw new Exception(s"For output stream: $streamName partitioned output is set")

          case None =>
            producerPolicyByOutput(streamName) = (partitionedOutput, createPartitionedOutput(producer))

            producerPolicyByOutput(streamName)._2.asInstanceOf[PartitionedOutput]
        }

      case None =>
        logger.error(s"There is no output for name $streamName")
        throw new IllegalArgumentException(s"There is no output for name $streamName")

    }
  }

  /**
    * Allows getting round-robin output for specific output stream
    *
    * @param streamName Name of output stream
    * @return Round-robin output that wrapping output stream
    */
  def getRoundRobinOutput(streamName: String)(implicit serialize: AnyRef => Array[Byte]): RoundRobinOutput = {
    logger.info(s"Get round-robin output for stream: $streamName\n")
    producers.get(streamName) match {
      case Some(producer) =>
        producerPolicyByOutput.get(streamName) match {
          case Some((`roundRobinOutput`, moduleOutput: RoundRobinOutput)) =>
            moduleOutput

          case Some(_) =>
            logger.error(s"For output stream: $streamName partitioned output is set")
            throw new Exception(s"For output stream: $streamName partitioned output is set")

          case None =>
            producerPolicyByOutput(streamName) = (roundRobinOutput, createRoundRobinOutput(producer))

            producerPolicyByOutput(streamName)._2.asInstanceOf[RoundRobinOutput]
        }

      case None =>
        logger.error(s"There is no output for name $streamName")
        throw new IllegalArgumentException(s"There is no output for name $streamName")

    }
  }

  /**
    * Enables user to use a timer in a module which will be invoked the time handler: onTimer
    *
    * @param delay Time after which the handler will call (in milliseconds)
    */
  def setTimer(delay: Long): Unit = moduleTimer.set(delay)

  /**
    * Provides a default method for getting state of module. Must be overridden in stateful module
    *
    * @return Module state
    */
  def getState: StateStorage = {
    logger.error("Module has no state")
    throw new Exception("Module has no state")
  }

  protected def createPartitionedOutput(producer: Producer)
                                       (implicit serialize: AnyRef => Array[Byte]): PartitionedOutput =
    new PartitionedOutput(producer, performanceMetrics)

  protected def createRoundRobinOutput(producer: Producer)
                                      (implicit serialize: AnyRef => Array[Byte]): RoundRobinOutput =
    new RoundRobinOutput(producer, performanceMetrics)
}
