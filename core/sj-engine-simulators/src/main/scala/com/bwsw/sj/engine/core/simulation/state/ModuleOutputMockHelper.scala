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
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetricsProxy
import com.bwsw.tstreams.agents.producer.Producer
import com.bwsw.tstreams.common.RoundRobinPartitionIterationPolicy

import scala.collection.mutable

/**
  * Provides buffer for storing output elements
  *
  * @author Pavel Tomskikh
  */
trait ModuleOutputMockHelper {
  protected var partitionDataList: mutable.Map[Int, PartitionData] = mutable.Map.empty

  /**
    * Returns partitions with data
    */
  def getPartitionDataList: Seq[PartitionData] =
    partitionDataList.values.toList.sortBy(_.partition)

  /**
    * Removes data from partitions
    */
  def clear(): Unit =
    partitionDataList.clear()

  protected def append(data: AnyRef, partition: Int): Unit = {
    val partitionData = partitionDataList.getOrElse(partition, PartitionData(partition))
    partitionDataList += (partition -> (partitionData + data))
  }
}

/**
  * Mock for [[com.bwsw.sj.common.engine.core.environment.PartitionedOutput]]
  *
  * @param producer           producer of specific output
  * @param performanceMetrics set of metrics that characterize performance of
  *                           [[com.bwsw.sj.common.utils.EngineLiterals.regularStreamingType]] or
  *                           [[com.bwsw.sj.common.utils.EngineLiterals.batchStreamingType]] module
  */
class PartitionedOutputMock(producer: Producer,
                            performanceMetrics: PerformanceMetricsProxy)
                           (implicit serialize: AnyRef => Array[Byte])
  extends PartitionedOutput(producer, performanceMetrics) with ModuleOutputMockHelper {

  /**
    * Stores data in [[partitionDataList]]
    */
  override def put(data: AnyRef, partition: Int): Unit = {
    if (partition >= 0 && partition < producer.stream.partitionsCount)
      append(data, partition)
    else
      throw new IllegalArgumentException(s"'partition' must be non-negative and less that count of partitions in this " +
        s"output stream (partition = $partition, count of partitions = ${producer.stream.partitionsCount})")
  }

  override def clear(): Unit =
    super[ModuleOutputMockHelper].clear()
}

/**
  * Mock for [[com.bwsw.sj.common.engine.core.environment.RoundRobinOutput]]
  *
  * @param producer           producer of specific output
  * @param performanceMetrics set of metrics that characterize performance of
  *                           [[com.bwsw.sj.common.utils.EngineLiterals.regularStreamingType]] or
  *                           [[com.bwsw.sj.common.utils.EngineLiterals.batchStreamingType]] module
  */
class RoundRobinOutputMock(producer: Producer,
                           performanceMetrics: PerformanceMetricsProxy)
                          (implicit serialize: AnyRef => Array[Byte])
  extends RoundRobinOutput(producer, performanceMetrics) with ModuleOutputMockHelper {

  private val partitionPolicy = new RoundRobinPartitionIterationPolicy(
    producer.stream.partitionsCount,
    (0 until producer.stream.partitionsCount).toSet)

  /**
    * Stores data in [[partitionDataList]]
    */
  override def put(data: AnyRef): Unit =
    append(data, partitionPolicy.getNextPartition())

  override def clear(): Unit =
    super[ModuleOutputMockHelper].clear()
}
