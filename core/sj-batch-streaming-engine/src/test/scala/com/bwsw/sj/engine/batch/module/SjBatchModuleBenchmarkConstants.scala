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
package com.bwsw.sj.engine.batch.module

import com.bwsw.sj.engine.core.testutils.Constants

/**
  * @author Pavel Tomskikh
  */
object SjBatchModuleBenchmarkConstants {
  val stateFullCheckpoint = 3
  val window = 2
  val slidingInterval = 1
  val defaultValueOfTxns = 12
  val defaultValueOfElements = 1
  val inputCount = 2
  val outputCount = 2
  val partitions = 4

  val kafkaMode = "kafka"
  val tStreamMode = "tstream"
  val commonMode = "both"

  val modulePath = s"../../contrib/stubs/sj-stub-batch-streaming/target/scala-2.12/" +
    s"sj-stub-batch-streaming_2.12-${Constants.sjVersion}.jar"
}
