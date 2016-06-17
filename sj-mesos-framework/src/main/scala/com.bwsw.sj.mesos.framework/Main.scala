package com.bwsw.sj.mesos.framework

import org.apache.mesos.MesosSchedulerDriver
import org.apache.mesos.Protos.FrameworkInfo
import scala.util.Properties

object Main extends App {

  override def main(args: Array[String]) = {
    println("SOME")
    val port = if (args.nonEmpty) args(0).toInt else 8080
    Rest.start(port)

    val framework = FrameworkInfo.newBuilder.
      setName("JugglerFramework").
      setUser("root").
      setRole("*").
      setCheckpoint(false).
      setFailoverTimeout(0.0d).
      build()

    val scheduler = new ScalaScheduler
    val master_path = Properties.envOrElse("MESOS_MASTER", "zk://127.0.0.1:2181/mesos")
    val driver = new MesosSchedulerDriver(scheduler, framework, master_path)

    driver.start()
    driver.join()
  }
}