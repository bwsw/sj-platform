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
package com.bwsw.sj.engine.input.task

import com.bwsw.sj.common.si.model.instance.InputInstance
import com.bwsw.sj.common.engine.core.environment.{EnvironmentManager, InputEnvironmentManager}
import com.bwsw.sj.common.engine.core.input.InputStreamingExecutor
import com.bwsw.sj.common.engine.core.managment.TaskManager
import com.bwsw.sj.engine.input.config.InputEngineConfigNames
import com.bwsw.tstreams.agents.producer.Producer
import com.typesafe.config.ConfigFactory
import scaldi.Injector

import scala.util.Try

/**
  * Class allows to manage an environment of input streaming task
  *
  * @author Kseniya Mikhaleva
  */
class InputTaskManager(implicit injector: Injector) extends TaskManager {

  lazy val inputs: Nothing = {
    logger.error(s"Instance of Input module hasn't got execution plan " +
      s"and it's impossible to retrieve inputs.")
    throw new Exception(s"Instance of Input module hasn't got execution plan " +
      s"and it's impossible to retrieve inputs.")
  }

  val inputInstance: InputInstance = instance.asInstanceOf[InputInstance]
  val entryPort: Int = getEntryPort()
  val outputProducers: Map[String, Producer] = createOutputProducers()

  def getEntryPort(): Int = {
    val config = ConfigFactory.load()
    Try(config.getInt(InputEngineConfigNames.entryPort))
      .getOrElse(inputInstance.tasks(taskName).port)
  }

  def getExecutor(environmentManager: EnvironmentManager): InputStreamingExecutor[AnyRef] = {
    logger.debug(s"Task: $taskName. Start loading an executor class from module jar.")
    val executor = executorClass.getConstructor(classOf[InputEnvironmentManager])
      .newInstance(environmentManager)
      .asInstanceOf[InputStreamingExecutor[AnyRef]]
    logger.debug(s"Task: $taskName. Load an executor class.")

    executor
  }
}