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
package com.bwsw.sj.engine.core.output

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager

/**
  *
  * It is responsible for output module execution logic.
  * Module uses a specific instance to configure its work.
  * Executor provides the following methods, which don't do anything by default so you should define their implementation by yourself.
  *
  * @author Kseniya Tomskikh
  */
abstract class OutputStreamingExecutor[T <: AnyRef](manager: OutputEnvironmentManager) extends StreamingExecutor {
  /**
    * it is invoked for every received message from one of the inputs that are defined within the instance.
    * Inside the method you have an access to the message that has the TStreamEnvelope type.
    * By extension a t-stream envelope should be transformed to output envelopes.
    *
    */
  def onMessage(envelope: TStreamEnvelope[T]): Seq[OutputEnvelope] = {
    List()
  }

  /** *
    * This method return current working entity.
    * Must be implemented.
    * For example:
    * {{{
    *   val entityBuilder = new ConcreteEntityBuilder()
    *   val entity = entityBuilder
    *     .field(new IntegerField("id"))
    *     .field(new JavaStringField("name"))
    *     .build()
    *   return entity
    * }}}
    *
    * @return Current working Entity.
    */
  def getOutputEntity: Entity[_]
}
