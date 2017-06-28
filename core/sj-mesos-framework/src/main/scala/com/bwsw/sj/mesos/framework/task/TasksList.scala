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
package com.bwsw.sj.mesos.framework.task

import com.bwsw.sj.common.dal.model.instance.InputTask
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.FrameworkRestEntity
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.mesos.framework.schedule.{FrameworkUtil, OffersHandler}
import org.apache.log4j.Logger
import org.apache.mesos.Protos.{TaskID, TaskInfo, _}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.{Failure, Success, Try}

object TasksList {
  /***
    * availablePorts - ports that can be used for current task. This parameter update after new offer come.
    */

  private val logger = Logger.getLogger(this.getClass)
  private val tasksToLaunch = mutable.ListBuffer[String]()
  private val listTasks = mutable.Map[String, Task]()
  private var message: String = "Initialization"
  private val availablePorts = collection.mutable.ListBuffer[Long]()
  private var launchedOffers = Map[OfferID, ArrayBuffer[TaskInfo]]()

  private var launchedTasks = mutable.ListBuffer[String]()

  var perTaskCores: Double = 0.0
  var perTaskMem: Double = 0.0
  var perTaskPortsCount: Int = 0

  def newTask(taskId: String) = {
    val task = new Task(taskId)
    listTasks += taskId -> task
//    tasksToLaunch += taskId
  }

  def getList: Iterable[Task] = {
    listTasks.values
  }

  def getTask(taskId: String): Task = {
    listTasks(taskId)
  }

  def addToLaunch(taskId: String) = {
    tasksToLaunch += taskId
  }

  def toLaunch: mutable.ListBuffer[String] = {
    tasksToLaunch
  }

  def launched(taskId: String): ListBuffer[String] = {
    tasksToLaunch -= taskId
    launchedTasks += taskId
  }

  def stopped(taskId: String): ListBuffer[String] = {
    launchedTasks -= taskId
  }

  def killTask(taskId: String): ListBuffer[String] = {
    FrameworkUtil.driver.get.killTask(TaskID.newBuilder().setValue(taskId).build)
    stopped(taskId)
  }

  def clearLaunchedTasks(): Unit = {
    launchedTasks = mutable.ListBuffer[String]()
  }

  def toFrameworkTask: FrameworkRestEntity = {
    FrameworkRestEntity(listTasks.values.map(_.toFrameworkTask).toSeq)
  }

  def apply(taskId: String): Option[Task] = {
    listTasks.get(taskId)
  }

  def clearAvailablePorts(): Unit = {
    availablePorts.remove(0, availablePorts.length)
  }

  def getAvailablePorts: ListBuffer[Long] = {
    availablePorts
  }

  def count: Int = {
    toLaunch.size
  }

  def getLaunchedOffers: Map[OfferID, ArrayBuffer[TaskInfo]] = {
    launchedOffers
  }

  def getLaunchedTasks: ListBuffer[String] = {
    launchedTasks
  }

  def clearLaunchedOffers(): Unit = {
    launchedOffers = Map[OfferID, ArrayBuffer[TaskInfo]]()
  }

  def setMessage(message: String): Unit = {
    this.message = message
  }

  def prepare(instance: Instance): Unit = {
    perTaskCores = FrameworkUtil.instance.get.perTaskCores
    perTaskMem = FrameworkUtil.instance.get.perTaskRam
    perTaskPortsCount = FrameworkUtil.getCountPorts(FrameworkUtil.instance.get)

    val tasks = FrameworkUtil.instance.get.moduleType match {
      case EngineLiterals.inputStreamingType =>
        (0 until FrameworkUtil.instance.get.countParallelism).map(tn => FrameworkUtil.instance.get.name + "-task" + tn)
      case _ =>
        val executionPlan = FrameworkUtil.instance.get match {
          case regularInstance: RegularInstance => regularInstance.executionPlan
          case outputInstance: OutputInstance => outputInstance.executionPlan
          case batchInstance: BatchInstance => batchInstance.executionPlan
        }
        executionPlan.tasks.asScala.keys
    }
    tasks.foreach(task => newTask(task))
  }

  def createTaskToLaunch(task: String, offer: Offer)(implicit injector: Injector): TaskInfo = {
    TaskInfo.newBuilder
      .setCommand(getCommand(task, offer))
      .setName(task)
      .setTaskId(TaskID.newBuilder.setValue(task).build())
      .addResources(OffersHandler.getCpusResource)
      .addResources(OffersHandler.getMemResource)
      .addResources(OffersHandler.getPortsResource(offer, task))
      .setSlaveId(offer.getSlaveId)
      .build()
  }

