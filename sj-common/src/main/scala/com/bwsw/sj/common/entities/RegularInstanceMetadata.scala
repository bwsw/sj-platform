package com.bwsw.sj.common.entities

import org.mongodb.morphia.annotations.{Embedded, Entity, Id}

/**
 * Entity for base instance-json
 * Created:  13/04/2016
 * @author Kseniya Tomskikh
 */
@Entity("instances")
class RegularInstanceMetadata {
  var `module-type`: String = null
  var `module-name`: String = null
  var `module-version`: String = null
  var status: String = null
  @Id var name: String = null
  var description: String = null
  var inputs: List[String] = null
  var outputs: List[String] = null
  var `checkpoint-mode`: String = null
  var `checkpoint-interval`: Long = 0
  var `state-management`: String = null
  var `state-full-checkpoint`: Int = 0
  var parallelism: Any = null
  var options: Map[String, Any] = null
  var `start-from`: String = null
  var `per-task-cores`: Int = 0
  var `per-task-ram`: Int = 0
  var `jvm-options`: Map[String, Any] = null
  @Embedded var `execution-plan`: ExecutionPlan = null
  var tags: String = null
  var idle: Long = 0
}



