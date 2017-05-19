package com.bwsw.sj.engine.core.testutils

import com.bwsw.tstreamstransactionserver.options.CommonOptions.ZookeeperOptions
import com.bwsw.tstreamstransactionserver.options.ServerBuilder
import com.bwsw.tstreamstransactionserver.options.ServerOptions.{AuthOptions, BootstrapOptions, CommitLogOptions, StorageOptions}
import com.google.common.io.Files

/**
  * TTS server to launch benchmarks
  */
object TestStorageServer {

  private val serverBuilder = new ServerBuilder()

  private def getTmpDir(): String = Files.createTempDir().toString

  val token = "token"
  val prefix = "/bench-prefix"

  def start(): Unit = {
    val transactionServer = serverBuilder.withZookeeperOptions(ZookeeperOptions(endpoints = System.getenv("ZOOKEEPER_HOSTS"), prefix))
      .withBootstrapOptions(BootstrapOptions("192.168.1.174"))
      .withAuthOptions(AuthOptions(token))
      .withServerStorageOptions(StorageOptions(path = getTmpDir()))
      .withCommitLogOptions(CommitLogOptions(commitLogCloseDelayMs = 100))
      .build()

    println("START SERVER")
    transactionServer.start()
  }
}

object Server extends App {
  TestStorageServer.start()
}