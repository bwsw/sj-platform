package com.bwsw.sj.common

import java.util.UUID

/**
 * Object with constants
 */
object ModuleConstants {
  val persistentQueuePath = UUID.randomUUID().toString //todo: until t-strems can't remove persistent queue
  val persistentBlockingQueue = "persistentBlockingQueue"

  val windowedType = "windowed-streaming"
  val regularStreamingType = "regular-streaming"

  val moduleTypes = Set(windowedType, regularStreamingType)
  val checkpointModes = Set("every-nth", "time-interval")
  val stateManagementModes = Set("none", "ram", "rocks")
  val startFromModes = Set("oldest", "newest")

  val ready = "ready"
  val started = "started"
  val stopped = "stopped"
  val instanceStatusModes = Set(started, stopped, ready)

}

object StreamConstants {
  val streamTypes = Set("Tstream", "kafka")
}

object GeneratorConstants {
  val generatorTypes = Set("global", "local", "per-stream")
  val generatorTypesWithService = Set("global", "per-stream")
}

object ServiceConstants {
  val serviceTypes = Set("CassDB", "ESInd", "KfkQ", "TstrQ", "ZKCoord", "RdsCoord", "ArspkDB")
  val serviceTypesWithProvider = Set("CassDB", "ESInd", "KfkQ", "ZKCoord", "RdsCoord", "ArspkDB")
  val serviceTypeProviders = Map(
    "CassDB" -> "cassandra",
    "ESInd" -> "ES",
    "KfkQ" -> "kafka",
    "ZKCoord" -> "zookeeper",
    "RdsCoord" -> "redis",
    "ArspkDB" -> "aerospike"
  )
}
