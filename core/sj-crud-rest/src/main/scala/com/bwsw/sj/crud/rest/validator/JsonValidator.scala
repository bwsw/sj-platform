package com.bwsw.sj.crud.rest.validator

import com.bwsw.sj.common.utils.MessageResourceUtils._
import org.everit.json.schema.loader.SchemaLoader
import org.json.{JSONException, JSONObject, JSONTokener}

trait JsonValidator {

  def isEmptyOrNullString(value: String): Boolean = Option(value) match {
    case Some("") | None => true
    case _ => false
  }

  def isJSONValid(json: String): Boolean = {
    try {
      new JSONObject(json)
    } catch {
      case ex: JSONException => return false
    }
    true
  }

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
