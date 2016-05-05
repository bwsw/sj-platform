import mesosphere.mesos.util.FrameworkInfo
import org.apache.mesos.MesosSchedulerDriver
import scala.util.Properties


object Main extends App {

  val framework = FrameworkInfo("ScalaFramework")

  val scheduler = new ScalaScheduler

  val mesosURL = Properties.envOrElse("MASTER_ZK_PATH", "zk://172.17.0.3:2181/mesos")

  val driver = new MesosSchedulerDriver(scheduler, framework.toProto, mesosURL)
  driver.start()
  driver.join()
}
