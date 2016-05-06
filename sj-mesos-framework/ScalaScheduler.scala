import java.util
import mesosphere.mesos.util.ScalarResource
import org.apache.mesos.Protos._
import org.apache.mesos.{SchedulerDriver, Scheduler}
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.immutable
import scala.util.Properties

import com.mongodb.casbah.Imports._
import com.mongodb.DBObject

import org.slf4j._
import org.slf4j.LoggerFactory

import play.api.libs.json._

import com.twitter.common.zookeeper.{DistributedLockImpl, ZooKeeperClient}
import com.twitter.common.quantity.{Amount, Time}

import java.net.InetSocketAddress
import java.net.InetAddress



class ScalaScheduler extends Scheduler {

  var cores: Double = 0.0
  var ram: Double = 0.0
  var tasksToLaunch = List[Tuple2[String, JsValue]]()
  var params = immutable.Map[String, String]()
  val logger = LoggerFactory.getLogger(classOf[Scheduler])
  var instance: JsValue = _

  def error(driver: SchedulerDriver, message: String) {
    logger.error(s"Got error message: $message")
  }

  def executorLost(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, status: Int) {}

  def slaveLost(driver: SchedulerDriver, slaveId: SlaveID) {}

  def disconnected(driver: SchedulerDriver) {}

  def frameworkMessage(driver: SchedulerDriver, executorId: ExecutorID, slaveId: SlaveID, data: Array[Byte]) {
    logger.info(s"Got framework message")
    logger.debug(s"$data")
  }

  def statusUpdate(driver: SchedulerDriver, status: TaskStatus) {
    logger.info(s"STATUS UPDATE")
    if (status != null) {
      logger.info(s"Task: ${status.getTaskId.getValue}")
      logger.info(s"Status: ${status.getState}")
      val tasks = Json.parse((this.instance \ "execution-plan" \ "tasks").toString).as[JsObject].value
      if (status.getState.toString == "TASK_FAILED") {
        this.tasksToLaunch = this.tasksToLaunch.:::(
          List(Tuple2(status.getTaskId.getValue,
            tasks {
              status.getTaskId.getValue
            })))
        logger.info("ADDED TASK TO LAUNCH")
      }
    }
  }

  def offerRescinded(driver: SchedulerDriver, offerId: OfferID) {
  }


  def howMuchTasksOnSlave(perTaskCores: Double, perTaskRam: Double, tasksCount: Int, offers: util.List[Offer]):List[Tuple2[Offer, Int]]={
    /**
      * This method give list of slaves id and how many tasks we can launch on each slave.
      */
    var over_cpus = 0.0
    var over_mem = 0.0
    val req_cpus = perTaskCores*tasksCount
    val req_mem = perTaskRam*tasksCount
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

    logger.debug(s"Have resources: ${over_cpus} cpus, ${over_mem} mem")
    logger.debug(s"Resource requirements: ${req_cpus} cpus, ${req_mem} mem")
    return tasksNumber
  }


  def getResource(offer: Offer, name: String): Double = {
    /**
      * This method give how much resource of type <name> we have on <offer>
      */
    val res = offer.getResourcesList
    for (r <- res.asScala if r.getName == name) {
      return r.getScalar.getValue
    }
    return 0.0
  }


