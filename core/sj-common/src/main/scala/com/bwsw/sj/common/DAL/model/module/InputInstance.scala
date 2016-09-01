package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations.Property

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
  var tasks: java.util.Map[String, InputTask] = null
}
