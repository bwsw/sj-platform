package com.bwsw.sj.common.entities

import org.mongodb.morphia.annotations.{Property, Entity, Id, Reference}

@Entity("services")
class Service() {
  @Id var name: String = null
  @Property("type") var serviceType: String = null
  var description: String = null
  @Reference() var provider: Provider = null
}
