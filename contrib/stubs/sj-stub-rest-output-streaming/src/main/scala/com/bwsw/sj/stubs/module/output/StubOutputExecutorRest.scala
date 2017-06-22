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
package com.bwsw.sj.stubs.module.output

import com.bwsw.sj.common.engine.core.entities.TStreamEnvelope
import com.bwsw.sj.common.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.common.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.core.output.types.rest.{RestEntityBuilder, RestField}
import com.bwsw.sj.stubs.module.output.data.StubRestData

/**
  * @author Pavel Tomskikh
  */
class StubOutputExecutorRest(manager: OutputEnvironmentManager)
  extends OutputStreamingExecutor[(Integer, String)](manager) {

  override def onMessage(envelope: TStreamEnvelope[(Integer, String)]) = {
    println("Processed: " + envelope.data.size + " elements")

    val list = envelope.data.dequeueAll(_ => true).map {
      case (i, s) =>
        val data = new StubRestData
        data.value = i
        data.stringValue = s

        data
    }

    list
  }

  override def getOutputEntity = {
    val entityBuilder = new RestEntityBuilder()
    val entity = entityBuilder
      .field(new RestField("value"))
      .field(new RestField("stringValue"))
      .build()

    entity
  }
}
