package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._

/**
 *
 *
 * @author Kseniya Tomskikh
 */
object BenchmarkDataPrepare extends App {

  val instanceName: String = "test-bench-instance"
  val checkpointInterval = 3
  val checkpointMode = EngineLiterals.everyNthMode
  val partitions = 4

  val module = new File("./contrib/stubs/sj-stub-output/target/scala-2.11/sj-stub-output-1.0-SNAPSHOT.jar")

  println("module upload")
  uploadModule(module)
  open()
  println("cassandra prepare")
  prepareCassandra("bench")
  println("database prepare")
  prepareDatabase()
  println("create providers")
  createProviders()
  println("create services")
  createServices()
  println("create streams")
  createStreams(partitions)
  println("create instance")
  createInstance(instanceName, checkpointMode, checkpointInterval)

  println
  createIndex()
  createTable()

  println("create test data")
  createData(50, 20)

  println("close connections")
  close()
  ConnectionRepository.close()

  println("DONE")

}