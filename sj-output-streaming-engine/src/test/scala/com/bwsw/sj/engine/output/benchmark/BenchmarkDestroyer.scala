package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._

/**
  * Created: 6/20/16
  *
  * @author Kseniya Tomskikh
  */
object BenchmarkDestroyer extends App {
  val instanceName: String = "test-bench-instance"
  val module = new File("/home/mikhaleva_ka/Juggler/sj-stub-module/target/scala-2.11/sj-stub-module-test.jar")

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
