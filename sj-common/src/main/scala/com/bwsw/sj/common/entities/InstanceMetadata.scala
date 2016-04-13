package com.bwsw.sj.common.entities

import com.bwsw.common.DAL.Entity

/**
  * Entity for base instance-json
  * Created:  13/04/2016
  * @author Kseniya Tomskikh
  */
abstract class InstanceMetadata extends Entity {
  var name: String
  var uuid: String
  var description: String
  var inputs: List[String]
  var outputs: List[String]
  var checkpointMode: String
  var checkpointInterval: Int
  var stateManagement: String
  var checkpointFullInterval: Int
  var parallelism: Int
  var options: Map[String, Any]
  var startFrom: Any
}



