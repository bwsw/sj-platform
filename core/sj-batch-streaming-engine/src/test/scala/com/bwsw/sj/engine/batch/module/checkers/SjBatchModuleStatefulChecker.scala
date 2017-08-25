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
package com.bwsw.sj.engine.batch.module.checkers

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.engine.core.entities.{Batch, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.common.utils.benchmark.BenchmarkUtils
import com.bwsw.sj.engine.batch.module.DataFactory._
import com.bwsw.sj.engine.batch.module.SjBatchModuleBenchmarkConstants._
import com.bwsw.sj.engine.batch.utils.StateHelper

import scala.collection.JavaConverters._

object SjBatchModuleStatefulChecker extends App {
  BenchmarkUtils.exitAfter { () =>
    val streamService = connectionRepository.getStreamRepository
    val objectSerializer: ObjectSerializer = new ObjectSerializer()

    val inputTstreamConsumers = (1 to inputCount).map(x => createInputTstreamConsumer(partitions, x.toString))
    val inputKafkaConsumer = createInputKafkaConsumer(inputCount, partitions)
    val outputConsumers = (1 to outputCount).map(x => createOutputConsumer(partitions, x.toString))

    inputTstreamConsumers.foreach(x => x.start())
    outputConsumers.foreach(x => x.start())

    var totalInputElements = 0
    var totalOutputElements = 0

    var inputElements = scala.collection.mutable.ArrayBuffer[Int]()
    var outputElements = scala.collection.mutable.ArrayBuffer[Int]()

    val consumer = createStateConsumer(streamService)
    consumer.start()
    val initialState = StateHelper.getState(consumer, objectSerializer)
    var sum = initialState("sum").asInstanceOf[Int]

    inputTstreamConsumers.foreach(inputTstreamConsumer => {
      val partitions = inputTstreamConsumer.getPartitions.toIterator

      while (partitions.hasNext) {
        val currentPartition = partitions.next
        var maybeTxn = inputTstreamConsumer.getTransaction(currentPartition)
        while (maybeTxn.isDefined) {
          val transaction = maybeTxn.get
          while (transaction.hasNext) {
            val element = objectSerializer.deserialize(transaction.next).asInstanceOf[Int]
            inputElements.+=(element)
            totalInputElements += 1
          }
          maybeTxn = inputTstreamConsumer.getTransaction(currentPartition)
        }
      }
    })

    var records = inputKafkaConsumer.poll(1000 * 20)
    records.asScala.foreach(x => {
      val bytes = x.value()
      val element = objectSerializer.deserialize(bytes).asInstanceOf[Int]
      inputElements.+=(element)
      totalInputElements += 1
    })

    outputConsumers.foreach(outputConsumer => {
      val partitions = outputConsumer.getPartitions.toIterator

      while (partitions.hasNext) {
        val currentPartition = partitions.next
        var maybeTxn = outputConsumer.getTransaction(currentPartition)

        while (maybeTxn.isDefined) {
          val transaction = maybeTxn.get
          while (transaction.hasNext) {
            val batch = objectSerializer.deserialize(transaction.next).asInstanceOf[Batch]
            batch.envelopes.foreach {
              case tstreamEnvelope: TStreamEnvelope[Int@unchecked] => tstreamEnvelope.data.foreach(x => {
                outputElements.+=(x)
                totalOutputElements += 1
              })
              case kafkaEnvelope: KafkaEnvelope[Int@unchecked] =>
                outputElements.+=(kafkaEnvelope.data)
                totalOutputElements += 1
            }
          }
          maybeTxn = outputConsumer.getTransaction(currentPartition)
        }
      }
    })

    assert(totalInputElements == totalOutputElements,
      "Count of all txns elements that are consumed from output stream should equals count of all txns elements that are consumed from input stream")

    assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
      "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

    assert(sum == inputElements.sum,
      "Sum of all txns elements that are consumed from input stream should equals state variable sum")

    consumer.stop()
    inputTstreamConsumers.foreach(x => x.stop())
    outputConsumers.foreach(x => x.stop())
    connectionRepository.close()

    println("DONE")
  }
}

class SjBatchModuleStatefulChecker
