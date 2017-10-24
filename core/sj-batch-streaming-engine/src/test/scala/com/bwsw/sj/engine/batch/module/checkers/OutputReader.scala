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

import com.bwsw.sj.common.engine.core.entities.{Batch, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.batch.module.DataFactory.createOutputConsumer
import com.bwsw.sj.engine.batch.module.SjBatchModuleBenchmarkConstants.{outputCount, partitions}
import com.bwsw.sj.engine.core.testutils.checkers.{Reader, TStreamsReader}

/**
  * Returns a data from output stream
  *
  * @author Pavel Tomskikh
  */
class OutputReader extends Reader[Int] {

  private val reader =
    new TStreamsReader[Batch]((1 to outputCount).map(x => createOutputConsumer(partitions, x.toString)))

  /**
    * Returns a data from output stream
    *
    * @return a data from output stream
    */
  override def get(): Seq[Int] = {
    reader.get().flatMap(_.envelopes)
      .flatMap {
        case tStreamEnvelope: TStreamEnvelope[Int@unchecked] => tStreamEnvelope.data.toList
        case kafkaEnvelope: KafkaEnvelope[Int@unchecked] => Seq(kafkaEnvelope.data)
      }
  }
}
