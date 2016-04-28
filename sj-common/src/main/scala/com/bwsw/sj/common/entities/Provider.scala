package com.bwsw.sj.common.entities

import org.mongodb.morphia.annotations.{Property, Entity, Id}

@Entity("providers")
class Provider {
  @Id var name: String = null
  var description: String = null
  var hosts: Array[String] = null
  var login: String = null
  var password: String = null
  @Property("provider-type") var providerType: String = null
}
