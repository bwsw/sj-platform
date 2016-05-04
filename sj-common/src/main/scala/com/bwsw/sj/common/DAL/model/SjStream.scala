package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.{Embedded, Entity, Id, Reference}

@Entity("streams")
class SjStream() {
  @Id var name: String = null
  var description: String = null
  var partitions: Int = 0
  @Reference var service: Service = null
  var tags: String = null
  @Embedded("generator") var generator: Generator = null
}
