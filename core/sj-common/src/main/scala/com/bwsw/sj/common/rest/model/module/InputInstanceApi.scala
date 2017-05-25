package com.bwsw.sj.common.rest.model.module

import com.bwsw.sj.common.dal.model.instance.{InputInstanceDomain, InputTask}
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals

import scala.collection.JavaConverters._

/**
  * API entity for [[EngineLiterals.inputStreamingType]] instance
  */
class InputInstanceApi extends InstanceApi {
  var outputs: Array[String] = Array()
  var checkpointMode: String = _
  var checkpointInterval: Long = Long.MinValue
  var duplicateCheck: Boolean = false
  var lookupHistory: Int = Int.MinValue
  var queueMaxSize: Int = Int.MinValue
  var defaultEvictionPolicy: String = EngineLiterals.noneDefaultEvictionPolicy
  var evictionPolicy: String = EngineLiterals.fixTimeEvictionPolicy
  var backupCount: Int = 0
  var asyncBackupCount: Int = 0
  var tasks: Map[String, InputTask] = Map()

  override def asModelInstance(): InputInstanceDomain = {
    val serviceRepository = ConnectionRepository.getServiceRepository
    val service = serviceRepository.get(this.coordinationService).get.asInstanceOf[ZKServiceDomain]

    val modelInstance = new InputInstanceDomain(name, moduleType, moduleName, moduleVersion, engine, service, checkpointMode)
    super.fillModelInstance(modelInstance)
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
                               engineVersion: String): Unit = {
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    fillTasks()
  }

  override def createStreams(): Unit = {
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
