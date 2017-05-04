package com.bwsw.sj.common.DAL.model.module

import org.apache.avro.Schema
import org.mongodb.morphia.annotations.Property

/**
  * @author Pavel Tomskikh
  */
trait AvroSchemaForInstance {
  @Property("input-avro-schema") var inputAvroSchema: String = _

  def getInputAvroSchema: Option[Schema] = {
    val schemaParser = new Schema.Parser()
    Option(inputAvroSchema) match {
      case Some("{}") | None => None
      case _ => Some(schemaParser.parse(inputAvroSchema))
    }
  }
}
