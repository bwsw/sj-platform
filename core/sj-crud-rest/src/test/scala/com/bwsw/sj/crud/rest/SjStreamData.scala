package com.bwsw.sj.crud.rest

import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo, JsonProperty}

/**
  * Stream data case class
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "stream-type")
@JsonSubTypes(Array(new Type(value = classOf[CassStream], name = "cassandra"),
  new Type(value = classOf[TestStream], name = "test")
))
class SjStreamTest {
  var name: String = null
  var description: String = "No description"
  @JsonProperty("stream-type") var streamType: String = null
}

class CassStream extends  SjStreamTest {
  var keyspace: String = null
}

class TestStream extends SjStreamTest {
  var ttt: Int = 0
}

