package com.bwsw.sj.common.rest.entities.module

import com.bwsw.common.JsonSerializer
import org.apache.avro.Schema

/**
  * @author Pavel Tomskikh
  */
trait AvroSchemaForInstanceMetadata {
  var inputAvroSchema: Option[Map[String, Any]] = None

  def validateAvroSchema: Boolean = {
    val schemaParser = new Schema.Parser()
    val serializer = new JsonSerializer()
    inputAvroSchema match {
      case Some(s) => try {
        schemaParser.parse(serializer.serialize(s))
        true
      } catch {
        case _: Throwable => false
      }
      case None => true
    }
  }
}
