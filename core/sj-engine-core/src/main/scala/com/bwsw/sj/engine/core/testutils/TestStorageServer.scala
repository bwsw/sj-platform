package com.bwsw.sj.engine.core.testutils

import com.bwsw.tstreamstransactionserver.options.CommonOptions.ZookeeperOptions
import com.bwsw.tstreamstransactionserver.options.ServerBuilder
import com.bwsw.tstreamstransactionserver.options.ServerOptions.{AuthOptions, BootstrapOptions, CommitLogOptions, StorageOptions}
import com.google.common.io.Files
import com.typesafe.config.ConfigFactory

/**
  * TTS server to launch benchmarks
  */
object TestStorageServer {

  private val serverBuilder = new ServerBuilder()

  private def getTmpDir(): String = Files.createTempDir().toString

  val token = "token"
  val prefix = "/bench-prefix"
  val streamPath = "/bench-prefix/streams"
  val rootConfig = "test-storage-server"
  val zkHostsConfig = rootConfig + ".zookeeper.hosts"
  val hostConfig = rootConfig + ".host"

  def start(): Unit = {
    val config = ConfigFactory.load()
    val zkHosts = config.getString(zkHostsConfig)
    val host = config.getString(hostConfig)

    val transactionServer = serverBuilder.withZookeeperOptions(ZookeeperOptions(endpoints = zkHosts, prefix))
      .withBootstrapOptions(BootstrapOptions(host))
      .withAuthOptions(AuthOptions(token))
      .withServerStorageOptions(StorageOptions(path = getTmpDir(), streamPath))
      .withCommitLogOptions(CommitLogOptions(commitLogCloseDelayMs = 100))
      .build()

    println("START SERVER")
    transactionServer.start()
  }
}

object Server extends App {
  TestStorageServer.start()
}