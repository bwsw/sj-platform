package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.{InputInstance, InputTask}
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.JavaConverters._

class InputInstanceMetadata extends InstanceMetadata {
  var outputs: Array[String] = Array()
  @JsonProperty("duplicate-check") var duplicateCheck: Boolean = false
  @JsonProperty("lookup-history") var lookupHistory: Int = 0
  @JsonProperty("queue-max-size") var queueMaxSize: Int = 0
  @JsonProperty("default-eviction-policy") var defaultEvictionPolicy: String = "NONE"
  @JsonProperty("eviction-policy") var evictionPolicy: String = "fix-time"
  @JsonProperty("backup-count") var backupCount: Int = 0
  @JsonProperty("async-backup-count") var asyncBackupCount: Int = 0
  var tasks: Map[String, InputTask] = null

  override def toModelInstance() = {
    val modelInstance = new InputInstance()
    super.fillModelInstance(modelInstance)
    modelInstance.outputs = this.outputs
    modelInstance.duplicateCheck = this.duplicateCheck
    modelInstance.defaultEvictionPolicy = this.defaultEvictionPolicy
    modelInstance.evictionPolicy = this.evictionPolicy
    modelInstance.lookupHistory = this.lookupHistory
    modelInstance.queueMaxSize = this.queueMaxSize
    modelInstance.tasks = this.tasks.asJava
    modelInstance.backupCount = this.backupCount
    modelInstance.asyncBackupCount = this.asyncBackupCount

    modelInstance
  }
}
