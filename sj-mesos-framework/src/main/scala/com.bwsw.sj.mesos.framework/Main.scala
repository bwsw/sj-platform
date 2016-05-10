package com.bwsw.sj.mesos.framework

import org.apache.mesos.MesosSchedulerDriver
import org.apache.mesos.Protos.FrameworkInfo

import scala.util.Properties

object Main extends App {

  val framework = FrameworkInfo.newBuilder.
    setName("JugglerFramework").
    setUser("").
    setRole("*").
    setCheckpoint(false).
    setFailoverTimeout(0.0d).
    build()

  val scheduler = new ScalaScheduler

  val mesosURL = Properties.envOrElse("MASTER_ZK_PATH", "zk://172.17.0.3:2181/mesos")

  val driver = new MesosSchedulerDriver(scheduler, framework, mesosURL)

  driver.start()
  driver.join()
}
