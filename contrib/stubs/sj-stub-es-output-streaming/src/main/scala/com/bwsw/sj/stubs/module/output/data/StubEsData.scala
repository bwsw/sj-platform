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
package com.bwsw.sj.stubs.module.output.data

import java.util.Date

import com.bwsw.sj.common.engine.core.entities.OutputEnvelope
import com.fasterxml.jackson.annotation.JsonProperty

/**
  * @author Kseniya Tomskikh
  */
class StubEsData(
                  @JsonProperty("test-date") val testDate: Date,
                  val value: Int = 0,
                  val stringValue: String = "")
  extends OutputEnvelope {

  override def getFieldsValue: Map[String, Any] = {
    Map("test-date" -> testDate, "string-value" -> stringValue, "value" -> value)
  }
}
