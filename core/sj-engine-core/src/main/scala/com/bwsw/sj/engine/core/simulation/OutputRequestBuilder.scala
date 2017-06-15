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
package com.bwsw.sj.engine.core.simulation

import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.Entity

/**
  * Provides method for building request for output service from [[OutputEnvelope]].
  *
  * @tparam T type of data elements in [[Entity]]
  * @author Pavel Tomskikh
  */
trait OutputRequestBuilder[T] {

  protected val transactionFieldName: String = "txn"

  /**
    * Builds request for output service
    *
    * @param outputEnvelope envelope that outgoing from
    *                       [[com.bwsw.sj.engine.core.output.OutputStreamingExecutor OutputStreamingExecutor]]
    * @param inputEnvelope  envelope that incoming to
    *                       [[com.bwsw.sj.engine.core.output.OutputStreamingExecutor OutputStreamingExecutor]]
    * @param outputEntity   working entity gotten from
    *                       [[com.bwsw.sj.engine.core.output.OutputStreamingExecutor OutputStreamingExecutor]]
    * @return constructed request
    */
  def build(outputEnvelope: OutputEnvelope, inputEnvelope: TStreamEnvelope[_], outputEntity: Entity[T]): String
}
