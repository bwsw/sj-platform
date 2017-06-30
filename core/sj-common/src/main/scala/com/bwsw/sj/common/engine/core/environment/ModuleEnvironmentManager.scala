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

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.common.engine.core.state.StateStorage
import com.bwsw.sj.common.utils.{EngineLiterals, SjTimer}
import com.bwsw.tstreams.agents.producer.Producer

import scala.collection._

/**
  * Provides for user methods that can be used in [[EngineLiterals.regularStreamingType]]
  * or [[EngineLiterals.batchStreamingType]] module
  *
  * @param producers              t-streams producers for each output stream from instance [[InstanceDomain.outputs]]
  * @param options                user defined options from instance [[InstanceDomain.options]]
  * @param outputs                set of output streams [[StreamDomain]] from instance [[InstanceDomain.outputs]]
  * @param producerPolicyByOutput keeps a tag (partitioned or round-robin output) corresponding to the output for each
  *                               output stream
  * @param moduleTimer            provides a possibility to set a timer inside a module
  * @param performanceMetrics     set of metrics that characterize performance of [[EngineLiterals.regularStreamingType]]
  *                               or [[EngineLiterals.batchStreamingType]] module
  * @author Kseniya Mikhaleva
  */

class ModuleEnvironmentManager(options: String,
                               producers: Map[String, Producer],
                               outputs: Array[StreamDomain],
                               producerPolicyByOutput: mutable.Map[String, (String, ModuleOutput)],
                               moduleTimer: SjTimer,
                               performanceMetrics: PerformanceMetrics)
  extends EnvironmentManager(options, outputs) {

  /**
    * Allows getting partitioned output for specific output stream
    *
    * @param streamName Name of output stream
    * @return Partitioned output that wrapping output stream
    */
  def getPartitionedOutput(streamName: String)(implicit serialize: AnyRef => Array[Byte]): PartitionedOutput = {
    logger.info(s"Get partitioned output for stream: $streamName\n")
    if (producers.contains(streamName)) {
      if (producerPolicyByOutput.contains(streamName)) {
        if (producerPolicyByOutput(streamName)._1 == "partitioned") {
          producerPolicyByOutput(streamName)._2.asInstanceOf[PartitionedOutput]
        }
        else {
          logger.error(s"For output stream: $streamName partitioned output is set")
          throw new Exception(s"For output stream: $streamName partitioned output is set")
        }
      } else {
        producerPolicyByOutput(streamName) = ("partitioned",
          new PartitionedOutput(producers(streamName), performanceMetrics))

        producerPolicyByOutput(streamName)._2.asInstanceOf[PartitionedOutput]
      }
    } else {
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
    if (producers.contains(streamName)) {
      if (producerPolicyByOutput.contains(streamName)) {
        if (producerPolicyByOutput(streamName)._1 == "round-robin") {
          producerPolicyByOutput(streamName)._2.asInstanceOf[RoundRobinOutput]
        }
        else {
          logger.error(s"For output stream: $streamName partitioned output is set")
          throw new Exception(s"For output stream: $streamName partitioned output is set")
        }
      } else {
        producerPolicyByOutput(streamName) = ("round-robin",
          new RoundRobinOutput(producers(streamName), performanceMetrics))

        producerPolicyByOutput(streamName)._2.asInstanceOf[RoundRobinOutput]
      }
    } else {
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
}


