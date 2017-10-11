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
package com.bwsw.sj.engine.batch.benchmark.read_kafka.sj

import com.bwsw.sj.common.dal.model.module.IOstream
import com.bwsw.sj.common.si.model.module.BatchSpecification
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonProperty
import scaldi.Injector

class BatchSpecificationApi(val name: String,
                            val description: String,
                            val version: String,
                            val author: String,
                            val license: String,
                            val inputs: IOstream,
                            val outputs: IOstream,
                            @JsonProperty("engine-name") val engineName: String,
                            @JsonProperty("engine-version") val engineVersion: String,
                            @JsonProperty("validator-class") val validatorClass: String,
                            @JsonProperty("executor-class") val executorClass: String,
                            @JsonProperty("batch-collector-class") val batchCollectorClass: String,
                            @JsonProperty("module-type") val moduleType: String = EngineLiterals.batchStreamingType) {

  def to(implicit injector: Injector): BatchSpecification = new BatchSpecification(
    name,
    description,
    version,
    author,
    license,
    inputs,
    outputs,
    engineName,
    engineVersion,
    validatorClass,
    executorClass,
    batchCollectorClass)
}
