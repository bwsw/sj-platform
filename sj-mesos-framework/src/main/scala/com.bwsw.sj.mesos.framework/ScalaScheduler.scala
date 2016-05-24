package com.bwsw.sj.mesos.framework

import java.util

import org.apache.mesos.Protos._
import org.apache.mesos.{Protos, Scheduler, SchedulerDriver}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.immutable
import scala.util.Properties
import org.apache.log4j.Logger
import com.twitter.common.zookeeper.{DistributedLockImpl, ZooKeeperClient}
import com.twitter.common.quantity.{Amount, Time}
import java.net.InetSocketAddress
import java.net.InetAddress


import com.bwsw.common.client.Client._
import com.bwsw.sj.common.DAL.model.RegularInstance
import com.bwsw.sj.common.DAL.model.{Task => ormTask}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import org.apache.log4j.Logger


class ScalaScheduler extends Scheduler {
  var cores: Double = 0.0
  var ram: Double = 0.0
  var params = immutable.Map[String, String]()
  private val logger = Logger.getLogger(getClass)
  var instance: RegularInstance = null

  def error(driver: SchedulerDriver, message: String) {
    logger.error(s"Got error message: $message")
    TasksList.message = s"Got error message: $message"

  }

  def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int) {}

  def slaveLost(driver: SchedulerDriver, slaveId: SlaveID) {}

  def disconnected(driver: SchedulerDriver) {}

  def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]) {
    logger.info(s"Got framework message")
    logger.debug(s"$data")
    TasksList.message = s"Got framework message: $data"
  }

  def statusUpdate(driver: SchedulerDriver, status: TaskStatus) {
    logger.info(s"STATUS UPDATE")
    if (status != null) {
      TasksList(status.getTaskId.getValue).foreach(x => x.update(
        state = status.getState.toString,
        state_changed = (status.getTimestamp.toLong*1000).toString,
        last_node = if (x.node != "") x.node else x.last_node, node = status.getSlaveId.getValue, reason = ""
      ))
      logger.info(s"Task: ${status.getTaskId.getValue}")
      logger.info(s"Status: ${status.getState}")
      if (status.getState.toString == "TASK_FAILED" || status.getState.toString == "TASK_ERROR") {
        TasksList(status.getTaskId.getValue).foreach(x => x.update(
          node = "", reason = status.getMessage
        ))
        logger.info(s"Error: ${status.getMessage}")

        TasksList.addToLaunch(status.getTaskId.getValue)
        logger.info("Added task to launch")
      }
    }
  }

  def offerRescinded(driver: SchedulerDriver, offerId: OfferID) {
  }

  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]) {
    logger.info(s"RESOURCE OFFERS")

    val filteredOffers = filterOffers(offers, this.instance.attributes)
    if (filteredOffers.size == 0) {
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId)
      }
      TasksList.message = "No one node selected"
      return
    }

    for (offer <- filteredOffers.asScala) {
      logger.debug(s"Offer ID: ${offer.getId.getValue}")
      logger.debug(s"Slave ID: ${offer.getSlaveId.getValue}")
    }

    val tasksCount = TasksList.toLaunch.size //this.tasksToLaunch.size
    var tasksOnSlaves = howMuchTasksOnSlave(this.cores, this.ram, tasksCount, filteredOffers)

    var overTasks = 0
    for (slave <- tasksOnSlaves) {overTasks += slave._2}
    logger.info(s"Count tasks can be launched: $overTasks")
    logger.info(s"Count tasks must be launched: $tasksCount")
    if (tasksCount > overTasks) {
      logger.info(s"Can not launch tasks: no required resources")
      TasksList.message = "Can not launch tasks: no required resources"
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId)
      }
      return
    }

    logger.info(s"Tasks to launch: ${TasksList.toLaunch}")

    var offerNumber = 0
    var launchedTasks: Map[OfferID, List[TaskInfo]] = Map()
    for (curr_task <- TasksList.toLaunch) {

      val cmd = CommandInfo.newBuilder
        .addUris(CommandInfo.URI.newBuilder.setValue(getModuleUrl(this.instance)))
        .setValue("sh testScript.sh")
      //        .setValue("java -cp sj-common-assembly-0.1.jar com.bwsw.sj.common.module.regular.RegularTaskRunner")

      val cpus = Resource.newBuilder.
        setType(org.apache.mesos.Protos.Value.Type.SCALAR).
        setName("cpus").
        setScalar(org.apache.mesos.Protos.Value.
          Scalar.newBuilder.setValue(this.cores)).
        build
      val mem = Resource.newBuilder.
        setType(org.apache.mesos.Protos.Value.Type.SCALAR).
        setName("mem").
        setScalar(org.apache.mesos.Protos.Value.
          Scalar.newBuilder.setValue(this.ram)).
        build

      while (tasksOnSlaves(offerNumber)._2 == 0) {
        tasksOnSlaves = tasksOnSlaves.filterNot(elem => elem == tasksOnSlaves(offerNumber))
        if (offerNumber > tasksOnSlaves.size - 1) {
          offerNumber = 0
        }
      }
      val currentOffer = tasksOnSlaves(offerNumber)
      if (offerNumber >= tasksOnSlaves.size - 1) {
        offerNumber = 0
      } else {
        offerNumber += 1
      }

      logger.info(s"Current task: $curr_task")
      logger.info(s"Current slave: ${currentOffer._1.getSlaveId.getValue}")

      val task = TaskInfo.newBuilder
        .setCommand(cmd)
        .setName(curr_task)
        .setTaskId(TaskID.newBuilder.setValue(curr_task))
        .addResources(cpus)
        .addResources(mem)
        .setSlaveId(currentOffer._1.getSlaveId)
        .build()
      var listTasks: List[TaskInfo] = List()
      listTasks ::= task

      if (launchedTasks.contains(currentOffer._1.getId)) {
        launchedTasks += (currentOffer._1.getId -> launchedTasks {
          currentOffer._1.getId
        }.:::(listTasks))
      } else {
        launchedTasks += (currentOffer._1.getId -> listTasks)
      }

      tasksOnSlaves.updated(tasksOnSlaves.indexOf(currentOffer), Tuple2(currentOffer._1, currentOffer._2 - 1))
      TasksList.launched(curr_task)

    }

    for (task <- launchedTasks) {
      driver.launchTasks(List(task._1).asJava, task._2.asJava)
    }

    for (offer <- offers.asScala) {
      driver.declineOffer(offer.getId)
    }
    TasksList.message = "Tasks launched"
  }


  def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo) {
    logger.info(s"New master $masterInfo")
    TasksList.message = s"New master $masterInfo"
  }


  def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo) {
    logger.info(s"Registered framework as: ${frameworkId.getValue}")

    this.params = immutable.Map(
      ("instanceId", Properties.envOrElse("INSTANCE_ID", "00000000-0000-0000-0000-000000000000")),
      ("zkPrefix", Properties.envOrElse("ZK_PREFIX", "zk")),
      ("zkHosts", Properties.envOrElse("ZK_HOSTS", "127.0.0.1")),
//      ("zkSecret", Properties.envOrElse("ZK_SECRET", "")),
//      ("zkMesosPrefix", Properties.envOrElse("ZK_MESOS_PREFIX", "zk:/mesos")),
      ("mongodbHost", Properties.envOrElse("MONGODB_HOST", "127.0.0.1"))
//      ("mongodbUser", Properties.envOrElse("MONGODB_USER", "")),
//      ("mongodbPassword", Properties.envOrElse("MONGODB_PASSWORD", "")),
//      ("restService", Properties.envOrElse("REST_SERVICE", "127.0.0.1"))
    )
    logger.debug(s"Got environment variable: ${this.params}")

    this.instance = ConnectionRepository.getInstanceService.get(this.params("instanceId"))
    if (this.instance == null) {
      logger.error(s"Not found instance")
      driver.stop()
      TasksList.message = "Framework shut down: not found instance."
      return
    }
    this.cores = instance.perTaskCores
    this.ram = instance.perTaskRam
    logger.info(s"Got instance")
    logger.debug(s"${this.instance}")

    //lock framework
    val zkhost = new InetSocketAddress(InetAddress.getByName(this.params{"zkHosts"}), 2181)
//    val zkhost = new InetSocketAddress(InetAddress.getByName(this.instance.coordinationService.provider.hosts(1)), 2181)
    val zkClient = new ZooKeeperClient(Amount.of(1, Time.MINUTES), zkhost)
    val lockPath = s"/${this.params {"zkPrefix"}}/${this.params {"instanceId"}}/lock"
//    val lockPath = s"/${this.instance.coordinationService.namespace}/${this.params{"instanceId"}}/lock"
    val dli = new DistributedLockImpl(zkClient, lockPath)
    dli.lock()
    logger.info("Framework locked")

    val tasks = instance.executionPlan.tasks
    logger.info(s"Got tasks")
    for (task <- tasks.asScala) {
      TasksList.newTask(task._1)
      logger.info(s"$task")
    }
    TasksList.message = s"Registered framework as: ${frameworkId.getValue}"
  }

  def howMuchTasksOnSlave(perTaskCores: Double, perTaskRam: Double, tasksCount: Int, offers: util.List[Offer]): List[Tuple2[Offer, Int]] = {
    /**
      * This method give list of slaves id and how many tasks we can launch on each slave.
      */
    var over_cpus = 0.0
    var over_mem = 0.0
    val req_cpus = perTaskCores * tasksCount
    val req_mem = perTaskRam * tasksCount
    var tasksNumber: List[Tuple2[Offer, Int]] = List()
    for (offer <- offers.asScala) {
      tasksNumber = tasksNumber.:::(List(Tuple2(
        offer, java.lang.Math.min(
          getResource(offer, "cpus") / perTaskCores,
          getResource(offer, "mem") / perTaskRam
        ).floor.toInt
      )))
      over_cpus += getResource(offer, "cpus")
      over_mem += getResource(offer, "mem")
    }
    logger.debug(s"Have resources: $over_cpus cpus, $over_mem mem")
    logger.debug(s"Required resources: $req_cpus cpus, $req_mem mem")
    tasksNumber
  }


  def getResource(offer: Offer, name: String): Double = {
    /**
      * This method give how much resource of type <name> we have on <offer>
      */
    val res = offer.getResourcesList
    for (r <- res.asScala if r.getName == name) {
      return r.getScalar.getValue
    }
    0.0
  }


  def filterOffers(offers: util.List[Offer], filters: java.util.Map[String, String]): util.List[Offer] = {

    logger.info(s"FILTER OFFERS")
    var result: List[Offer] = List()
    if (filters != null) {
      for (offer <- offers.asScala) {
        for (filter <- filters.asScala) {
          if (filter._1.matches("""\+.+""".r.toString)) {
            for (attribute <- offer.getAttributesList.asScala) {
              if (filter._1.toString.substring(1) == attribute.getName &
              attribute.getText.getValue.matches(filter._2.r.toString)) {
                result ::= offer
              }
            }
          }
          if (filter._1.matches("""\-.+""".r.toString)) {
            for (attribute <- offer.getAttributesList.asScala) {
              if (filter._1.matches(attribute.getName.r.toString) &
                attribute.getText.getValue.matches(filter._2.r.toString)) {
                result = result.filterNot(elem => elem == offer)
              }
            }
          }
        }
      }
    } else return offers
    result.asJava
  }

  def getModuleUrl(instance: RegularInstance): String = {
    //    return "http://192.168.1.225:8000/sj-common-assembly-0.1.jar"
    "http://192.168.1.225:8000/testScript.sh"
  }

}

