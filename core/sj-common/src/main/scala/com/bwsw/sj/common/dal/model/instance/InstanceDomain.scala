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
import java.util.Date

import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{EmbeddedField, IdField, PropertyField}
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import org.mongodb.morphia.annotations.Entity

/**
  * Domain entity for instance
  *
  * @author Kseniya Tomskikh
  */
@Entity("instances")
class InstanceDomain(@IdField val name: String,
                     @PropertyField("module-type") val moduleType: String,
                     @PropertyField("module-name") val moduleName: String,
                     @PropertyField("module-version") val moduleVersion: String,
                     val engine: String,
                     @EmbeddedField("coordination-service") val coordinationService: ZKServiceDomain,
                     val status: String = EngineLiterals.ready,
                     @PropertyField("rest-address") val restAddress: String = RestLiterals.defaultRestAddress,
                     val description: String = RestLiterals.defaultDescription,
                     val outputs: Array[String] = Array(),
                     val parallelism: Int = 1,
                     val options: String = "{}",
                     @PropertyField("per-task-cores") val perTaskCores: Double = 0.1,
                     @PropertyField("per-task-ram") val perTaskRam: Int = 32,
                     @PropertyField("jvm-options") val jvmOptions: java.util.Map[String, String] = new util.HashMap[String, String](),
                     @PropertyField("node-attributes") val nodeAttributes: java.util.Map[String, String] = new util.HashMap[String, String](),
                     @PropertyField("environment-variables") val environmentVariables: java.util.Map[String, String] = new util.HashMap[String, String](),
                     val stage: FrameworkStage = FrameworkStage(),
                     @PropertyField("performance-reporting-interval") val performanceReportingInterval: Long = 60000,
                     @PropertyField("framework-id") val frameworkId: String = System.currentTimeMillis().toString,
                     val creationDate: Date) {

  def getInputsWithoutStreamMode: Array[String] = Array()
}
