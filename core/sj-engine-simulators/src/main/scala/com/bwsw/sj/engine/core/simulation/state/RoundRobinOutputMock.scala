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

import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.engine.core.environment.RoundRobinOutput
import com.bwsw.tstreams.common.RoundRobinPartitionIterationPolicy

/**
  * Mock for [[com.bwsw.sj.common.engine.core.environment.RoundRobinOutput]]
  *
  * @param stream       output stream
  * @param senderThread mock of thread for sending data to the T-Streams service
  */
class RoundRobinOutputMock(stream: TStreamStreamDomain,
                           senderThread: TStreamsSenderThreadMock)
                          (implicit serialize: AnyRef => Array[Byte])
  extends RoundRobinOutput(stream.name, senderThread) {

  private val partitionPolicy = new RoundRobinPartitionIterationPolicy(
    stream.partitions,
    (0 until stream.partitions).toSet)

  /**
    * Stores data in [[TStreamsSenderThreadMock]]
    */
  override def put(data: AnyRef): Unit =
    senderThread.send(data, stream.name, partitionPolicy.getNextPartition())
}
