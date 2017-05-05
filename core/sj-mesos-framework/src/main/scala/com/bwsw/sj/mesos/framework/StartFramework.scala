package com.bwsw.sj.mesos.framework

import java.net.URI

import com.bwsw.common.LeaderLatch
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.ConfigurationSetting
import com.bwsw.sj.mesos.framework.rest.Rest
import com.bwsw.sj.mesos.framework.schedule.FrameworkScheduler
import org.apache.mesos.MesosSchedulerDriver
import org.apache.mesos.Protos.{Credential, FrameworkInfo}

import scala.util.Properties


object StartFramework {

  val frameworkName = "JugglerFramework"
  val frameworkUser =  Properties.envOrElse("FRAMEWORK_USER", "root")
  val frameworkCheckpoint = false
  val frameworkFailoverTimeout = 0.0d
  val frameworkRole = "*"

  val master_path = Properties.envOrElse("MESOS_MASTER", "zk://127.0.0.1:2181/mesos")
  val frameworkTaskId = Properties.envOrElse("FRAMEWORK_ID", "broken")

  /**
    * Main function to start rest and framework.
    * @param args exposed port for rest service
    */
  def main(args: Array[String]): Unit = {
    val port = if (args.nonEmpty) args(0).toInt else 8080
    val restThread = Rest.start(port)

    val frameworkInfo = FrameworkInfo.newBuilder.
      setName(frameworkName).
      setUser(frameworkUser).
      setCheckpoint(frameworkCheckpoint).
      setFailoverTimeout(frameworkFailoverTimeout).
      setRole(frameworkRole).
      setPrincipal("sherman").
      build()

    val scheduler = new FrameworkScheduler

    val frameworkPrincipal: Option[ConfigurationSetting] = ConnectionRepository.getConfigService.get(ConfigLiterals.frameworkPrincipalTag)
    val frameworkSecret: Option[ConfigurationSetting] = ConnectionRepository.getConfigService.get(ConfigLiterals.frameworkSecretTag)
    var credential: Option[Credential] = None

    if (frameworkPrincipal.isDefined && frameworkSecret.isDefined) {
      credential = Some(Credential.newBuilder.
        setPrincipal(frameworkPrincipal.get.value).
        setSecret(frameworkSecret.get.value).
        build())
    }



    val driver: MesosSchedulerDriver = {
      if (credential.isDefined) new MesosSchedulerDriver(scheduler, frameworkInfo, master_path, credential.get)
      else new MesosSchedulerDriver(scheduler, frameworkInfo, master_path)
    }



    val zkServers: String = getZooKeeperServers(master_path)
    val leader: LeaderLatch = new LeaderLatch(Set(zkServers), s"/framework/$frameworkTaskId/lock")
    leader.start()
    leader.takeLeadership(5)


    driver.start()
    driver.join()
    restThread.join()

    leader.close()
    System.exit(0)
  }


  private def getZooKeeperServers(marathonMaster: String): String = {
    val marathonMasterUrl = new URI(marathonMaster)
    marathonMasterUrl.getHost + ":" + marathonMasterUrl.getPort
  }
}