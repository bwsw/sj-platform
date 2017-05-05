package com.bwsw.sj.common.DAL.model.module

import org.apache.avro.Schema
import org.mongodb.morphia.annotations.Property

/**
  * @author Pavel Tomskikh
  */
trait AvroSchemaForInstance {
  @Property("input-avro-schema") var inputAvroSchema: Option[String] = None

  def getInputAvroSchema: Option[Schema] = {
    val schemaParser = new Schema.Parser()
    inputAvroSchema.map(s => schemaParser.parse(s))
  }
}
