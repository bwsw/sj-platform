package com.bwsw.sj.common.dal.model.instance

import org.apache.avro.Schema
import org.mongodb.morphia.annotations.Property

/**
  * @author Pavel Tomskikh
  */
trait InputAvroSchema {
  @Property("input-avro-schema") var inputAvroSchema: Option[String] = None

  def getInputAvroSchema: Option[Schema] = {
    val schemaParser = new Schema.Parser()
    inputAvroSchema.map(s => schemaParser.parse(s))
  }
}