  def getCommand(task: String, offer: Offer)(implicit injector: Injector): CommandInfo = {
    def getAgentPorts: String = {
      var agentPorts: String = ""
      var taskPort: String = ""
      val ports = OffersHandler.getPortsResource(offer, task)

      var availablePorts = ports.getRanges.getRangeList.asScala.map(_.getBegin.toString)

    val host = OffersHandler.getOfferIp(offer)
    TasksList(task).foreach(task => task.update(host=host))
    if (FrameworkUtil.instance.get.moduleType.equals(EngineLiterals.inputStreamingType)) {
      taskPort = availablePorts.head
      availablePorts = availablePorts.tail
      val inputInstance = FrameworkUtil.instance.get.asInstanceOf[InputInstance]
      inputInstance.tasks.put(task, new InputTask(host, taskPort.toInt))
      inject[ConnectionRepository].getInstanceRepository.save(FrameworkUtil.instance.get.to)
    }

      agentPorts = availablePorts.mkString(",")
      agentPorts.dropRight(1)
    }

//    logger.debug(s"Task: $task. Ports for task: ${availablePorts.mkString(",")}.")
//
//    val cmd = CommandInfo.newBuilder()
//    val hosts: collection.mutable.ListBuffer[String] = collection.mutable.ListBuffer()
//    TasksList.toLaunch.foreach(task =>
//      if (TasksList.getTask(task).host.nonEmpty) hosts.append(TasksList.getTask(task).host.get)
//    )
//
//    var environmentVariables = List(
////      Environment.Variable.newBuilder.setName("MONGO_HOSTS").setValue(FrameworkUtil.params {"mongodbHosts"}),
//      Environment.Variable.newBuilder.setName("INSTANCE_NAME").setValue(FrameworkUtil.params {"instanceId"}),
//      Environment.Variable.newBuilder.setName("TASK_NAME").setValue(task),
//      Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(OffersHandler.getOfferIp(offer)),
//      Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(agentPorts),
//      Environment.Variable.newBuilder.setName("INSTANCE_HOSTS").setValue(hosts.mkString(","))
//    )
//    ConnectionConstants.mongoEnvironment.foreach(variable =>
//      environmentVariables = environmentVariables :+ Environment.Variable.newBuilder.setName(variable._1).setValue(variable._2)
//    )
//
//    Try {
//      val environments = Environment.newBuilder
//      environmentVariables.foreach(variable => environments.addVariables(variable))
//
//      val jvmOptions = FrameworkUtil.instance.get.jvmOptions.asScala
//        .foldLeft("")((acc, option) => s"$acc ${option._1}${option._2}")
//      cmd
//        .addUris(CommandInfo.URI.newBuilder.setValue(FrameworkUtil.getModuleUrl(FrameworkUtil.instance.get)))
//        .setValue("java " + jvmOptions + " -jar " + FrameworkUtil.jarName.get)
//        .setEnvironment(environments)
//    } match {
//      case Success(_) =>
//      case Failure(e: Exception) => FrameworkUtil.handleSchedulerException(e, logger)
//      case Failure(e) => throw e

    def getInstanceHosts: String = {
      val hosts: collection.mutable.ListBuffer[String] = collection.mutable.ListBuffer()
      TasksList.toLaunch.foreach(task =>
        if (TasksList.getTask(task).host.nonEmpty) hosts.append(TasksList.getTask(task).host.get)
      )
      hosts.mkString(",")
    }

    def getEnvironments: Environment = {
      Environment.newBuilder
        .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_NAME").setValue(FrameworkUtil.params {"instanceId"}))
        .addVariables(Environment.Variable.newBuilder.setName("TASK_NAME").setValue(task))
        .addVariables(Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(OffersHandler.getOfferIp(offer)))
        .addVariables(Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(getAgentPorts))
        .addVariables(Environment.Variable.newBuilder.setName("INSTANCE_HOSTS").setValue(getInstanceHosts))
        .build()
    }

    CommandInfo.newBuilder
      .addUris(CommandInfo.URI.newBuilder.setValue(FrameworkUtil.getModuleUrl(FrameworkUtil.instance.get)))
      .setValue("java " + FrameworkUtil.getJvmOptions + " -jar " + FrameworkUtil.jarName)
      .setEnvironment(getEnvironments).build()
  }


  def addTaskToSlave(task: TaskInfo, offer: (Offer, Int)) = {
    if (launchedOffers.contains(offer._1.getId)) {
      launchedOffers(offer._1.getId) += task
    } else {
      launchedOffers += offer._1.getId -> ArrayBuffer(task)
    }
  }

}
