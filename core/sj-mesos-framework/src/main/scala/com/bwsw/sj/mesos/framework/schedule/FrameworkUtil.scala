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
package com.bwsw.sj.mesos.framework.schedule

import java.io.{PrintWriter, StringWriter}
import java.net.URI

import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.utils.{CommonAppConfigNames, FrameworkLiterals}
import com.bwsw.sj.mesos.framework.task.TasksList
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.log4j.Logger
import org.apache.mesos.Protos.MasterInfo
import org.apache.mesos.SchedulerDriver
import scaldi.Injectable.inject

import scala.collection.immutable
import scala.util.Try

/**
  * Contains several common functions to control framework
  */
object FrameworkUtil {

  import com.bwsw.sj.common.SjModule._

  // Java threads created from JNI code in a non-java thread have null ContextClassloader unless the creator explicitly sets it.
  // Also in such context Thread.currentThread().getContextClassLoader() returns null.
  private val classLoader = ClassLoader.getSystemClassLoader
  Thread.currentThread().setContextClassLoader(classLoader)

  var master: Option[MasterInfo] = None
  var frameworkId: Option[String] = None
  var driver: Option[SchedulerDriver] = None
  var jarName: Option[String] = None
  var instance: Option[Instance] = None
  var instancePort: Option[Int] = None
  var config: Option[Config] = None
  lazy val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  lazy val configRepository: GenericMongoRepository[ConfigurationSettingDomain] = connectionRepository.getConfigRepository
  private val logger = Logger.getLogger(this.getClass)

  /**
    * Returns count of ports needed for current task.
    *
    * @param instance current launched task
    * @return count of ports for current task
    */
  def getCountPorts(instance: Instance): Int = {
    instance match {
      case _: OutputInstance => 2
      case regularInstance: RegularInstance => regularInstance.inputs.length + regularInstance.outputs.length + 4
      case inputInstance: InputInstance => inputInstance.outputs.length + 2
      case batchInstance: BatchInstance => batchInstance.inputs.length + batchInstance.outputs.length + 4
    }
  }

  /**
    * Handles Scheduler Exception
    */
  def handleSchedulerException(e: Exception, logger: Logger): Unit = {
    val sw = new StringWriter
    e.printStackTrace(new PrintWriter(sw))
    TasksList.setMessage(e.getMessage)
    logger.error(s"Framework error: ${sw.toString}")
    driver.foreach(_.stop())
    System.exit(1)
  }


  /**
    * Returns jar URI for framework
    *
    * @param instance :Instance
    * @return String
    */
  def getModuleUrl(instance: Instance): String = {
    jarName = configRepository.get(ConfigLiterals.systemDomain + "." + instance.engine).map(_.value)
    val restHost = configRepository.get(ConfigLiterals.hostOfCrudRestTag).get.value
    val restPort = configRepository.get(ConfigLiterals.portOfCrudRestTag).get.value.toInt
    val restAddress = new URI(s"http://$restHost:$restPort/v1/custom/jars/${jarName.get}").toString
    logger.debug(s"Engine downloading URL: $restAddress.")
    restAddress
  }

  /**
    * Check if instance started
    * @return Boolean
    */
  private def isInstanceStarted: Boolean = {
    updateInstance()
    instance.exists(_.status == "started")
  }

  /**
    * Killing all launched tasks when it needed
    */
  def killAllLaunchedTasks(): Unit = {
    TasksList.getLaunchedTasks.foreach(taskId => {
      TasksList.killTask(taskId)
    })
  }

  /**
    * Teardowns framework, needed to be executed if instance did not started.
    */
  def teardown(): Unit = {
    logger.info(s"Kill all launched tasks: ${TasksList.getLaunchedTasks}")
    killAllLaunchedTasks()
  }


  def selectingNotLaunchedTasks(): Unit = {
    TasksList.getList.foreach(task => {
      if (!TasksList.getLaunchedTasks.contains(task.id)) TasksList.addToLaunch(task.id)
    })
    logger.info(s"Selecting tasks to launch: ${TasksList.toLaunch}")
  }

  /**
    * Fetch instance info from database
    * @return
    */
  def updateInstance(): Any = {
    val optionInstance = connectionRepository.getInstanceRepository.get(FrameworkParameters(FrameworkParameters.instanceId))
      .map(inject[InstanceCreator].from)

    if (optionInstance.isEmpty) {
      logger.error(s"Not found instance")
      TasksList.setMessage("Framework shut down: not found instance.")
      driver.foreach(_.stop())
    } else {
      FrameworkUtil.instance = optionInstance
    }
  }

  def getJvmOptions: String = {
    instance.get.jvmOptions.foldLeft("")((acc, option) => s"$acc ${option._1}${option._2}")
  }

  /**
    * Launch tasks if instance started, else teardown
    */
  def checkInstanceStarted(): Unit = {
    logger.info(s"Check is instance status 'started': $isInstanceStarted")
    if (isInstanceStarted) selectingNotLaunchedTasks()
    else teardown()
  }
}
