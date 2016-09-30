package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._

/**
 *
 *
 * @author Kseniya Tomskikh
 */
object BenchmarkDestroyer extends App {
  open()
  val instanceName: String = "test-bench-instance"
  val module = new File(getClass.getClassLoader.getResource("sj-stub-output-bench-test.jar").getPath)

  clearEsStream()
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
