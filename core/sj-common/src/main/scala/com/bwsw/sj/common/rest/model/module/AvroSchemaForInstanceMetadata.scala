package com.bwsw.sj.common.rest.model.module

import com.bwsw.common.JsonSerializer
import org.apache.avro.Schema

import scala.util.Try

/**
  * Provides 'inputAvroSchema' field and method for validation it for some types of [[InstanceApi]]
  *
  * @author Pavel Tomskikh
  */
trait AvroSchemaForInstanceMetadata {
  var inputAvroSchema: Map[String, Any] = Map()

  /**
    * Indicates that [[inputAvroSchema]] is correct avro schema
    */
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
