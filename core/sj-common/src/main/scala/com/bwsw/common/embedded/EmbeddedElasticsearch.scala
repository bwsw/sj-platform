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
package com.bwsw.common.embedded

import java.nio.file.Files
import java.util.Collections

import org.apache.commons.io.FileUtils
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.index.reindex.ReindexPlugin
import org.elasticsearch.node.Node
import org.elasticsearch.node.InternalSettingsPreparer
import org.elasticsearch.transport.Netty4Plugin

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Used for testing purposes only. Single node es cluster
  */
class EmbeddedElasticsearch(val port: Int) {
  private val clusterName = "elasticsearch"
  private val dataDir = Files.createTempDirectory("elasticsearch_data_").toFile
  private val settings = Settings.builder()
    .put("path.home", dataDir.getParent)
    .put("path.data", dataDir.toString)
    .put("cluster.name", clusterName)
    .put("transport.type", "netty4")
    .put("http.enabled", false)
    .put("transport.tcp.port", port)
    .build

  private lazy val node = new PluginNode(settings)

  def start(): Unit = {
    node.start()
  }

  def stop(): Unit = {
    if (!node.isClosed) node.close()
    Try(FileUtils.forceDelete(dataDir))
  }
}

/**
  * The first plugin is used for connection, the second - for deleting
  */
class PluginNode(settings: Settings)
  extends Node(InternalSettingsPreparer.prepareEnvironment(settings, null),
    Collections.list(Iterator[Class[_ <: org.elasticsearch.plugins.Plugin]](classOf[Netty4Plugin], classOf[ReindexPlugin]).asJavaEnumeration))
