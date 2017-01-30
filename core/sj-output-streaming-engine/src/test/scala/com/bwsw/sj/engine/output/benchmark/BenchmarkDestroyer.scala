package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.TempHelperForConfigDestroy
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._

/**
 *
 *
 * @author Kseniya Tomskikh
  *
  *         MONGO_HOST=176.120.25.19:27017
  *         AGENTS_HOST=176.120.25.19
  *         AGENTS_PORTS=31000,31001
  *         CASSANDRA_HOSTS=176.120.25.19:9042
  *         ZOOKEEPER_HOSTS=176.120.25.19:2181
  *         ES_HOSTS=176.120.25.19:9300
  *         JDBC_HOSTS=0.0.0.0:5432
 */
object BenchmarkDestroyer extends App {
  open()
  val instanceName: String = "test-es-bench-instance"
  val instanceNameJDBC: String = "test-jdbc-bench-instance"
  val module = new File("./contrib/stubs/sj-stub-output/target/scala-2.11/sj-stub-output-1.0-SNAPSHOT.jar")
  val jdbcModule = new File("./contrib/stubs/sj-stub-output-jdbc/target/scala-2.11/sj-stub-output-jdbc-1.0-SNAPSHOT.jar")

  clearEsStream()
  try {clearDatabase()} catch {case e: Exception => e}
  deleteStreams()
  deleteServices()
  deleteProviders()
  deleteInstance(instanceName)
  deleteInstance(instanceNameJDBC)
  deleteModule(module.getName)
  deleteModule(jdbcModule.getName)
  cassandraDestroy("bench")
  close()
  TempHelperForConfigDestroy.main(Array())
  ConnectionRepository.close()

  println("DONE")
}
