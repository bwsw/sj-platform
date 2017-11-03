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

import com.bwsw.sj.common.benchmark.BatchExecutorOptions
import com.bwsw.sj.common.dal.model.instance.BatchInstanceDomain
import com.bwsw.sj.common.utils.benchmark.ClassRunner
import com.bwsw.sj.engine.batch.BatchTaskRunner
import com.bwsw.sj.engine.core.testutils.Constants
import com.bwsw.sj.engine.core.testutils.benchmark.batch.BatchBenchmarkParameters
import com.bwsw.sj.engine.core.testutils.benchmark.sj.{InputStreamFactory, SjBenchmarkHelper}

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Provides methods for testing the speed of reading data from storage by Stream Juggler in batch mode
  *
  * @author Pavel Tomskikh
  */
class SjBatchBenchmarkHelper(zooKeeperAddress: String,
                             zkNamespace: String,
                             tStreamsPrefix: String,
                             tStreamsToken: String,
                             inputStreamFactory: InputStreamFactory,
                             runEmbeddedTTS: Boolean)
  extends SjBenchmarkHelper[BatchInstanceDomain](
    zooKeeperAddress,
    zkNamespace,
    tStreamsPrefix,
    tStreamsToken,
    BatchInstanceFactory,
    inputStreamFactory,
    "../../contrib/benchmarks/sj-batch-performance-benchmark/target/scala-2.12/" +
      s"sj-batch-performance-benchmark_2.12-${Constants.sjVersion}.jar",
    runEmbeddedTTS) {

  private var taskId = 0

  def runProcess(parameters: BatchBenchmarkParameters, messagesCount: Long, outputFilePath: String): Process = {
    updateInstance(
      outputFilePath,
      messagesCount,
      parameters.windowSize,
      parameters.slidingInterval,
      parameters.batchSize)

    new ClassRunner(classOf[BatchTaskRunner], environment = environment).start()
  }

  private def updateInstance(outputFilePath: String,
                             messagesCount: Long,
                             window: Int,
                             slidingInterval: Int,
                             batchSize: Int): Unit = {
    val instance = maybeInstance.get

    Try(connectionRepository.getInstanceRepository.delete(instanceName))

    val options = BatchExecutorOptions(outputFilePath, messagesCount, batchSize)

    val executionPlan = instance.executionPlan
    val task = executionPlan.tasks.get(taskName)
    taskId += 1
    taskName = instanceName + "-task" + taskId
    executionPlan.tasks = Map(taskName -> task).asJava
    environment += "TASK_NAME" -> taskName

    maybeInstance = Some(
      new BatchInstanceDomain(
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
        window = window,
        slidingInterval = slidingInterval,
        executionPlan = executionPlan,
        startFrom = instance.startFrom,
        stateManagement = instance.stateManagement,
        stateFullCheckpoint = instance.stateFullCheckpoint,
        eventWaitIdleTime = instance.eventWaitIdleTime,
        creationDate = instance.creationDate))

    connectionRepository.getInstanceRepository.save(maybeInstance.get)
  }
}
