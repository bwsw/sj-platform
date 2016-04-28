package com.bwsw.sj.common.entities

import org.mongodb.morphia.annotations.{Entity, Id, Reference}

@Entity("streams")
class SjStream() {
  @Id var name: String = null
  var description: String = null
  var partitions: Array[String] = null
  @Reference() var service: Service = null
  var tags: String = null
  var generator: Array[String] = null
}
