package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.{InputInstance, InputTask}
import com.bwsw.sj.common.utils.EngineLiterals
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable

class InputInstanceMetadata extends InstanceMetadata {
  var outputs: Array[String] = Array()
  @JsonProperty("duplicate-check") var duplicateCheck: Boolean = false
  @JsonProperty("lookup-history") var lookupHistory: Int = 0
  @JsonProperty("queue-max-size") var queueMaxSize: Int = 0
  @JsonProperty("default-eviction-policy") var defaultEvictionPolicy: String = EngineLiterals.noneDefaultEvictionPolicy
  @JsonProperty("eviction-policy") var evictionPolicy: String = EngineLiterals.fixTimeEvictionPolicy
  @JsonProperty("backup-count") var backupCount: Int = 0
  @JsonProperty("async-backup-count") var asyncBackupCount: Int = 0
  var tasks: Map[String, InputTask] = null

  override def asModelInstance() = {
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

  override def fillInstance(moduleType: String,
                            moduleName: String,
                            moduleVersion: String,
                            engineName: String,
                            engineVersion: String) = {
    val instance = super.fillInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion).asInstanceOf[InputInstance]

    fillTasks(instance)
    val stages = createStages(this.outputs)
    instance.stages = mapAsJavaMap(stages)

    instance
  }

  override def createStreams() = {
    val sjStreams = getStreams(this.outputs)
    sjStreams.foreach(_.create())
  }

  /**
   * Create tasks object for instance of input module
   *
   * @param instance - instance for input module
   */
  private def fillTasks(instance: InputInstance): Unit = {
    val tasks = mutable.Map[String, InputTask]()

    for (i <- 0 until instance.parallelism) {
      val task = new InputTask("", 0)
      tasks.put(s"${instance.name}-task$i", task)
    }
    instance.tasks = mapAsJavaMap(tasks)
  }
}
