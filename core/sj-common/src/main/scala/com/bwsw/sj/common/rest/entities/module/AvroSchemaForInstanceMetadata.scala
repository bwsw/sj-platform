package com.bwsw.sj.common.rest.entities.module

import com.bwsw.common.JsonSerializer
import org.apache.avro.Schema

/**
  * @author Pavel Tomskikh
  */
trait AvroSchemaForInstanceMetadata {
  var inputAvroSchema: Map[String, Any] = Map()

  def validateAvroSchema: Boolean = {
    val schemaParser = new Schema.Parser()
    val serializer = new JsonSerializer()
    inputAvroSchema == Map.empty || {
      try {
        schemaParser.parse(serializer.serialize(inputAvroSchema))
        true
      } catch {
        case _: Throwable => false
      }
    }
  }
}
