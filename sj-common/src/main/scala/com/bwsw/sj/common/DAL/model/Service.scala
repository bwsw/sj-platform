package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.{Entity, Id, Property}

@Entity("services")
class Service() {
  @Id var name: String = null
  @Property("type") var serviceType: String = null
  var description: String = null
}
