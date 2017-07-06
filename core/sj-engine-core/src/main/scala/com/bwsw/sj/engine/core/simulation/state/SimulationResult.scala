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

/**
  * Contains data elements for each output stream and a state at a certain point in time
  *
  * @param streamDataList list of data elements that written in output streams
  * @param state          key/map value
  */
case class SimulationResult(streamDataList: Seq[StreamData], state: Map[String, Any])

/**
  * Contains data elements that has been sent in an output stream
  *
  * @param stream            stream name
  * @param partitionDataList list of data that elements written in partitions of that stream
  */
case class StreamData(stream: String, partitionDataList: Seq[PartitionData])

/**
  * Contains data elements that has been sent in a partition of output stream
  *
  * @param partition partition index
  * @param dataList  data elements
  */
case class PartitionData(partition: Int, dataList: Seq[AnyRef] = Seq.empty) {
  def +(data: AnyRef): PartitionData =
    PartitionData(partition, dataList :+ data)
}
