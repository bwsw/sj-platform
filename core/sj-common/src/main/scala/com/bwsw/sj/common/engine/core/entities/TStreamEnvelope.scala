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
package com.bwsw.sj.common.engine.core.entities

import com.bwsw.sj.common.utils.StreamLiterals
import com.fasterxml.jackson.annotation.JsonIgnore

import scala.collection.mutable

/**
  * Provides a wrapper for t-stream transaction.
  *
  * @param consumerName name of consumer that obtains messages from t-stream
  *                     (used for saving stream offset to recover after failure)
  * @param data         message data
  * @tparam T type of data containing in a message
  */

class TStreamEnvelope[T <: AnyRef](var data: mutable.Queue[T], var consumerName: String) extends Envelope {
  streamType = StreamLiterals.tstreamType

  @JsonIgnore
  override def equals(obj: Any): Boolean = obj match {
    case t: TStreamEnvelope[_] =>
      data.toList == t.data.toList &&
        consumerName == t.consumerName &&
        streamType == t.streamType &&
        id == t.id &&
        stream == t.stream &&
        (tags sameElements t.tags) &&
        partition == t.partition

    case _ => super.equals(obj)
  }
}







