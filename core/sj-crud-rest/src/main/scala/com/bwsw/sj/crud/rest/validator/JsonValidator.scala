package com.bwsw.sj.crud.rest.validator

import com.bwsw.sj.common.utils.MessageResourceUtils
import org.everit.json.schema.loader.SchemaLoader
import org.json.{JSONException, JSONObject, JSONTokener}

trait JsonValidator extends MessageResourceUtils {

  def isEmptyOrNullString(value: String): Boolean = value == null || value.isEmpty

  def isJSONValid(json: String): Boolean = {
    try {
      new JSONObject(json)
    } catch {
      case ex: JSONException => return false
    }
    true
  }

  def validateWithSchema(json: String, schema: String): Boolean = {
    val schemaStream = getClass.getClassLoader.getResourceAsStream(schema)
    if (schemaStream != null) {
      val rawSchema = new JSONObject(new JSONTokener(schemaStream))
      val schema = SchemaLoader.load(rawSchema)
      val specification = new JSONObject(json)
      schema.validate(specification)
      true
    } else {
      throw new Exception(createMessage("json.schema.not.found"))
    }
  }
}
