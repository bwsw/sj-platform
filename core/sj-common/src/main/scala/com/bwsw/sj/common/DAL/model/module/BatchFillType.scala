package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations.Property

class BatchFillType {
  @Property("type-name") var typeName: String = null
  var value: Long = Long.MinValue
}
