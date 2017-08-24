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
package com.bwsw.sj.engine.regular.module

/**
  * @author Pavel Tomskikh
  */
object SjRegularBenchmarkConstants {
  val inputCount = 2
  val outputCount = 2
  val partitions = 4
  val defaultValueOfTxns = 4
  val defaultValueOfElements = 4
  val checkpointInterval = 2
  val stateFullCheckpoint = 2

  val kafkaMode = "kafka"
  val tstreamMode = "tstream"
  val commonMode = "both"
  val inputStreamsType = commonMode

  val modulePath = "../../contrib/stubs/sj-stub-regular-streaming/target/scala-2.12/sj-stub-regular-streaming-1.0-SNAPSHOT.jar"
}
