package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations.Property

/**
 * Entity for input instance-json
 * Created:  10/07/2016
 *
 * @author Kseniya Tomskikh
 */
class InputInstance extends Instance {
  @Property("lookup-history") var lookupHistory: Long = 0
  @Property("queue-max-size") var queueMaxSize: Long = 0
  @Property("eviction-policy") var evictionPolicy: String = null
}
