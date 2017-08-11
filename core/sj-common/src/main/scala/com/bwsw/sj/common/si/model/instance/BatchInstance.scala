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
package com.bwsw.sj.common.si.model.instance

import java.util.Date

import com.bwsw.sj.common.dal.model.instance.{BatchInstanceDomain, ExecutionPlan, FrameworkStage}
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.utils.StreamUtils.clearStreamFromMode
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import scaldi.Injector

import scala.collection.JavaConverters._

class BatchInstance(name: String,
                    description: String = RestLiterals.defaultDescription,
                    parallelism: Any = 1,
                    options: String = "{}",
                    perTaskCores: Double = 1,
                    perTaskRam: Int = 1024,
                    jvmOptions: Map[String, String] = Map(),
                    nodeAttributes: Map[String, String] = Map(),
                    coordinationService: String,
                    environmentVariables: Map[String, String] = Map(),
                    performanceReportingInterval: Long = 60000,
                    moduleName: String,
                    moduleVersion: String,
                    moduleType: String,
                    engine: String,
                    val inputs: Array[String],
                    outputs: Array[String],
                    val window: Int = 1,
                    val slidingInterval: Int = 1,
                    val startFrom: String = EngineLiterals.newestStartMode,
                    val stateManagement: String = EngineLiterals.noneStateMode,
                    val stateFullCheckpoint: Int = 100,
                    val eventWaitIdleTime: Long = 1000,
                    val executionPlan: ExecutionPlan = new ExecutionPlan(),
                    private val _restAddress: Option[String] = None,
                    stage: FrameworkStage = FrameworkStage(),
                    private val _status: String = EngineLiterals.ready,
                    frameworkId: String = System.currentTimeMillis().toString,
                    creationDate: String)
                   (implicit injector: Injector)
  extends Instance(
    name = name,
    description = description,
    parallelism = parallelism,
    options = options,
    perTaskCores = perTaskCores,
    perTaskRam = perTaskRam,
    jvmOptions = jvmOptions,
    nodeAttributes = nodeAttributes,
    coordinationService = coordinationService,
    environmentVariables = environmentVariables,
    performanceReportingInterval = performanceReportingInterval,
    moduleName = moduleName,
    moduleVersion = moduleVersion,
    moduleType = moduleType,
    engine = engine,
    restAddress = _restAddress,
    stage = stage,
    status = _status,
    frameworkId = frameworkId,
    outputs = outputs,
    creationDate = creationDate) {

  override def to: BatchInstanceDomain = {
    val serviceRepository = connectionRepository.getServiceRepository

    new BatchInstanceDomain(
      name = name,
      moduleType = moduleType,
      moduleName = moduleName,
      moduleVersion = moduleVersion,
      engine = engine,
      coordinationService = serviceRepository.get(coordinationService).get.asInstanceOf[ZKServiceDomain],
      status = status,
      restAddress = restAddress.getOrElse(RestLiterals.defaultRestAddress),
      description = description,
      parallelism = countParallelism,
      options = options,
      perTaskCores = perTaskCores,
      perTaskRam = perTaskRam,
      jvmOptions = jvmOptions.asJava,
      nodeAttributes = nodeAttributes.asJava,
      environmentVariables = environmentVariables.asJava,
      stage = stage,
      performanceReportingInterval = performanceReportingInterval,
      frameworkId = frameworkId,
      inputs = inputs,
      outputs = outputs,
      window = window,
      slidingInterval = slidingInterval,
      executionPlan = executionPlan,
      startFrom = startFrom,
      stateManagement = stateManagement,
      stateFullCheckpoint = stateFullCheckpoint,
      eventWaitIdleTime = eventWaitIdleTime,
      creationDate = new Date())
  }

  override protected def inputsOrEmptyList: Array[String] = inputs

  override def prepareInstance(): Unit =
    executionPlan.fillTasks(createTaskStreams(), createTaskNames(countParallelism, name))

  override def countParallelism: Int =
    castParallelismToNumber(getStreamsPartitions(streams))

  override def createStreams(): Unit =
    getStreams(streams).foreach(_.create())

  override def getInputsWithoutStreamMode: Array[String] = inputs.map(clearStreamFromMode)

  override val streams: Array[String] = getInputsWithoutStreamMode ++ outputs
}
