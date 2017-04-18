package com.bwsw.sj.engine.core.testutils

import com.bwsw.tstreamstransactionserver.options.CommonOptions.ZookeeperOptions
import com.bwsw.tstreamstransactionserver.options.ServerBuilder
import com.bwsw.tstreamstransactionserver.options.ServerOptions.{AuthOptions, BootstrapOptions, CommitLogOptions, StorageOptions}
import com.google.common.io.Files

object TestStorageServer {

  private val serverBuilder = new ServerBuilder()

  private def getTmpDir(): String = Files.createTempDir().toString

  val token = "token"

  def start() = {
    val transactionServer = serverBuilder.withZookeeperOptions(new ZookeeperOptions(endpoints = System.getenv("ZOOKEEPER_HOSTS"), "/masha-test"))
      .withBootstrapOptions(new BootstrapOptions("192.168.1.174"))
      .withAuthOptions(new AuthOptions(token))
      .withServerStorageOptions(new StorageOptions(path = "/tmp/masha-test"))
      .withCommitLogOptions(new CommitLogOptions(commitLogCloseDelayMs = 100))
      .build()

    println("START SERVER")
    transactionServer.start()
  }
}

object Server extends App {
  TestStorageServer.start()
}