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

import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, FrameworkStage, OutputInstanceDomain}
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.utils.StreamUtils.clearStreamFromMode
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}
import scaldi.Injector

import scala.collection.JavaConverters._

class OutputInstance(name: String,
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
                     val checkpointMode: String,
                     val checkpointInterval: Long,
                     val input: String,
                     val output: String,
                     val startFrom: String = EngineLiterals.newestStartMode,
                     val executionPlan: ExecutionPlan = new ExecutionPlan(),
                     restAddress: Option[String] = None,
                     stage: FrameworkStage = FrameworkStage(),
                     private val _status: String = EngineLiterals.ready,
                     frameworkId: String = System.currentTimeMillis().toString)
                    (implicit injector: Injector)
  extends Instance(
    name,
    description,
    parallelism,
    options,
    perTaskCores,
    perTaskRam,
    jvmOptions,
    nodeAttributes,
    coordinationService,
    environmentVariables,
    performanceReportingInterval,
    moduleName,
    moduleVersion,
    moduleType,
    engine,
    restAddress,
    stage,
    _status,
    frameworkId,
    Array(output)) {

  override def to: OutputInstanceDomain = {
    val serviceRepository = connectionRepository.getServiceRepository

    new OutputInstanceDomain(
      name,
      moduleType,
      moduleName,
      moduleVersion,
      engine,
      serviceRepository.get(coordinationService).get.asInstanceOf[ZKServiceDomain],
      status,
      restAddress.getOrElse(RestLiterals.defaultRestAddress),
      description,
      countParallelism,
      options,
      perTaskCores,
      perTaskRam,
      jvmOptions.asJava,
      nodeAttributes.asJava,
      environmentVariables.asJava,
      stage,
      performanceReportingInterval,
      frameworkId,
      Array(input),
      Array(output),
      checkpointMode,
      checkpointInterval,
      executionPlan,
      startFrom)
  }

  override def inputsOrEmptyList: Array[String] = Array(input)

  override def prepareInstance(): Unit =
    executionPlan.fillTasks(createTaskStreams(), createTaskNames(countParallelism, name))

  override def countParallelism: Int =
    castParallelismToNumber(getStreamsPartitions(streams))

  override def createStreams(): Unit =
    getStreams(Array(input)).foreach(_.create())

  override def getInputsWithoutStreamMode: Array[String] = {
    Option(input) match {
      case Some(_input) => Array(clearStreamFromMode(_input))
      case None => super.getInputsWithoutStreamMode
    }
  }

  override val streams: Array[String] = getInputsWithoutStreamMode ++ outputs
}
