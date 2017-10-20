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
package com.bwsw.sj.engine.output.benchmark.data_checkers

import com.bwsw.sj.common.dal.model.stream.{StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import com.bwsw.sj.common.utils.benchmark.ProcessTerminator
import com.bwsw.sj.engine.output.benchmark.DataFactory.{connectionRepository, createConsumer, objectSerializer, tstreamInputName}

import scala.collection.mutable.ArrayBuffer

/**
  * Validates that data in output storage corresponds to data in input storage
  *
  * @author Pavel Tomskikh
  */
trait DataChecker extends App {
  ProcessTerminator.terminateProcessAfter { () =>
    val inputElements = getInputElements()
    val outputElements = getOutputElements()

    assert(inputElements.size == outputElements.size,
      s"Count of all txns elements that are consumed from output stream (${outputElements.size}) " +
        s"should equals count of all txns elements that are consumed from input stream (${inputElements.size})")

    assert(inputElements.toList.sorted == outputElements.toList.sorted,
      "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

    connectionRepository.close()
  }

  /**
    * Returns a data from input storage
    *
    * @return a data from input storage
    */
  def getInputElements(): Seq[(Int, String)] = {
    val streamService: GenericMongoRepository[StreamDomain] = connectionRepository.getStreamRepository
    val tStream: TStreamStreamDomain = streamService.get(tstreamInputName).get.asInstanceOf[TStreamStreamDomain]
    val inputConsumer = createConsumer(tStream)
    inputConsumer.start()

    val inputElements = ArrayBuffer.empty[(Int, String)]
    inputConsumer.getPartitions.foreach { partition =>
      var maybeTxn = inputConsumer.getTransaction(partition)
      while (maybeTxn.isDefined) {
        inputElements ++= maybeTxn.get.getAll.toList
          .map(bytes => objectSerializer.deserialize(bytes).asInstanceOf[(Int, String)])

        maybeTxn = inputConsumer.getTransaction(partition)
      }
    }
    inputConsumer.stop()

    inputElements
  }

  /**
    * Returns a data from output storage
    *
    * @return a data from output storage
    */
  def getOutputElements(): Seq[(Int, String)]
}
