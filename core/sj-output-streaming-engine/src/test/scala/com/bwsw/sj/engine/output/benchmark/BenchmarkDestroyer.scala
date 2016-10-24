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
  val instanceName: String = "test-es-bench-instance"
  val instanceNameJDBC: String = "test-jdbc-bench-instance"
  val module = new File("./contrib/stubs/sj-stub-output/target/scala-2.11/sj-stub-output-1.0-SNAPSHOT.jar")
  val jdbcModule = new File("./contrib/stubs/sj-stub-output-jdbc/target/scala-2.11/sj-stub-output-jdbc-1.0-SNAPSHOT.jar")

  clearEsStream()
  clearDatabase()
  deleteStreams()
  deleteServices()
  deleteProviders()
  deleteInstance(instanceName)
  deleteInstance(instanceNameJDBC)
  deleteModule(module.getName)
  deleteModule(jdbcModule.getName)
  cassandraDestroy("bench")
  close()
  ConnectionRepository.close()

  println("DONE")
}
