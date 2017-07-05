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
package com.bwsw.sj.engine.core.simulation.state

import com.bwsw.sj.common.engine.core.environment.{PartitionedOutput, RoundRobinOutput}
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.tstreams.agents.producer.Producer

import scala.collection.mutable

/**
  * Provides buffer for storing output elements
  *
  * @author Pavel Tomskikh
  */
trait ModuleOutputMockHelper {
  protected var outputElements: mutable.Buffer[OutputElement] = mutable.Buffer.empty

  /**
    * Returns output elements
    */
  def getOutputElements: mutable.Buffer[OutputElement] =
    outputElements

  /**
    * Removes all output elements
    */
  def clear(): Unit =
    outputElements = mutable.Buffer.empty
}

/**
  * Mock for [[PartitionedOutput]]
  *
  * @param producer           producer of specific output
  * @param performanceMetrics set of metrics that characterize performance of
  *                           [[com.bwsw.sj.common.utils.EngineLiterals.regularStreamingType]] or
  *                           [[com.bwsw.sj.common.utils.EngineLiterals.batchStreamingType]] module
  */
class PartitionedOutputMock(producer: Producer,
                            performanceMetrics: PerformanceMetrics)
                           (implicit serialize: AnyRef => Array[Byte])
  extends PartitionedOutput(producer, performanceMetrics) with ModuleOutputMockHelper {

  /**
    * Stores data in [[outputElements]]
    */
  override def put(data: AnyRef, partition: Int): Unit = {
    if (partition >= 0 && partition < producer.stream.partitionsCount)
      outputElements += OutputElement(data, partition)
    else
      throw new IllegalArgumentException(s"'partition' must be non-negative and less that count of partitions in this " +
        s"output stream (partition = $partition, count of partitions = ${producer.stream.partitionsCount})")
  }

  override def clear(): Unit = super.clear()
}

/**
  * Mock for [[RoundRobinOutput]]
  *
  * @param producer           producer of specific output
  * @param performanceMetrics set of metrics that characterize performance of
  *                           [[com.bwsw.sj.common.utils.EngineLiterals.regularStreamingType]] or
  *                           [[com.bwsw.sj.common.utils.EngineLiterals.batchStreamingType]] module
  */
class RoundRobinOutputMock(producer: Producer,
                           performanceMetrics: PerformanceMetrics)
                          (implicit serialize: AnyRef => Array[Byte])
  extends RoundRobinOutput(producer, performanceMetrics) with ModuleOutputMockHelper {

  private var currentPartition: Int = 0

  /**
    * Stores data in [[outputElements]]
    */
  override def put(data: AnyRef): Unit = {
    outputElements += OutputElement(data, currentPartition)
    currentPartition += 1
    if (currentPartition == producer.stream.partitionsCount)
      currentPartition = 0
  }

  override def clear(): Unit = super.clear()
}

/**
  * Contains info about output
  */
case class OutputElement(data: AnyRef, partition: Int)