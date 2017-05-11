package com.bwsw.sj.common.rest.model.module

import com.bwsw.common.JsonSerializer
import org.apache.avro.Schema

import scala.util.Try

/**
  * @author Pavel Tomskikh
  */
trait AvroSchemaForInstanceMetadata {
  var inputAvroSchema: Option[Map[String, Any]] = None

  def validateAvroSchema: Boolean = {
    val schemaParser = new Schema.Parser()
    val serializer = new JsonSerializer()
    inputAvroSchema.forall { s =>
      Try(schemaParser.parse(serializer.serialize(s))).isSuccess
    }
  }
}
