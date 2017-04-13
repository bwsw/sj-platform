package com.bwsw.sj.mesos.framework.schedule

import java.util

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

  def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]) {
    logger.debug(s"Got framework message: $data.")
    TasksList.setMessage(s"Got framework message: $data")
  }

  /**
   * Execute when task change status.
    *
    * @param driver scheduler driver
   * @param status received status from master
   */
  def statusUpdate(driver: SchedulerDriver, status: TaskStatus) {
    logger.info(s"GOT STATUS UPDATE")
    StatusHandler.handle(status)
  }

  /**
   * Obtain resources and launch tasks.
   *
   * @param driver scheduler driver
   * @param offers resources, that master offered to framework
   */
  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]): Unit = {
    logger.info(s"GOT RESOURCE OFFERS")

    val started = FrameworkUtil.isInstanceStarted
    logger.info(s"Check is instance status 'started': $started")
    if (started) FrameworkUtil.prepareTasksToLaunch()
    else FrameworkUtil.teardown()

    TasksList.clearAvailablePorts()
    OffersHandler.setOffers(offers.asScala)

    OffersHandler.filter(FrameworkUtil.instance.nodeAttributes)
    if (OffersHandler.filteredOffers.isEmpty) {
      declineOffers("No one node selected", OffersHandler.getOffers)
      return
    }

    logger.debug("Selected resource offers: ")
    OffersHandler.filteredOffers.foreach(offer => {
      logger.debug(s"Offer ID: ${offer.getId.getValue}.")
      logger.debug(s"Slave ID: ${offer.getSlaveId.getValue}.")
    })

    var tasksCountOnSlaves: mutable.ListBuffer[(Offer, Int)] = OffersHandler.getOffersForSlave
    var overTasks = tasksCountOnSlaves.foldLeft(0)(_ + _._2)
    if (uniqueHosts && tasksCountOnSlaves.length < overTasks) overTasks = tasksCountOnSlaves.length

    logger.debug(s"Can be launched: $overTasks tasks.")
    logger.debug(s"Must be launched: ${TasksList.count} tasks.")

    if (TasksList.count > overTasks) {
      declineOffers("Can not launch tasks: insufficient resources", OffersHandler.getOffers)
      return
    }


    logger.info(s"Distribute tasks to resource offers")
    OffersHandler.offerNumber = 0
    if (FrameworkUtil.isInstanceStarted) {
      for (currTask <- TasksList.toLaunch) {
        createTaskToLaunch(currTask, tasksCountOnSlaves)
        tasksCountOnSlaves = OffersHandler.updateOfferNumber(tasksCountOnSlaves)
      }
    }
    logger.info(s"Launch tasks")
    launchTasks(driver)
    TasksList.clearLaunchedOffers()
    declineOffers("Tasks have been launched", OffersHandler.getOffers)
    logger.info(s"Launched tasks: ${TasksList.getLaunchedTasks}")
  }

  private def declineOffers(message: String, offers: mutable.Buffer[Offer]) = {
    logger.info(message)
    TasksList.setMessage(message)
    offers.foreach(offer => FrameworkUtil.driver.declineOffer(offer.getId))
  }


  /**
    * Create task to launch.
    *
    * @param taskName
    * @param tasksCountOnSlaves
    * @return
    */
  private def createTaskToLaunch(taskName: String, tasksCountOnSlaves: mutable.ListBuffer[(Offer, Int)]) = {
    val currentOffer = OffersHandler.getNextOffer(tasksCountOnSlaves)
    val task = TasksList.createTaskToLaunch(taskName, currentOffer._1)

    TasksList.addTaskToSlave(task, currentOffer)

    // update how much tasks we can run on slave when launch current task
    tasksCountOnSlaves.update(tasksCountOnSlaves.indexOf(currentOffer), Tuple2(currentOffer._1, currentOffer._2 - 1))
    TasksList.launched(taskName)
  }

  /**
    * Ask driver to launch prepared tasks.
    *
    * @param driver
    */
  private def launchTasks(driver: SchedulerDriver) = {
    for (task <- TasksList.getLaunchedOffers) {
      driver.launchTasks(List(task._1).asJava, task._2.asJava)
    }
  }

  /**
   * Perform a reregistration of framework after master disconnected.
   */
  def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo) {
    logger.debug(s"New master $masterInfo.")
    TasksList.setMessage(s"New master $masterInfo")
  }


  /**
   * Registering framework.
   */
  def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo) {
    logger.info(s"Registered framework as: ${frameworkId.getValue}.")

    FrameworkUtil.driver = driver
    FrameworkUtil.frameworkId = frameworkId.getValue
    FrameworkUtil.master = masterInfo

    FrameworkUtil.params = FrameworkUtil.getEnvParams
    logger.debug(s"Got environment variable: ${FrameworkUtil.params}.")

    FrameworkUtil.updateInstance()
    logger.debug(s"Got instance ${FrameworkUtil.instance.name}.")

    TasksList.prepare(FrameworkUtil.instance)
    logger.debug(s"Got tasks: $TasksList.")

    uniqueHosts = scala.util.Try(System.getenv("UNIQUE_HOSTS").toBoolean).getOrElse(false)

    TasksList.setMessage(s"Registered framework as: ${frameworkId.getValue}")
  }
}

