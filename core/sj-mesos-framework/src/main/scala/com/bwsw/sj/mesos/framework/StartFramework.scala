package com.bwsw.sj.mesos.framework

import java.net.URI

import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.mesos.framework.rest.Rest
import com.bwsw.sj.mesos.framework.schedule.FrameworkScheduler
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import org.apache.mesos.MesosSchedulerDriver
import org.apache.mesos.Protos.FrameworkInfo
import org.apache.mesos.Protos.Credential
import scala.util.Properties
import com.bwsw.common.LeaderLatch


object StartFramework {

  /**
    * Main function to start rest and framework.
    * @param args exposed port for rest service
    */
  def main(args: Array[String]): Unit = {
    val port = if (args.nonEmpty) args(0).toInt else 8080
    Rest.start(port)

    val framework = FrameworkInfo.newBuilder.
      setName("JugglerFramework").
      setUser("root").
      setCheckpoint(false).
      setFailoverTimeout(0.0d).
      setRole("*").
      setPrincipal("sherman").
      build()


    val frameworkPrincipal = ConnectionRepository.getConfigService.get(ConfigLiterals.frameworkPrincipalTag)
    val frameworkSecret = ConnectionRepository.getConfigService.get(ConfigLiterals.frameworkSecretTag)
    var credential: Option[Credential] = None

    if (frameworkPrincipal.isDefined && frameworkSecret.isDefined) {
      credential = Some(Credential.newBuilder.
        setPrincipal(frameworkPrincipal.get.value).
        setSecret(frameworkSecret.get.value).
        build())
    }

    val scheduler = new FrameworkScheduler
    val master_path = Properties.envOrElse("MESOS_MASTER", "zk://127.0.0.1:2181/mesos")
    val frameworkId = Properties.envOrElse("FRAMEWORK_ID", "broken")

    val driver: MesosSchedulerDriver = {
      if (credential.isDefined) new MesosSchedulerDriver(scheduler, framework, master_path, credential.get)
      else new MesosSchedulerDriver(scheduler, framework, master_path)
    }

    val zkServers = getZooKeeperServers(master_path)
    val leader = new LeaderLatch(Set(zkServers), s"/framework/$frameworkId/lock")

    leader.start()
    leader.takeLeadership(5)


    driver.start()
    driver.join()

    leader.close()
    System.exit(0)
  }


  private def getZooKeeperServers(marathonMaster: String) = {
    val marathonMasterUrl = new URI(marathonMaster)
    marathonMasterUrl.getHost + ":" + marathonMasterUrl.getPort
  }


  def uuid = java.util.UUID.randomUUID.toString

}