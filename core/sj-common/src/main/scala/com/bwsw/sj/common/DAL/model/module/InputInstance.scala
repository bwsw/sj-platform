package com.bwsw.sj.common.DAL.model.module

import java.util

import com.bwsw.sj.common.rest.entities.module.{InputInstanceMetadata, InstanceMetadata}
import org.mongodb.morphia.annotations.Property
import scala.collection.JavaConverters._

/**
 * Entity for input instance-json
 *
 * @author Kseniya Tomskikh
 */
class InputInstance extends Instance {
  @Property("duplicate-check") var duplicateCheck: Boolean = false
  @Property("lookup-history") var lookupHistory: Int = 0
  @Property("queue-max-size") var queueMaxSize: Int = 0
  @Property("default-eviction-policy") var defaultEvictionPolicy: String = null
  @Property("eviction-policy") var evictionPolicy: String = null
  @Property("backup-count") var backupCount: Int = 0
  @Property("async-backup-count") var asyncBackupCount: Int = 0
  var tasks: java.util.Map[String, InputTask] = new util.HashMap()

  override def toProtocolInstance(): InstanceMetadata = {
    val protocolInstance = new InputInstanceMetadata()
    super.fillProtocolInstance(protocolInstance)

    protocolInstance.outputs = this.outputs
    protocolInstance.defaultEvictionPolicy = this.defaultEvictionPolicy
    protocolInstance.evictionPolicy = this.evictionPolicy
    protocolInstance.lookupHistory = this.lookupHistory
    protocolInstance.queueMaxSize = this.queueMaxSize
    protocolInstance.tasks = Map(this.tasks.asScala.toList: _*)

    protocolInstance
  }
}
