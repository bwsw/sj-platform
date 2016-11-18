package com.bwsw.sj.mesos.framework.schedule

import java.util

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.mesos.framework.task.{StatusHandler, TasksList}
import org.apache.log4j.Logger
import org.apache.mesos.Protos._
import org.apache.mesos.{Scheduler, SchedulerDriver}
import scala.collection.JavaConverters._
import scala.collection.mutable


/**
  * Mesos scheduler implementation
  */
class FrameworkScheduler extends Scheduler {

  private val logger = Logger.getLogger(this.getClass)
  var uniqueHosts = false


  def error(driver: SchedulerDriver, message: String) {
    logger.error(s"Got error message: $message")
    TasksList.message = s"Got error message: $message"
  }

  def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int): Unit = {
    /// TODO:
  }

  def slaveLost(driver: SchedulerDriver, slaveId: SlaveID): Unit = {
    /// TODO:
  }

  def disconnected(driver: SchedulerDriver): Unit = {
    /// TODO:
  }

  def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]) {
    logger.debug(s"Got framework message: $data")
    TasksList.message = s"Got framework message: $data"
  }


  /**
   * Execute when task change status.
   * @param driver scheduler driver
   * @param status received status from master
   */
  def statusUpdate(driver: SchedulerDriver, status: TaskStatus) {
    StatusHandler.handle(status)
  }



  def offerRescinded(driver: SchedulerDriver, offerId: OfferID): Unit = {
    /// TODO:
  }


  /**
   * Obtaining resources and launch tasks.
   *
   * @param driver scheduler driver
   * @param offers resources, that master offered to framework
   */
  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]) {
    logger.info(s"RESOURCE OFFERS")
    TasksList.clearAvailablePorts()
    OfferHandler.setOffers(offers)
    OfferHandler.filter(FrameworkUtil.instance.nodeAttributes)
    if (OfferHandler.filteredOffers.size == 0) {
      logger.info("No one node selected")
      TasksList.message = "No one node selected"
      for (offer <- offers.asScala) driver.declineOffer(offer.getId)
      return
    }

    OfferHandler.filteredOffers.asScala.foreach(offer => {
      logger.debug(s"Offer ID: ${offer.getId.getValue}")
      logger.debug(s"Slave ID: ${offer.getSlaveId.getValue}")
    })

    var tasksCountOnSlaves: mutable.ListBuffer[(Offer, Int)] = OfferHandler.getOffersForSlave()
    var overTasks = tasksCountOnSlaves.foldLeft(0)(_+_._2)
    if (uniqueHosts && tasksCountOnSlaves.length < overTasks) overTasks = tasksCountOnSlaves.length

    logger.debug(s"Count tasks can be launched: $overTasks")
    logger.debug(s"Count tasks must be launched: ${TasksList.count}")

    if (TasksList.count > overTasks) {
      logger.info(s"Can not launch tasks: no required resources")
      TasksList.message = "Can not launch tasks: no required resources"
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId)
      }
      return
    }
    logger.debug(s"Tasks to launch: ${TasksList.toLaunch}")


    OfferHandler.offerNumber = 0
    TasksList.launchedTasks = mutable.Map()
    for (currTask <- TasksList.toLaunch) {

      val currentOffer = OfferHandler.getNextOffer(tasksCountOnSlaves)
      val task = TasksList.createTaskToLaunch(currTask, currentOffer._1)

      TasksList.addTaskToSlave(task, currentOffer)

      // update how much tasks we can run on slave when launch current task
      tasksCountOnSlaves.update(tasksCountOnSlaves.indexOf(currentOffer), Tuple2(currentOffer._1, currentOffer._2 - 1))
      TasksList.launched(currTask)

      tasksCountOnSlaves = OfferHandler.updateOfferNumber(tasksCountOnSlaves)

    }

    for (task <- TasksList.launchedTasks) {
      driver.launchTasks(List(task._1).asJava, task._2.asJava)
    }

    for (offer <- offers.asScala) {
      driver.declineOffer(offer.getId)
    }
    TasksList.message = "Tasks launched"
  }


  /**
   * Reregistering framework after master disconnected.
   */
  def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo) {
    logger.debug(s"New master $masterInfo")
    TasksList.message = s"New master $masterInfo"
  }


  /**
   * Registering framework.
   */
  def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo) {
    logger.info(s"Registered framework as: ${frameworkId.getValue}")

    FrameworkUtil.driver = driver
    FrameworkUtil.frameworkId = frameworkId.getValue
    FrameworkUtil.master = masterInfo

    FrameworkUtil.params = FrameworkUtil.getEnvParams()
    logger.debug(s"Got environment variable: ${FrameworkUtil.params}")

    val optionInstance = ConnectionRepository.getInstanceService.get(FrameworkUtil.params("instanceId"))

    if (optionInstance.isEmpty) {
      logger.error(s"Not found instance")
      TasksList.message = "Framework shut down: not found instance."
      driver.stop()
      return
    } else { FrameworkUtil.instance = optionInstance.get}
    logger.debug(s"Got instance ${FrameworkUtil.instance.name}")

    TasksList.prepare(FrameworkUtil.instance)
    logger.debug(s"Got tasks: $TasksList")

    uniqueHosts = scala.util.Try(System.getenv("UNIQUE_HOSTS").toBoolean).getOrElse(false)

    TasksList.message = s"Registered framework as: ${frameworkId.getValue}"
  }

}

