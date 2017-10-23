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
package com.bwsw.sj.engine.core.testutils.checkers

import com.bwsw.common.ObjectSerializer
import com.bwsw.tstreams.agents.consumer.Consumer

import scala.collection.mutable.ArrayBuffer

/**
  * Returns a data from a T-Streams
  *
  * @author Pavel Tomskikh
  */
class TStreamsReader[+T](consumers: Seq[Consumer]) extends Reader[T] {

  /**
    * Returns a data from a T-Streams
    *
    * @return a data from a T-Streams
    */
  def get(): Seq[T] = {
    val objectSerializer = new ObjectSerializer()

    val inputElements = ArrayBuffer.empty[T]

    consumers.foreach { consumer =>
      consumer.start()
      val partitions = consumer.getPartitions

      partitions.foreach { partition =>
        var maybeTxn = consumer.getTransaction(partition)

        while (maybeTxn.isDefined) {
          inputElements ++= maybeTxn.get.getAll.toList
            .map(bytes => objectSerializer.deserialize(bytes).asInstanceOf[T])

          maybeTxn = consumer.getTransaction(partition)
        }
      }

      consumer.stop()
    }

    inputElements
  }
}