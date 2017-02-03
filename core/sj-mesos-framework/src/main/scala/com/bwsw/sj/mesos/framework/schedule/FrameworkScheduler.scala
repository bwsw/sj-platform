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
    TasksList.setMessage(s"Got error message: $message")
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
    TasksList.setMessage(s"Got framework message: $data")
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
   * Obtain resources and launch tasks.
   *
   * @param driver scheduler driver
   * @param offers resources, that master offered to framework
   */
  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]): Unit = {

    if (!FrameworkUtil.isInstanceStarted) FrameworkUtil.killAllLaunchedTasks()


    val internalOffers: mutable.Buffer[Offer] = offers.asScala
    logger.info(s"RESOURCE OFFERS")
    TasksList.clearAvailablePorts()
    OfferHandler.setOffers(internalOffers)
    OfferHandler.filter(FrameworkUtil.instance.nodeAttributes)
    if (OfferHandler.filteredOffers.isEmpty) {
      logger.info("No one node selected")
      TasksList.setMessage("No one node selected")
      declineOffers(driver, internalOffers)
      return
    }

    OfferHandler.filteredOffers.foreach(offer => {
      logger.debug(s"Offer ID: ${offer.getId.getValue}")
      logger.debug(s"Slave ID: ${offer.getSlaveId.getValue}")
    })

    var tasksCountOnSlaves: mutable.ListBuffer[(Offer, Int)] = OfferHandler.getOffersForSlave()
    var overTasks = tasksCountOnSlaves.foldLeft(0)(_ + _._2)
    if (uniqueHosts && tasksCountOnSlaves.length < overTasks) overTasks = tasksCountOnSlaves.length

    logger.debug(s"Count tasks can be launched: $overTasks")
    logger.debug(s"Count tasks must be launched: ${TasksList.count}")

    if (TasksList.count > overTasks) {
      logger.info(s"Can not launch tasks: no required resources")
      TasksList.setMessage("Can not launch tasks: no required resources")
      declineOffers(driver, internalOffers)
      return
    }
    logger.debug(s"Tasks to launch: ${TasksList.toLaunch}")

    OfferHandler.offerNumber = 0
    if (FrameworkUtil.isInstanceStarted) {
      TasksList.clearLaunchedOffers()
      for (currTask <- TasksList.toLaunch) {
        createTaskToLaunch(currTask, tasksCountOnSlaves)
        tasksCountOnSlaves = OfferHandler.updateOfferNumber(tasksCountOnSlaves)
      }
    }

    launchTasks(driver)
    declineOffers(driver, internalOffers)
    TasksList.setMessage("Tasks have been launched")
  }

  private def declineOffers(driver: SchedulerDriver, offers: mutable.Buffer[Offer]) = {
    offers.foreach(offer => driver.declineOffer(offer.getId))
  }

  /**
    * Create task to launch.
    * @param taskName
    * @param tasksCountOnSlaves
    * @return
    */
  private def createTaskToLaunch(taskName: String, tasksCountOnSlaves: mutable.ListBuffer[(Offer, Int)]) = {
    val currentOffer = OfferHandler.getNextOffer(tasksCountOnSlaves)
    val task = TasksList.createTaskToLaunch(taskName, currentOffer._1)

    TasksList.addTaskToSlave(task, currentOffer)

    // update how much tasks we can run on slave when launch current task
    tasksCountOnSlaves.update(tasksCountOnSlaves.indexOf(currentOffer), Tuple2(currentOffer._1, currentOffer._2 - 1))
    TasksList.launched(taskName)
  }

  /**
    * Ask driver to launch prepared tasks.
    * @param driver
    */
  private def launchTasks(driver: SchedulerDriver) = {
    for (task <- TasksList.getLaunchedOffers()) {
      driver.launchTasks(List(task._1).asJava, task._2.asJava)
    }
  }

  /**
   * Perform a reregistration of framework after master disconnected.
   */
  def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo) {
    logger.debug(s"New master $masterInfo")
    TasksList.setMessage(s"New master $masterInfo")
  }


  /**
   * Registering framework.
   */
  def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo) {
    logger.info(s"Registered framework as: ${frameworkId.getValue}")

    FrameworkUtil.driver = driver
    FrameworkUtil.frameworkId = frameworkId.getValue
    FrameworkUtil.master = masterInfo

    FrameworkUtil.params = FrameworkUtil.getEnvParams
    logger.debug(s"Got environment variable: ${FrameworkUtil.params}")

    val optionInstance = ConnectionRepository.getInstanceService.get(FrameworkUtil.params("instanceId"))

    if (optionInstance.isEmpty) {
      logger.error(s"Not found instance")
      TasksList.setMessage("Framework shut down: not found instance.")
      driver.stop()
      return
    } else {
      FrameworkUtil.instance = optionInstance.get
    }
    logger.debug(s"Got instance ${FrameworkUtil.instance.name}")

    TasksList.prepare(FrameworkUtil.instance)
    logger.debug(s"Got tasks: $TasksList")

    uniqueHosts = scala.util.Try(System.getenv("UNIQUE_HOSTS").toBoolean).getOrElse(false)

    TasksList.setMessage(s"Registered framework as: ${frameworkId.getValue}")
  }
}

