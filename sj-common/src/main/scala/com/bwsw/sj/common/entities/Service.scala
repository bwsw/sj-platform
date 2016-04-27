package com.bwsw.sj.common.entities

import org.mongodb.morphia.annotations.{Entity, Id, Reference}

@Entity("services")
class Service() {
  @Id var name: String = null
  var `type`: String = null
  var description: String = null
  @Reference() var provider = null
}