  def filterOffers(offers: util.List[Offer], filters: List[String]): Unit = {

  }

  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]) {
    logger.info(s"RESOURCE OFFERS")
    logger.debug(s"$offers")

    for (offer <- offers.asScala){
      logger.info(s"Offer ID: ${offer.getId.getValue}")
      logger.info(s"Slave ID: ${offer.getSlaveId.getValue}")
    }

    logger.info(s"Tasks to launch: ${this.tasksToLaunch}")

    val tasksCount = this.tasksToLaunch.size
    var tasksOnSlaves = howMuchTasksOnSlave(this.cores, this.ram, tasksCount, offers)

    var overTasks = 0
    for (slave <- tasksOnSlaves) {
      overTasks += slave._2
    }
    logger.info(s"Number of tasks can be launched: $overTasks")
    logger.info(s"Number of tasks must be launched: $tasksCount")
    if (tasksCount > overTasks) {
      logger.info(s"Can not launch tasks: no required resources")
      for (offer <- offers.asScala) {
        driver.declineOffer(offer.getId)
      }
      return
    }

    var i = 0
    var launchedTasks: Map[OfferID, List[TaskInfo]] = Map()
    for (task_temp <- this.tasksToLaunch) {

      val cmd = CommandInfo.newBuilder
        .addUris(CommandInfo.URI.newBuilder.setValue("http://192.168.1.225:8000/testScript.sh"))
        .setValue("sh testScript.sh")

      val cpus = ScalarResource("cpus", this.cores)
      val mem = ScalarResource("mem", this.ram)
      val task_id = task_temp._1

      while (tasksOnSlaves(i)._2 == 0) {
        tasksOnSlaves = tasksOnSlaves.filterNot(elem => elem == tasksOnSlaves(i))
      }
      val currentOffer = tasksOnSlaves(i)
      if (i >= offers.size - 1) {i = 0} else {i += 1}
      logger.info(s"Current task: ${task_temp._1}")
      logger.info(s"Current slave: ${currentOffer._1.getId.getValue}")

      val task = TaskInfo.newBuilder
        .setCommand(cmd)
        .setName(task_id)
        .setTaskId(TaskID.newBuilder.setValue(task_id))
        .addResources(cpus.toProto)
        .addResources(mem.toProto)
        .setSlaveId(currentOffer._1.getSlaveId)
        .build
      var listTasks: List[TaskInfo] = List()
      listTasks :::= (List(task))

      if (launchedTasks.contains(currentOffer._1.getId)){
          launchedTasks += (currentOffer._1.getId -> launchedTasks {currentOffer._1.getId}.:::(listTasks))
        } else {
        launchedTasks += (currentOffer._1.getId -> listTasks)
      }

      tasksOnSlaves.updated(tasksOnSlaves.indexOf(currentOffer), Tuple2(currentOffer._1, currentOffer._2-1))
      this.tasksToLaunch = this.tasksToLaunch.filterNot(elem => elem == task_temp)

    }
    for (task <- launchedTasks) {
      driver.launchTasks(task._1, task._2.asJava)
    }
    for (offer <- offers.asScala) {
      driver.declineOffer(offer.getId)
    }
  }


  def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo) {
    logger.info(s"New master ${masterInfo}")
  }


  def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo) {
    logger.info(s"Registered framework as: ${frameworkId.getValue}")
    this.params = immutable.Map(
      ("instanceId", Properties.envOrElse("INSTANCE_ID", "00000000-0000-0000-0000-000000000000")),
      ("zkPrefix", Properties.envOrElse("ZK_PREFIX", "zk")),
      ("zkHosts", Properties.envOrElse("ZK_HOSTS", "127.0.0.1")),
      ("zkSecret", Properties.envOrElse("ZK_SECRET", "")),
      ("zkMesosPrefix", Properties.envOrElse("ZK_MESOS_PREFIX", "mesos")),
      ("mongodbHost", Properties.envOrElse("MONGODB_HOST", "127.0.0.1")),
      ("mongodbUser", Properties.envOrElse("MONGODB_USER", "")),
      ("mongodbPassword", Properties.envOrElse("MONGODB_PASSWORD", "")),
      ("restService", Properties.envOrElse("REST_SERVICE", "127.0.0.1")),
      ("jarUri", Properties.envOrElse("JAR_URI", ""))
    )
    logger.info(s"Got environment variable: ${this.params}")

    val zkhost = new InetSocketAddress(InetAddress.getByName(this.params{"zkHosts"}), 2181)
    val zkClient = new ZooKeeperClient(Amount.of(60, Time.MINUTES), zkhost)
    val lockPath = s"/${this.params{"zkPrefix"}}/${this.params{"instanceId"}}/lock"
    val dli = new DistributedLockImpl(zkClient, lockPath)
    dli.lock()
    logger.info("Framework locked")

    val dbName = "stream_juggler"
    val collName = "instances"
    val dbHost = this.params{"mongodbHost"}
    val entityCollection = MongoClient(dbHost)(dbName)(collName)
    val obj = MongoDBObject("uuid" -> this.params{"instanceId"})
    entityCollection += obj
    this.instance = Json.parse(s"""${entityCollection.findOne().map(_.toString).get}""")
    logger.debug(s"Find instance: ${this.instance}")

    this.cores = (this.instance \ "per-task-cores").toString.toDouble
    this.ram = (this.instance \ "per-task-ram").toString.toDouble

    val tasks = Json.parse((this.instance \ "execution-plan" \ "tasks").toString).as[JsObject].value
    logger.info(s"Got tasks")
    for (task <- tasks) {
      this.tasksToLaunch :::= List(task)
      logger.info(s"$task")
    }
  }
}

