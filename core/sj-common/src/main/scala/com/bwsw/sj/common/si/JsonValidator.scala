package com.bwsw.sj.common.si

import com.bwsw.sj.common.utils.MessageResourceUtils._
import org.everit.json.schema.loader.SchemaLoader
import org.json.{JSONObject, JSONTokener}

import scala.util.Try

/**
  * Provides helping methods for JSON validation
  */
trait JsonValidator {

  def isEmptyOrNullString(value: String): Boolean = Option(value) match {
    case Some("") | None => true
    case _ => false
  }

  /**
    * Indicates that JSON-formatted string is valid
    *
    * @param json JSON-formatted string
    */
  def isJSONValid(json: String): Boolean =
    Try(new JSONObject(json)).isSuccess

  /**
    * Indicates that JSON-formatted string corresponds to JSON-schema
    *
    * @param json   JSON-formatted string
    * @param schema JSON-schema
    */
  def validateWithSchema(json: String, schema: String): Boolean = {
    Option(getClass.getClassLoader.getResourceAsStream(schema)) match {
      case Some(schemaStream) =>
        val rawSchema = new JSONObject(new JSONTokener(schemaStream))
        val schema = SchemaLoader.load(rawSchema)
        val specification = new JSONObject(json)
        schema.validate(specification)
        true
      case None =>
        throw new Exception(createMessage("json.schema.not.found"))
    }
  }
}
