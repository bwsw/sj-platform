package com.bwsw.sj.common.entities

import org.mongodb.morphia.annotations.{Entity, Id}

@Entity("providers")
class Provider {
  @Id var name: String = null
  var description: String = null
  var hosts: List[String] = null
  var login: String = null
  var password: String = null
  var `provider-type`: String = null
}
