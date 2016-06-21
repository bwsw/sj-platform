package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._

/**
  * Created: 20/06/2016
  *
  * @author Kseniya Tomskikh
  */
object BenchmarkDestroyer extends App {
  val instanceName: String = "test-bench-instance"
  val module = new File("/home/tomskikh_ka/work/Juggler/sj-stub-output-module/target/scala-2.11/sj-stub-output-bench-test.jar")

  deleteStreams()
  deleteServices()
  deleteProviders()
  deleteInstance(instanceName)
  deleteModule(module.getName)
  cassandraDestroy("bench")

  close()
  ConnectionRepository.close()

  println("DONE")
}
