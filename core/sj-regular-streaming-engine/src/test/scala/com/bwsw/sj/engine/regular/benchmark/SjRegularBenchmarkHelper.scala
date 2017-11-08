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
package com.bwsw.sj.engine.regular.benchmark

import com.bwsw.sj.common.benchmark.RegularExecutorOptions
import com.bwsw.sj.common.dal.model.instance.RegularInstanceDomain
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.core.testutils.Constants
import com.bwsw.sj.engine.core.testutils.benchmark.sj.{InputStreamFactory, SjBenchmarkHelper}
import com.bwsw.sj.engine.regular.RegularTaskRunner

import scala.util.Try

/**
  * Provides methods for testing the speed of reading data from storage by Stream Juggler in regular mode
  *
  * @author Pavel Tomskikh
  */
class SjRegularBenchmarkHelper(zooKeeperAddress: String,
                               zkNamespace: String,
                               tStreamsPrefix: String,
                               tStreamsToken: String,
                               inputStreamFactory: InputStreamFactory,
                               runEmbeddedTTS: Boolean)
  extends SjBenchmarkHelper[RegularInstanceDomain](
    zooKeeperAddress,
    zkNamespace,
    tStreamsPrefix,
    tStreamsToken,
    RegularInstanceFactory,
    inputStreamFactory,
    "../../contrib/benchmarks/sj-regular-performance-benchmark/target/scala-2.12/" +
      s"sj-regular-performance-benchmark_2.12-${Constants.sjVersion}.jar",
    runEmbeddedTTS) {

  def runProcess(outputFilePath: String, messagesCount: Long): Process = {
    updateInstance(outputFilePath, messagesCount)

    new ClassRunner(classOf[RegularTaskRunner], environment = environment).start()
  }

  private def updateInstance(outputFilePath: String, messagesCount: Long): Unit = {
    val instance = maybeInstance.get
    Try(connectionRepository.getInstanceRepository.delete(instanceName))
    val options = RegularExecutorOptions(outputFilePath, messagesCount)

    maybeInstance = Some(
      new RegularInstanceDomain(
        name = instance.name,
        moduleType = instance.moduleType,
        moduleName = instance.moduleName,
        moduleVersion = instance.moduleVersion,
        engine = instance.engine,
        coordinationService = instance.coordinationService,
        status = instance.status,
        restAddress = instance.restAddress,
        description = instance.description,
        parallelism = instance.parallelism,
        options = jsonSerializer.serialize(options),
        perTaskCores = instance.perTaskCores,
        perTaskRam = instance.perTaskRam,
        jvmOptions = instance.jvmOptions,
        nodeAttributes = instance.nodeAttributes,
        environmentVariables = instance.environmentVariables,
        stage = instance.stage,
        performanceReportingInterval = instance.performanceReportingInterval,
        frameworkId = instance.frameworkId,
        inputs = instance.inputs,
        outputs = instance.outputs,
        checkpointMode = instance.checkpointMode,
        checkpointInterval = instance.checkpointInterval,
        executionPlan = instance.executionPlan,
        startFrom = instance.startFrom,
        stateManagement = instance.stateManagement,
        stateFullCheckpoint = instance.stateFullCheckpoint,
        eventWaitIdleTime = instance.eventWaitIdleTime,
        creationDate = instance.creationDate))

    connectionRepository.getInstanceRepository.save(maybeInstance.get)
  }
}
