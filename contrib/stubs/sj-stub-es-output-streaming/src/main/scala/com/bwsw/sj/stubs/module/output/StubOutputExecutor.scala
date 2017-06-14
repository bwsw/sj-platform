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

import java.util.Calendar

import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor
import com.bwsw.sj.engine.core.output.types.es._
import com.bwsw.sj.stubs.module.output.data.StubEsData

/**
  * Handler for work with t-stream envelopes
  * Executor trait for output-streaming module
  *
  * @author Kseniya Tomskikh
  */
class StubOutputExecutor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor[(Integer, String)](manager) {

  /**
    * Transform t-stream transaction to output entities
    *
    * @param envelope Input T-Stream envelope
    * @return List of output envelopes
    */
  override def onMessage(envelope: TStreamEnvelope[(Integer, String)]): Seq[OutputEnvelope] = {
    println("Processed: " + envelope.data.size + " elements")

    val list = envelope.data.dequeueAll(_ => true).map {
      case (i, s) => new StubEsData(Calendar.getInstance().getTime, i, s)
    }

    list
  }

  override def getOutputEntity = {
    val entityBuilder = new ElasticsearchEntityBuilder()
    val entity = entityBuilder
      .field(new DateField("test-date"))
      .field(new IntegerField("value"))
      .field(new JavaStringField("string-value"))
      .build()
    entity
  }
}
