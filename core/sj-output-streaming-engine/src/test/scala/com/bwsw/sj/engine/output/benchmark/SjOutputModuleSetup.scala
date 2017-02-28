package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.TempHelperForConfigSetup
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.output.benchmark.DataFactory._

/**
  *
  *
  * @author Kseniya Tomskikh
  *
  *         Environment for Prepare
  *         MONGO_HOSTS=176.120.25.19:27017
  *         AGENTS_HOST=176.120.25.19
  *         AGENTS_PORTS=31000,31001
  *         CASSANDRA_HOSTS=176.120.25.19:9042
  *         ZOOKEEPER_HOSTS=176.120.25.19:2181
  *         ES_HOSTS=176.120.25.19:9300
  *         JDBC_HOSTS=176.120.25.19:5432  -postgresql
  */
object SjESOutputModuleSetup extends App {
  TempHelperForConfigSetup.main(Array())
  val checkpointInterval = 3
  val checkpointMode = EngineLiterals.everyNthMode
  val partitions = 4

  val module = new File("./contrib/stubs/sj-stub-output/target/scala-2.12/sj-stub-output-1.0-SNAPSHOT.jar")

  println("module upload")
  uploadModule(module)
  open()
  println("cassandra prepare")
  prepareCassandra()
  println("create providers")
  createProviders()
  println("create services")
  createServices()
  println("create streams")
  createStreams(partitions)
  println("create instance")
  createInstance(esInstanceName, checkpointMode, checkpointInterval,
    esStreamName, "com.bwsw.stub.output-bench-test")

  println("prepare a storage")
  createIndex()

  println("create test data")
  createData(50, 20)

  println("close connections")
  close()
  ConnectionRepository.close()

  println("DONE")

}

object SjJDBCOutputModuleSetup extends App {
  TempHelperForConfigSetup.main(Array())
  val checkpointInterval = 3
  val checkpointMode = EngineLiterals.everyNthMode
  val partitions = 4

  val jdbcModule = new File("./contrib/stubs/sj-stub-output-jdbc/target/scala-2.12/sj-stub-output-jdbc-1.0-SNAPSHOT.jar")

  println("module upload")
  uploadModule(jdbcModule)
  open()
  println("cassandra prepare")
  prepareCassandra()
  println("create providers")
  createProviders()
  println("create services")
  createServices()
  println("create streams")
  createStreams(partitions)
  println("create instance")

  createInstance(jdbcInstanceName, checkpointMode, checkpointInterval,
    jdbcStreamName, "com.bwsw.stub.output-bench-test-jdbc")

  println("Prepare a storage")
  createTable()

  println("create test data")
  createData(50, 20)

  println("close connections")
  close()
  ConnectionRepository.close()

  println("DONE")

}