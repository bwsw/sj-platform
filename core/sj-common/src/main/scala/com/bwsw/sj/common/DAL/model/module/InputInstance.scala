package com.bwsw.sj.common.DAL.model.module

import java.util

import com.bwsw.sj.common.DAL.model.service.ZKService
import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.PropertyField
import com.bwsw.sj.common.rest.DTO.module.{InputInstanceData, InstanceData}
import com.bwsw.sj.common.utils.EngineLiterals
import org.mongodb.morphia.annotations.Property

import scala.collection.JavaConverters._

/**
  * Entity for input instance-json
  *
  * @author Kseniya Tomskikh
  */
class InputInstance(override val name: String,
                    override val moduleType: String,
                    override val moduleName: String,
                    override val moduleVersion: String,
                    override val engine: String,
                    override val coordinationService: ZKService,
                    @PropertyField("checkpoint-mode") val checkpointMode: String)
  extends Instance(name, moduleType, moduleName, moduleVersion, engine, coordinationService) {

  @Property("checkpoint-interval") var checkpointInterval: Long = 0
  @Property("duplicate-check") var duplicateCheck: Boolean = false
  @Property("lookup-history") var lookupHistory: Int = 0
  @Property("queue-max-size") var queueMaxSize: Int = 0
  @Property("default-eviction-policy") var defaultEvictionPolicy: String = EngineLiterals.noneDefaultEvictionPolicy
  @Property("eviction-policy") var evictionPolicy: String = EngineLiterals.fixTimeEvictionPolicy
  @Property("backup-count") var backupCount: Int = 0
  @Property("async-backup-count") var asyncBackupCount: Int = 0
  var tasks: java.util.Map[String, InputTask] = new util.HashMap()

  override def asProtocolInstance(): InstanceData = {
    val protocolInstance = new InputInstanceData()
    super.fillProtocolInstance(protocolInstance)

    protocolInstance.outputs = this.outputs
    protocolInstance.checkpointMode = this.checkpointMode
    protocolInstance.checkpointInterval = this.checkpointInterval
    protocolInstance.duplicateCheck = this.duplicateCheck
    protocolInstance.lookupHistory = this.lookupHistory
    protocolInstance.queueMaxSize = this.queueMaxSize
    protocolInstance.defaultEvictionPolicy = this.defaultEvictionPolicy
    protocolInstance.evictionPolicy = this.evictionPolicy
    protocolInstance.backupCount = this.backupCount
    protocolInstance.asyncBackupCount = this.asyncBackupCount
    protocolInstance.tasks = Map(this.tasks.asScala.toList: _*)

    protocolInstance
  }
}
