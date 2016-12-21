package com.bwsw.sj.common.rest.entities.module

import com.fasterxml.jackson.annotation.JsonProperty


class BatchFillType {
  @JsonProperty("type-name") var typeName: String = null
  var value: Long = Long.MinValue
}
