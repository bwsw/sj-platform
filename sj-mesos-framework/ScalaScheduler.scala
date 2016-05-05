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

  var tasksLaunched = List[String]()
  var tasksToLaunch = List[String]()
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
    logger.info(s"Received status update $status")
    logger.debug(s"$status")
  }

  def offerRescinded(driver: SchedulerDriver, offerId: OfferID) {
  }


  def howMuchTasksOnSlave(perTaskCores: Double, perTaskRam: Double, tasksCount: Int, offers: util.List[Offer]):List[Map[String, Any]]={
    /**
      * This method give list of slaves id and how many tasks we can launch on each slave.
      */
    var over_cpus = 0.0
    var over_mem = 0.0
    val req_cpus = perTaskCores*tasksCount
    val req_mem = perTaskRam*tasksCount
    var tasksNumber: List[Map[String, Any]] = List()
    for (offer <- offers.asScala) {
      tasksNumber = tasksNumber.:::(List(Map(
        ("id", offer.getSlaveId.getValue),
        ("tasksCount", java.lang.Math.min(
          getResource(offer, "cpus") / perTaskCores,
          getResource(offer, "mem") / perTaskRam
        ).floor.toInt
          ))))
      over_cpus += getResource(offer, "cpus")
      over_mem += getResource(offer, "mem")
    }

    logger.info(s"Have resources: ${over_cpus} cpus, ${over_mem} mem")
    logger.info(s"Resource requirements: ${req_cpus} cpus, ${req_mem} mem")
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
    throw new IllegalArgumentException("No resource called " + name + " in " + res)
  }

  def filterTasks(): Unit = {
    val tasks = Json.parse((this.instance \ "execution-plan" \ "tasks").toString).as[JsObject].value

    for (task <- tasks) {
      if (this.tasksLaunched.contains(task._1)){
        logger.info(s"Task ID: ${task._1}")
      } else {
        this.tasksToLaunch :::= (List(task._1))
      }
    }
    logger.info(s"${this.tasksToLaunch}")
  }


  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]) {
    logger.info(s"Got resource offers")
    logger.debug(s"$offers")

    filterTasks()
    logger.info(s"TASKS TO LAUNCH: ${this.tasksToLaunch}")

    val cores = (this.instance \ "per-task-cores").toString.toDouble
    val ram = (this.instance \ "per-task-ram").toString.toDouble
    val tasksCount = Json.parse((this.instance \ "execution-plan" \ "tasks").toString).as[JsObject].value.size
    val tasksOnSlaves = howMuchTasksOnSlave(cores, ram, tasksCount, offers)

    var overTasks = 0
    for (slave <- tasksOnSlaves) {
      overTasks += slave{"tasksCount"}.toString.toInt
    }
    logger.info(s"Number of tasks must be launched: $tasksCount")
    logger.info(s"Number of tasks can be launched: $overTasks")
    if (tasksCount > overTasks) {
      logger.info(s"Can not run tasks: no required resources")
    }




//    logger.debug(s"INSTANCE: ${Json.prettyPrint(this.instance)}")

//    val str = Json.prettyPrint(inst)
//    println(str)

    for (offer <- offers.asScala) {

      val cmd = CommandInfo.newBuilder
        .addUris(CommandInfo.URI.newBuilder.setValue("http://192.168.1.225:8000/testScript.sh"))
        .setValue("sh testScript.sh")

//      val attributes = offer.getAttributesList()
//      logger.debug(s"Offer ${offer.getId.getValue()} got attributes: $attributes")


      val cpus = ScalarResource("cpus", cores)
      val mem = ScalarResource("mem", ram)
      val task_id = "task" + offer.getSlaveId.getValue()

      val task = TaskInfo.newBuilder
        .setCommand(cmd)
        .setName(task_id)
        .setTaskId(TaskID.newBuilder.setValue(task_id))
        .addResources(cpus.toProto)
        .addResources(mem.toProto)
        .setSlaveId(offer.getSlaveId)
        .build

      val listTasks = mutable.MutableList[TaskInfo]()
      listTasks += task

      driver.launchTasks(offer.getId, listTasks.asJava)


      logger.info(s"Launched tasks ")
      logger.debug(s"$listTasks ")
      logger.info(s"on slave ${offer.getSlaveId.getValue}")


//      driver.declineOffer(offer.getId)

    }


//    while (true) {
//      logger.info("loop")
//      Thread.sleep(5000)
//    }
//      for (offer <- offers.asScala){
//        logger.info("WAIT")
//        Thread.sleep(5000)
//      }
    // @todo remove stop()
//    driver.stop()
  }

  def launchTask(driver: SchedulerDriver, taskInfo: TaskInfo) {
    logger.info(s"Launched Task: $taskInfo")
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

  }
}

