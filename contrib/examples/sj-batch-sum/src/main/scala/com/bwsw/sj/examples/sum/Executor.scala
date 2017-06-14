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
package com.bwsw.sj.examples.sum

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.entities.KafkaEnvelope
import com.bwsw.sj.engine.core.environment.ModuleEnvironmentManager
import com.bwsw.sj.engine.core.batch.{WindowRepository, BatchStreamingExecutor}


class Executor(manager: ModuleEnvironmentManager) extends BatchStreamingExecutor[Array[Byte]](manager) {
  val objectSerializer = new ObjectSerializer()

  override def onWindow(windowRepository: WindowRepository): Unit = {
    //val outputs = manager.getStreamsByTags(Array("output"))
    //val output = manager.getRoundRobinOutput(outputs(new Random().nextInt(outputs.length)))
    val t0 = System.currentTimeMillis()
    val allWindows = windowRepository.getAll()

    val envelopes = allWindows.flatMap(_._2.batches).flatMap(_.envelopes).map(_.asInstanceOf[KafkaEnvelope[Array[Byte]]])
    val numbers = envelopes.map(x => {
      objectSerializer.deserialize(x.data).asInstanceOf[Int]
    })
    val t1 = System.currentTimeMillis()

    println("sum = " + numbers.sum + ", count = " + numbers.size +
      ", firstMessageTs = " + envelopes.head.id + ", lastMessageTs = " + envelopes.last.id +
      ", processingStartTs = " + t0 + ", processingEndTs = " + t1 +
      ". Elapsed time: " + (t1 - t0) + "ms")
  }
}