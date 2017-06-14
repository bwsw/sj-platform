/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.si

import com.bwsw.sj.common.utils.MessageResourceUtils
import org.everit.json.schema.loader.SchemaLoader
import org.json.{JSONObject, JSONTokener}
import scaldi.Injectable.inject
import scaldi.Injector

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
  def validateWithSchema(json: String, schema: String)(implicit injector: Injector): Boolean = {
    val messageResourceUtils = inject[MessageResourceUtils]
    import messageResourceUtils.createMessage

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
