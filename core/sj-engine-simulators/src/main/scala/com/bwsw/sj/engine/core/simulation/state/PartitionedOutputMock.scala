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
import com.bwsw.sj.common.engine.core.environment.PartitionedOutput

/**
  * Mock for [[com.bwsw.sj.common.engine.core.environment.PartitionedOutput]]
  *
  * @param stream       output stream
  * @param senderThread mock of thread for sending data to the T-Streams service
  */
class PartitionedOutputMock(stream: TStreamStreamDomain,
                            senderThread: TStreamsSenderThreadMock)
                           (implicit serialize: AnyRef => Array[Byte])
  extends PartitionedOutput(stream.name, senderThread) {

  /**
    * Stores data in [[TStreamsSenderThreadMock]]
    */
  override def put(data: AnyRef, partition: Int): Unit = {
    if (partition >= 0 && partition < stream.partitions)
      senderThread.send(data, stream.name, partition)
    else
      throw new IllegalArgumentException(s"'partition' must be non-negative and less that count of partitions in this " +
        s"output stream (partition = $partition, count of partitions = ${stream.partitions})")
  }
}
