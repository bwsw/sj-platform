package com.bwsw.sj.common.entities

import com.bwsw.common.DAL.Entity

/**
  * Entity for base instance-json
  * Created:  13/04/2016
  * @author Kseniya Tomskikh
  */
abstract class InstanceMetadata extends Entity {
  var uuid: String
  var moduleType: String
  var moduleName: String
  var moduleVersion: String
  var status: String
  var name: String
  var description: String
  var inputs: List[String]
  var outputs: List[String]
  var checkpointMode: String
  var checkpointInterval: Long
  var stateManagement: String
  var stateFullCheckpoint: Int
  var parallelism: Any
  var options: Map[String, Any]
  var startFrom: String
  var perTaskCores: Int
  var perTaskRam: Int
  var jvmOptions: Map[String, Any]
  var executionPlan: ExecutionPlan
  var tags: String
  var idle: Long
}



