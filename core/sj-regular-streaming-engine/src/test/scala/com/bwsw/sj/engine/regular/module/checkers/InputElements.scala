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
package com.bwsw.sj.engine.regular.module.checkers

import com.bwsw.sj.engine.regular.module.checkers.elements_readers.{KafkaInputElementsReader, TStreamInputElementsReader}

trait InputElements {
  def getInputElements(): Seq[Int]
}

trait InputElementsBothType extends InputElements {
  override def getInputElements(): Seq[Int] =
    TStreamInputElementsReader.getInputElements() ++ KafkaInputElementsReader.getInputElements()
}

trait InputElementsKafka extends InputElements {
  override def getInputElements(): Seq[Int] =
    KafkaInputElementsReader.getInputElements()
}

trait InputElementsTStream extends InputElements {
  override def getInputElements(): Seq[Int] =
    TStreamInputElementsReader.getInputElements()
}
