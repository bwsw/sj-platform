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
package com.bwsw.sj.engine.input

import com.bwsw.sj.engine.input.DataFactory._

import scala.util.{Failure, Success, Try}

object DuplicateChecker extends App {

  val exitCode = Try {
    val streamService = connectionRepository.getStreamRepository

    val outputConsumers = (1 to outputCount).map(x => createOutputConsumer(streamService, x.toString))
    outputConsumers.foreach(_.start())

    val totalInputElements = args(0).toInt * outputCount
    val totalDuplicates = args(1).toInt * outputCount
    var totalOutputElements = 0

    outputConsumers.foreach(outputConsumer => {
      val partitions = outputConsumer.getPartitions.toIterator

      while (partitions.hasNext) {
        val currentPartition = partitions.next
        var maybeTxn = outputConsumer.getTransaction(currentPartition)

        while (maybeTxn.isDefined) {
          val transaction = maybeTxn.get
          while (transaction.hasNext) {
            transaction.next()
            totalOutputElements += 1
          }
          maybeTxn = outputConsumer.getTransaction(currentPartition)
        }
      }
    })

    assert(totalOutputElements == totalInputElements - totalDuplicates,
      s"Count of all txns elements that are consumed from output stream($totalOutputElements) should equals count of the elements that are consumed from input socket excluding the duplicates(${totalInputElements - totalDuplicates})")

    outputConsumers.foreach(_.stop())
    connectionRepository.close()
  } match {
    case Success(_) =>
      println("DONE")
      0

    case Failure(e) =>
      e.printStackTrace()
      1
  }

  System.exit(exitCode)
}
