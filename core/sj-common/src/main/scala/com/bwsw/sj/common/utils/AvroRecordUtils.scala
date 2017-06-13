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
package com.bwsw.sj.common.utils

import com.bwsw.common.JsonSerializer
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData.Record

/**
  * Utils for [[Record]]
  *
  * @author Pavel Tomskikh
  */
object AvroRecordUtils {

  private val serializer = new JsonSerializer(true)

  /**
    * Returns concatenated fields values from record.
    *
    * @param fieldNames field names
    * @param record     provides fields with their values
    * @return
    */
  def concatFields(fieldNames: Seq[String], record: Record): String =
    fieldNames.foldLeft("") { (acc, field) => acc + "," + record.get(field).toString }

  def jsonToSchema(json: String): Option[Schema] = {
    Option(json) match {
      case None | Some("{}") => None
      case Some(s) =>
        val parser = new Schema.Parser()
        Option(parser.parse(s))
    }
  }

  def schemaToJson(schema: Option[Schema]): String =
    schema.map(_.toString).getOrElse("{}")

  def mapToSchema(map: Map[String, Any]): Option[Schema] = {
    if (map.isEmpty) None
    else {
      val schemaJson = serializer.serialize(map)
      val parser = new Schema.Parser()
      Option(parser.parse(schemaJson))
    }
  }

  def schemaToMap(schema: Option[Schema]): Map[String, Any] =
    schema.map(_.toString).map(serializer.deserialize[Map[String, Any]]).getOrElse(Map())
}
