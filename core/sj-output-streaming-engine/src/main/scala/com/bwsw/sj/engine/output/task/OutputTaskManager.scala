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
package com.bwsw.sj.engine.output.task

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.si.model.instance.OutputInstance
import com.bwsw.sj.common.engine.core.environment.{EnvironmentManager, OutputEnvironmentManager}
import com.bwsw.sj.common.engine.core.managment.TaskManager
import com.bwsw.sj.common.engine.core.output.OutputStreamingExecutor
import scaldi.Injector

import scala.collection.mutable

/**
  * Class allows to manage an environment of output streaming task
  *
  * @author Kseniya Tomskikh
  */
class OutputTaskManager(implicit injector: Injector) extends TaskManager {
  val outputInstance: OutputInstance = instance.asInstanceOf[OutputInstance]
  val inputs: mutable.Map[StreamDomain, Array[Int]] = getInputs(outputInstance.executionPlan)

  require(numberOfAgentsPorts >= 1, "Not enough ports for t-stream consumers. One or more ports are required")

  def getExecutor(environmentManager: EnvironmentManager): OutputStreamingExecutor[AnyRef] = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar.")
    val executor = executorClass.getConstructor(classOf[OutputEnvironmentManager])
      .newInstance(environmentManager)
      .asInstanceOf[OutputStreamingExecutor[AnyRef]]
    logger.debug(s"Task: $taskName. Create an instance of executor class.")

    executor
  }
}
