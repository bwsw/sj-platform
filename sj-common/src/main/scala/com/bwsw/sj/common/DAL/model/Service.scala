package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.{Entity, Id, Property, Reference}

@Entity("services")
class Service() {
  @Id var name: String = null
  @Property("type") var serviceType: String = null
  var description: String = null
  @Reference(value = "provider", `lazy` = true) var provider: Provider = null
}
