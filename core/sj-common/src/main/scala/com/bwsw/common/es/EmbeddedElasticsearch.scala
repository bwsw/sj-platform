package com.bwsw.common.es

import java.nio.file.Files
import java.util.Collections

import org.apache.commons.io.FileUtils
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.index.reindex.ReindexPlugin
import org.elasticsearch.node.Node
import org.elasticsearch.node.internal.InternalSettingsPreparer
import org.elasticsearch.transport.Netty4Plugin

import scala.collection.JavaConverters._
import scala.util.Try

class EmbeddedElasticsearch {
  private val clusterName = "elasticsearch"
  private val dataDir = Files.createTempDirectory("elasticsearch_data_").toFile
  private val settings = Settings.builder()
    .put("path.home", dataDir.getParent)
    .put("path.data", dataDir.toString)
    .put("cluster.name", clusterName)
    .put("transport.type", "netty4")
    .put("http.enabled", false)
    .build

  private lazy val node = new PluginNode(settings)

  def start(): Unit = {
    node.start()
  }

  def stop(): Unit = {
    node.close()
    Try(FileUtils.forceDelete(dataDir))
  }
}

//the first plugin is used for connection, the second - for deleting
class PluginNode(settings: Settings)
  extends Node(InternalSettingsPreparer.prepareEnvironment(settings, null),
    Collections.list(Iterator[Class[_ <: org.elasticsearch.plugins.Plugin]](classOf[Netty4Plugin], classOf[ReindexPlugin]).asJavaEnumeration))
