package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.TempHelperForConfigDestroy
import com.bwsw.sj.engine.output.benchmark.DataFactory._

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
object SjESOutputModuleDestroy extends App {
  open()

  val module = new File("./contrib/stubs/sj-stub-output/target/scala-2.12/sj-stub-output-1.0-SNAPSHOT.jar")

  clearEsStream()
  deleteStreams()
  deleteServices()
  deleteProviders()
  deleteInstance(esInstanceName)
  deleteModule(module.getName)
  cassandraDestroy(cassandraTestKeyspace)
  close()
  TempHelperForConfigDestroy.main(Array())
  ConnectionRepository.close()

  println("DONE")
}

object SjJDBCOutputModuleDestroy extends App {
  open()

  val jdbcModule = new File("./contrib/stubs/sj-stub-output-jdbc/target/scala-2.12/sj-stub-output-jdbc-1.0-SNAPSHOT.jar")

  try {clearDatabase()} catch {case e: Exception => e}
  deleteStreams()
  deleteServices()
  deleteProviders()
  deleteInstance(jdbcInstanceName)
  deleteModule(jdbcModule.getName)
  cassandraDestroy(cassandraTestKeyspace)
  close()
  TempHelperForConfigDestroy.main(Array())
  ConnectionRepository.close()

  println("DONE")
}