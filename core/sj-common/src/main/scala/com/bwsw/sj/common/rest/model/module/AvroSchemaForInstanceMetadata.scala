package com.bwsw.sj.common.rest.model.module

import com.bwsw.common.JsonSerializer
import org.apache.avro.Schema

import scala.util.Try

/**
  * @author Pavel Tomskikh
  */
trait AvroSchemaForInstanceMetadata {
  var inputAvroSchema: Map[String, Any] = Map()

  def validateAvroSchema: Boolean = {
    val schemaParser = new Schema.Parser()
    val serializer = new JsonSerializer()
    Option(inputAvroSchema) match {
      case Some(s) if s.nonEmpty =>
        Try(schemaParser.parse(serializer.serialize(s))).isSuccess
      case _ => true
    }
  }
}
