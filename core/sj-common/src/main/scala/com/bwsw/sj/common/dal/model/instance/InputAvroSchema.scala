package com.bwsw.sj.common.dal.model.instance

import org.apache.avro.Schema
import org.mongodb.morphia.annotations.Property

/**
  * @author Pavel Tomskikh
  */
trait InputAvroSchema {
  @Property("input-avro-schema") var inputAvroSchema: String = "{}"

  def getInputAvroSchema: Option[Schema] = {
    val schemaParser = new Schema.Parser()
    Option(inputAvroSchema) match {
      case Some("{}") | None => None
      case Some(s) => Option(schemaParser.parse(s))
    }
  }
}
