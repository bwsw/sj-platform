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
package com.bwsw.sj.engine.regular.module.checkers.elements_readers

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.regular.module.DataFactory._
import com.bwsw.sj.engine.regular.module.SjRegularBenchmarkConstants._

import scala.collection.mutable.ArrayBuffer

/**
  * @author Pavel Tomskikh
  */
object OutputElementsReader {

  def getOutputElements(): ArrayBuffer[Int] = {
    val objectSerializer = new ObjectSerializer()

    val consumers = (1 to outputCount).map(x => createOutputConsumer(partitions, x.toString))
    val outputElements = ArrayBuffer.empty[Int]

    consumers.foreach { consumer =>
      consumer.start()

      consumer.getPartitions.foreach { partition =>
        var maybeTxn = consumer.getTransaction(partition)

        while (maybeTxn.isDefined) {
          outputElements ++= maybeTxn.get.getAll.toList
            .map(bytes => objectSerializer.deserialize(bytes).asInstanceOf[Int])

          maybeTxn = consumer.getTransaction(partition)
        }
      }

      consumer.stop()
    }

    outputElements
  }
}
