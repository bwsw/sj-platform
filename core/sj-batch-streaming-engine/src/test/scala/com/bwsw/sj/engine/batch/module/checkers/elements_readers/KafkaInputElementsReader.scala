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
package com.bwsw.sj.engine.batch.module.checkers.elements_readers

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.batch.module.DataFactory.createInputKafkaConsumer
import com.bwsw.sj.engine.batch.module.SjBatchModuleBenchmarkConstants.{inputCount, partitions}

import scala.collection.JavaConverters._

object KafkaInputElementsReader extends InputElementsReader {

  def getInputElements(): Seq[Int] = {
    val objectSerializer = new ObjectSerializer()
    val inputKafkaConsumer = createInputKafkaConsumer(inputCount, partitions)

    inputKafkaConsumer.poll(1000 * 20).asScala.toList
      .map(_.value())
      .map(bytes => objectSerializer.deserialize(bytes).asInstanceOf[Int])
  }
}