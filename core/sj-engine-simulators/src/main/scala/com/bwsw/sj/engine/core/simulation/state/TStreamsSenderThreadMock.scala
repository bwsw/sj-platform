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

import com.bwsw.sj.common.engine.core.environment.TStreamsSenderThread
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import org.mockito.Mockito.mock

import scala.collection.mutable

/**
  * Mock for [[TStreamsSenderThread]]
  *
  * @param streams set of output stream names
  * @author Pavel Tomskikh
  */
class TStreamsSenderThreadMock(streams: Set[String])
  extends TStreamsSenderThread(
    Map.empty,
    mock(classOf[PerformanceMetrics]),
    "TStreamsSenderThreadMock") {

  private val streamDataList: mutable.Map[String, StreamData] = mutable.Map.empty

  def send(data: AnyRef, stream: String, partition: Int): Unit = {
    val streamData = streamDataList.getOrElse(stream, StreamData(stream, Seq.empty))

    streamDataList += (stream -> (streamData + PartitionData(partition, Seq(data))))
  }

  def getStreamDataList: Seq[StreamData] =
    streamDataList.values.filter(_.partitionDataList.nonEmpty).toSeq

  override def prepareToCheckpoint(): Unit = streamDataList.clear()


  override def run(): Unit = {}

  override def send(bytes: Array[Byte], stream: String, partition: Int): Unit = {}
}
