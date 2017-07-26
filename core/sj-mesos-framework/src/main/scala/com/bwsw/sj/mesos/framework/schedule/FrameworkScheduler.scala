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

import java.util

import com.bwsw.sj.mesos.framework.config.FrameworkConfigNames
import com.bwsw.sj.mesos.framework.task.{StatusHandler, TasksList}
import com.typesafe.config.ConfigFactory
import org.apache.log4j.Logger
import org.apache.mesos.Protos._
import org.apache.mesos.{Scheduler, SchedulerDriver}
import scaldi.Injector

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try

/**
  * Mesos scheduler implementation
  */
class FrameworkScheduler(implicit injector: Injector) extends Scheduler {

  private val logger = Logger.getLogger(this.getClass)
  var uniqueHosts = false


  def error(driver: SchedulerDriver, message: String): Unit = {
    logger.error(s"Got error message: $message")
    TasksList.setMessage(s"Got error message: $message")
  }

  def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int): Unit = {
    logger.debug(s"Executor ${slaveId.getValue}/${executorId.getValue} lost with status $status.")
    TasksList.setMessage(s"Executor ${slaveId.getValue}/${executorId.getValue} lost with status $status.")
  }

  def slaveLost(driver: SchedulerDriver, slaveId: SlaveID): Unit = {
    logger.debug(s"Slave ${slaveId.getValue} lost.")
    TasksList.setMessage(s"Slave ${slaveId.getValue} lost.")
  }

  def disconnected(driver: SchedulerDriver): Unit = {
    logger.debug(s"Framework disconnected.")
    TasksList.setMessage(s"Framework disconnected.")
  }

  def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]): Unit = {
    logger.debug(s"Got framework message: $data.")
    TasksList.setMessage(s"Got framework message: $data")
  }


  /**
    * Execute when task change status.
    *
    * @param driver scheduler driver
    * @param status received status from master
    */
  def statusUpdate(driver: SchedulerDriver, status: TaskStatus): Unit = {
    logger.info(s"GOT STATUS UPDATE")
    StatusHandler.handle(status)
  }

  def offerRescinded(driver: SchedulerDriver, offerId: OfferID): Unit = {
    logger.debug(s"Offer ${offerId.getValue} rescinded.")
    TasksList.setMessage(s"Offer ${offerId.getValue} rescinded.")
  }


  /**
    * Obtain resources and launch tasks.
    *
    * @param driver scheduler driver
    * @param offers resources, that master offered to framework
    */
  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]): Unit = {
    logger.info(s"GOT RESOURCE OFFERS")

    FrameworkUtil.checkInstanceStarted()
    TasksList.clearAvailablePorts()
    OffersHandler.setOffers(offers.asScala)

    OffersHandler.filter(FrameworkUtil.instance.get.nodeAttributes)
    if (OffersHandler.filteredOffers.isEmpty) {
      declineOffers("No one node selected", OffersHandler.getOffers)
      return
    }

    val overTasks = getAvailableNumberOfTasks

    if (TasksList.count > overTasks) {
      declineOffers("Can not launch tasks: insufficient resources", OffersHandler.getOffers)
      return
    }

    OffersHandler.distributeTasksOnSlaves()
    logger.info(s"Launch tasks")
    launchTasks(driver)
    logger.info(s"Launched tasks: ${TasksList.getLaunchedTasks}")
  }

  private def declineOffers(message: String, offers: mutable.Buffer[Offer]): Unit = {
    logger.info(message)
    TasksList.setMessage(message)
    offers.foreach(offer => FrameworkUtil.driver.get.declineOffer(offer.getId))
  }

  def getAvailableNumberOfTasks: Int = {
    logger.debug("Selected resource offers: ")
    OffersHandler.filteredOffers.foreach(offer => {
      logger.debug(s"Offer ID: ${offer.getId.getValue}.")
      logger.debug(s"Slave ID: ${offer.getSlaveId.getValue}.")
    })
    var overTasks: Int = OffersHandler.tasksCountOnSlaves.foldLeft(0)(_ + _._2)
    if (uniqueHosts && OffersHandler.tasksCountOnSlaves.length < overTasks) overTasks =
      OffersHandler.tasksCountOnSlaves.length
    logger.debug(s"Can be launched: $overTasks tasks.")
    logger.debug(s"Must be launched: ${TasksList.count} tasks.")
    overTasks
  }

  /**
    * Ask driver to launch prepared tasks.
    *
    * @param driver
    */
  private def launchTasks(driver: SchedulerDriver): Unit = {
    for (task <- TasksList.getLaunchedOffers) {
      driver.launchTasks(List(task._1).asJava, task._2.asJava)
    }
    TasksList.clearLaunchedOffers()
    declineOffers("Tasks have been launched", OffersHandler.getOffers)
  }

  /**
    * Perform a reregistration of framework after master disconnected.
    */
  def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo): Unit = {
    logger.debug(s"New master $masterInfo.")
    TasksList.setMessage(s"New master $masterInfo")
  }

  /**
    * Registering framework.
    */
  def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo): Unit = {
    logger.info(s"Registered framework as: ${frameworkId.getValue}.")

    FrameworkUtil.driver = Option(driver)
    FrameworkUtil.frameworkId = Option(frameworkId.getValue)
    FrameworkUtil.master = Option(masterInfo)

    FrameworkParameters.prepareEnvParams()
    logger.debug(s"Got environment variable: ${FrameworkParameters()}.")

    FrameworkUtil.updateInstance()
    logger.debug(s"Got instance ${FrameworkUtil.instance.get.name}.")

    TasksList.prepare(FrameworkUtil.instance.get)
    logger.debug(s"Got tasks: $TasksList.")

    val config = ConfigFactory.load()
    uniqueHosts = Try(config.getBoolean(FrameworkConfigNames.uniqueHosts)).getOrElse(false)

    TasksList.setMessage(s"Registered framework as: ${frameworkId.getValue}")
  }
}

