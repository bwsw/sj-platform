package com.bwsw.sj.common._dal.model.module

import org.apache.avro.Schema
import org.mongodb.morphia.annotations.Property

/**
  * @author Pavel Tomskikh
  */
trait AvroSchemaForInstance {
  @Property("input-avro-schema") var inputAvroSchema: String = "{}"

  def getInputAvroSchema: Option[Schema] = {
    val schemaParser = new Schema.Parser()
    if (inputAvroSchema != null && inputAvroSchema != "{}") Some(schemaParser.parse(inputAvroSchema))
    else None
  }
}
