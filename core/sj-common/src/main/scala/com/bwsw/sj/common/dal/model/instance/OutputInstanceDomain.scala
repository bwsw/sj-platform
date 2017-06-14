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
package com.bwsw.sj.common.dal.model.instance

import java.util

import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import com.bwsw.sj.common.utils.StreamUtils._
import org.mongodb.morphia.annotations.{Embedded, Property}

/**
  * Domain entity for [[EngineLiterals.outputStreamingType]] instance
  *
  * @author Kseniya Tomskikh
  */
class OutputInstanceDomain(override val name: String,
                           override val moduleType: String,
                           override val moduleName: String,
                           override val moduleVersion: String,
                           override val engine: String,
                           override val coordinationService: ZKServiceDomain,
                           override val status: String = EngineLiterals.ready,
                           override val restAddress: String = "",
                           override val description: String = RestLiterals.defaultDescription,
                           override val parallelism: Int = 1,
                           override val options: String = "{}",
                           override val perTaskCores: Double = 0.1,
                           override val perTaskRam: Int = 32,
                           override val jvmOptions: java.util.Map[String, String] = new util.HashMap[String, String](),
                           override val nodeAttributes: java.util.Map[String, String] = new util.HashMap[String, String](),
                           override val environmentVariables: java.util.Map[String, String] = new util.HashMap[String, String](),
                           override val stage: FrameworkStage = FrameworkStage(),
                           override val performanceReportingInterval: Long = 60000,
                           override val frameworkId: String = System.currentTimeMillis().toString,
                           var inputs: Array[String] = Array(),
                           override val outputs: Array[String] = Array(),
                           @PropertyField("checkpoint-mode") val checkpointMode: String,
                           @Property("checkpoint-interval") var checkpointInterval: Long = 0,
                           @Embedded("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan(),
                           @Property("start-from") var startFrom: String = "newest",
                           @Property("input-avro-schema") var inputAvroSchema: String = "{}")
  extends InstanceDomain(
    name,
    moduleType,
    moduleName,
    moduleVersion,
    engine,
    coordinationService,
    status,
    restAddress,
    description,
    outputs,
    parallelism,
    options,
    perTaskCores,
    perTaskRam,
    jvmOptions,
    nodeAttributes,
    environmentVariables,
    stage,
    performanceReportingInterval,
    frameworkId) {

  override def getInputsWithoutStreamMode: Array[String] = inputs.map(clearStreamFromMode)
}
