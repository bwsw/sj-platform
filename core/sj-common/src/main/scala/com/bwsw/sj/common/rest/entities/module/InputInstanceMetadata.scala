package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.{InputInstance, InputTask}
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.JavaConverters._

class InputInstanceMetadata extends InstanceMetadata {
  var outputs: Array[String] = Array()
  @JsonProperty("checkpoint-mode") var checkpointMode: String = _
  @JsonProperty("checkpoint-interval") var checkpointInterval: Long = Long.MinValue
  @JsonProperty("duplicate-check") var duplicateCheck: Boolean = false
  @JsonProperty("lookup-history") var lookupHistory: Int = Int.MinValue
  @JsonProperty("queue-max-size") var queueMaxSize: Int = Int.MinValue
  @JsonProperty("default-eviction-policy") var defaultEvictionPolicy: String = EngineLiterals.noneDefaultEvictionPolicy
  @JsonProperty("eviction-policy") var evictionPolicy: String = EngineLiterals.fixTimeEvictionPolicy
  @JsonProperty("backup-count") var backupCount: Int = 0
  @JsonProperty("async-backup-count") var asyncBackupCount: Int = 0
  var tasks: Map[String, InputTask] = Map()

  override def asModelInstance() = {
    val modelInstance = new InputInstance()
    super.fillModelInstance(modelInstance)
    modelInstance.checkpointMode = this.checkpointMode
    modelInstance.checkpointInterval = this.checkpointInterval
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

  override def prepareInstance(moduleType: String,
                            moduleName: String,
                            moduleVersion: String,
                            engineName: String,
                            engineVersion: String) = {
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    fillTasks()
    fillStages(this.outputs)
  }

  override def createStreams() = {
    val sjStreams = getStreams(this.outputs)
    sjStreams.foreach(_.create())
  }

  private def fillTasks(): Unit = {
    for (i <- 0 until this.parallelism.asInstanceOf[Int]) {
      val task = new InputTask()
      this.tasks += (s"${this.name}-task$i" -> task)
    }
  }
}
