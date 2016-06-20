package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._

/**
  * Created: 17/06/2016
  *
  * @author Kseniya Tomskikh
  */
object BenchmarkDataPrepare extends App {

  val instanceName: String = "test-bench-instance"
  val checkpointInterval = 5
  val checkpointMode = "every-nth"
  val partitions = 5

  val module = new File("/home/tomskikh_ka/work/Juggler/sj-stub-output-module/target/scala-2.11/sj-stub-output-bench-test.jar")
  println("module upload")
  uploadModule(module)

  println("cassandra prepare")
  prepareCassandra("bench")
  Thread.sleep(30000)
  println("create providers")
  createProviders()
  println("create services")
  createServices()
  println("create streams")
  createStreams(partitions)
  println("create instance")
  createInstance(instanceName, checkpointMode, checkpointInterval)

  println("create test data")
  Thread.sleep(30000)
  createData(12, 100)

  println("close connections")
  close()
  ConnectionRepository.close()

  println("DONE")

}