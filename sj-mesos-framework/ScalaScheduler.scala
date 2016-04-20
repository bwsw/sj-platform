import java.util
import mesosphere.mesos.util.ScalarResource
import org.apache.mesos.Protos._
import org.apache.mesos.{SchedulerDriver, Scheduler}
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.immutable
import scala.util.Properties
import org.mongodb.

import com.mongodb.casbah.Imports._
import Common._

import org.slf4j._
import org.slf4j.LoggerFactory


class ScalaScheduler extends Scheduler {
  var envVar = immutable.Map[String, Option[String]]()

  val logger = LoggerFactory.getLogger(classOf[Scheduler])

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
    logger.info(s"Received status update")
    logger.debug(s"$status")
  }

  def offerRescinded(driver: SchedulerDriver, offerId: OfferID) {
  }

  override def resourceOffers(driver: SchedulerDriver, offers: util.List[Offer]) {
    logger.info(s"Got resource offers")
    logger.debug(s"$offers")

    for (offer <- offers.asScala) {

      val cmd = CommandInfo.newBuilder
        .addUris(CommandInfo.URI.newBuilder.setValue("file:///home/diryavkin_dn/IdeaProjects/sss.sh"))
        .addUris(CommandInfo.URI.newBuilder.setValue("file:///home/diryavkin_dn/IdeaProjects/install"))
        .addUris(CommandInfo.URI.newBuilder.setValue("http://www-eu.apache.org/dist/spark/spark-1.6.1/spark-1.6.1.tgz"))
        .setValue("sh sss.sh")

      val attributes = offer.getAttributesList()
      logger.debug(s"Offer ${offer.getId.getValue()} got attributes: $attributes")

      val cpus = ScalarResource("cpus", 0.1)
      val mem = ScalarResource("mem", 32)
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

    }
    // @todo remove stop()
//    driver.stop()
  }

  def launchTask(driver: SchedulerDriver, taskInfo: TaskInfo) {

  }

  def reregistered(driver: SchedulerDriver, masterInfo: MasterInfo) {}

  def registered(driver: SchedulerDriver, frameworkId: FrameworkID, masterInfo: MasterInfo) {
    logger.info(s"Registered framework $frameworkId")
    this.envVar = immutable.Map(
      ("instanceId", Properties.envOrNone("INSTANCE_ID")),
      ("zkPrefix", Properties.envOrNone("ZK_PREFIX")),
      ("zkHosts", Properties.envOrNone("ZK_SECRET")),
      ("zkMesosPrefix", Properties.envOrNone("ZK_MESOS_PREFIX")),
      ("mongodbHost", Properties.envOrNone("MONGODB_HOST")),
      ("mongodbUser", Properties.envOrNone("MONGODB_USER")),
      ("mongodbPassword", Properties.envOrNone("MONGODB_PASSWORD")),
      ("restService", Properties.envOrNone("REST_SERVICE"))
    )
    logger.debug(s"Got environment variable: ${this.envVar}")
  }


}

