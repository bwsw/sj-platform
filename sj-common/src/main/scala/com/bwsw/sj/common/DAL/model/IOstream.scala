package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Embedded

@Embedded
class IOstream {
  val cardinality: Array[Int] = null
  val types: Array[String] = null
}
