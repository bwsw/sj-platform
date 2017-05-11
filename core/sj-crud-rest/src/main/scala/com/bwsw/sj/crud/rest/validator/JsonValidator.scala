package com.bwsw.sj.crud.rest.validator

import com.bwsw.sj.common.utils.MessageResourceUtils._
import org.everit.json.schema.loader.SchemaLoader
import org.json.{JSONObject, JSONTokener}

import scala.util.Try

trait JsonValidator {

  def isEmptyOrNullString(value: String): Boolean = Option(value) match {
    case Some("") | None => true
    case _ => false
  }

  def isJSONValid(json: String): Boolean =
    Try(new JSONObject(json)).isSuccess

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
