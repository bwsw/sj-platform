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

import com.bwsw.sj.common.utils.benchmark.BenchmarkUtils
import com.bwsw.sj.engine.batch.module.DataFactory.connectionRepository
import com.bwsw.sj.engine.batch.module.checkers.elements_readers.{OutputElementsReader, StateReader}

abstract class SjBatchModuleStatefulChecker extends App {
  BenchmarkUtils.exitAfter { () =>
    val inputElements = getInputElements()
    val outputElements = OutputElementsReader.getOutputElements()

    val sum = StateReader.getStateSum(connectionRepository)
    connectionRepository.close()

    assert(inputElements.length == outputElements.length,
      s"Count of all txns elements that are consumed from output stream (${outputElements.length}) " +
        s"should equals count of all txns elements that are consumed from input stream (${inputElements.length})")

    assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
      "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

    assert(sum == inputElements.sum,
      "Sum of all txns elements that are consumed from input stream should equals state variable sum")

    println("DONE")
  }

  def getInputElements(): Seq[Int]
}


object SjBatchModuleStatefulBothChecker extends SjBatchModuleStatefulChecker with InputElementsBothType

class SjBatchModuleStatefulBothChecker


object SjBatchModuleStatefulTstreamChecker extends SjBatchModuleStatefulChecker with InputElementsTStream

class SjBatchModuleStatefulTstreamChecker


object SjBatchModuleStatefulKafkaChecker extends SjBatchModuleStatefulChecker with InputElementsKafka

class SjBatchModuleStatefulKafkaChecker
