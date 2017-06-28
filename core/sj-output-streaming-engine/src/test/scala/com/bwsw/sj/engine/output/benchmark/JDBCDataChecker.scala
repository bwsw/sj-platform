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
package com.bwsw.sj.engine.output.benchmark

import com.bwsw.sj.common.dal.model.stream.{JDBCStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import com.bwsw.sj.engine.output.benchmark.DataFactory._

import scala.collection.mutable.ArrayBuffer

object JDBCDataChecker extends App {

  val streamService: GenericMongoRepository[StreamDomain] = connectionRepository.getStreamRepository
  val tStream: TStreamStreamDomain = streamService.get(tstreamInputName).get.asInstanceOf[TStreamStreamDomain]
  val inputConsumer = createConsumer(tStream)
  inputConsumer.start()

  val inputElements = new ArrayBuffer[(Int, String)]()
  val partitions = inputConsumer.getPartitions.toIterator

  while (partitions.hasNext) {
    val currentPartition = partitions.next
    var maybeTxn = inputConsumer.getTransaction(currentPartition)
    while (maybeTxn.isDefined) {
      val transaction = maybeTxn.get
      while (transaction.hasNext) {
        val element = objectSerializer.deserialize(transaction.next).asInstanceOf[(Int, String)]
        inputElements.append(element)
      }
      maybeTxn = inputConsumer.getTransaction(currentPartition)
    }
  }

  val jdbcStream: JDBCStreamDomain = streamService.get(jdbcStreamName).get.asInstanceOf[JDBCStreamDomain]

  val jdbcClient = openJdbcConnection(jdbcStream)
  jdbcClient.start()

  val stmt = jdbcClient.createPreparedStatement(s"SELECT COUNT(1) FROM ${jdbcStream.name}")
  var jdbcOutputDataSize = 0
  val res = stmt.executeQuery()
  while (res.next()) {
    jdbcOutputDataSize = res.getInt(1)
  }

  assert(inputElements.size == jdbcOutputDataSize,
    s"Count of all txns elements that are consumed from output stream ($jdbcOutputDataSize) " +
      s"should equals count of all txns elements that are consumed from input stream (${inputElements.size})")

  connectionRepository.close()
  inputConsumer.stop()
  jdbcClient.close()

  println("DONE")
}

