package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations._


@Entity("streams")
class SjStream() {
  @Id var name: String = null
  var description: String = null
  var partitions: Int = 0
  @Reference var service: Service = null
  @Property("stream-type") var streamType: String = null
  var tags: String = null
  @Embedded var generator: Generator = null
}
