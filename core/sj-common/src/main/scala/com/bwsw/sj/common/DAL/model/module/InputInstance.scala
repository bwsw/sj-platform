package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations.Property

/**
 * Entity for input instance-json
 * Created:  10/07/2016
 *
 * @author Kseniya Tomskikh
 */
class InputInstance extends Instance {
  @Property("lookup-history") var lookupHistory: Int = 0 //in seconds
  @Property("queue-max-size") var queueMaxSize: Int = 0
  @Property("default-eviction-policy") var defaultEvictionPolicy: String = null
  @Property("eviction-policy") var evictionPolicy: String = null //fix-time or expanded-time
  var tasks: java.util.Map[String, Int] = null
}
