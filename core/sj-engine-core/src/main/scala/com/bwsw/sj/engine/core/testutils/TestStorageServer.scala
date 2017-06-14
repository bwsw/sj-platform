/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
  val prefix = "/bench-prefix/master"
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