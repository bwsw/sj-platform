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
package com.bwsw.common

import java.lang.reflect.{ParameterizedType, Type}

import com.bwsw.common.exceptions.{JsonNotParsedException, _}
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature.{FAIL_ON_NULL_FOR_PRIMITIVES, FAIL_ON_UNKNOWN_PROPERTIES}
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.databind.{JsonMappingException, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Class based on jackson for json serialization.
  * Most commonly used in REST to serialize/deserialize api entities
  *
  * @param ignoreUnknown           indicates that unknown properties in JSON will be ignored
  * @param enableNullForPrimitives indicates that value of field with primitive type could contain null
  */
class JsonSerializer(ignoreUnknown: Boolean = false, enableNullForPrimitives: Boolean = true) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

  setIgnoreUnknown(ignoreUnknown)
  enableNullForPrimitives(enableNullForPrimitives)

  def serialize(value: Any): String = {
    import java.io.StringWriter

    logger.debug(s"Serialize a value of class: '${value.getClass}' to string.")
    val writer = new StringWriter()
    mapper.writeValue(writer, value)
    writer.toString
  }

  def deserialize[T: Manifest](value: String): T = {
    logger.debug(s"Deserialize a value: '$value' to object.")
    Try {
      mapper.readValue[T](value, typeReference[T])
    } match {
      case Success(entity) => entity

      case Failure(e: UnrecognizedPropertyException) =>
        throw new JsonUnrecognizedPropertyException(getProblemProperty(e))

      case Failure(e: JsonMappingException) =>
        if (e.getMessage.startsWith("No content"))
          throw new JsonIsEmptyException
        else if (e.getMessage.startsWith("Missing required creator property"))
          throw new JsonMissedPropertyException(getMissedProperty(e))
        else
          throw new JsonIncorrectValueException(getProblemProperty(e))

      case Failure(e: JsonParseException) =>
        val position = e.getProcessor.getTokenLocation.getCharOffset.toInt
        val leftBound = Math.max(0, position - 16)
        val rightBound = Math.min(position + 16, value.length)
        throw new JsonNotParsedException(value.substring(leftBound, rightBound))

      case Failure(_: NullPointerException) =>
        throw new JsonIsNullException

      case Failure(e) => throw e
    }
  }

  private def getProblemProperty(exception: JsonMappingException) = {
    exception.getPath.asScala.foldLeft("") { (s, ref) =>
      val fieldName = Option(ref.getFieldName)
      s + {
        if (fieldName.isDefined) {
          if (s.isEmpty) ""
          else "."
        } + fieldName.get
        else "(" + ref.getIndex + ")"
      }
    }
  }

  private def getMissedProperty(exception: JsonMappingException): String =
    exception.getOriginalMessage.replaceFirst("Missing required creator property\\s*'(.*?)'.*", "$1")

  private def typeReference[T: Manifest]: TypeReference[T] = new TypeReference[T] {
    override def getType: Type = typeFromManifest(manifest[T])
  }

  private def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) {
      m.runtimeClass
    }
    else new ParameterizedType {
      def getRawType: Class[_] = m.runtimeClass

      def getActualTypeArguments: Array[Type] = m.typeArguments.map(typeFromManifest).toArray

      def getOwnerType: Null = null
    }
  }

  def setIgnoreUnknown(ignore: Boolean): Unit = {
    logger.debug(s"Set a value of flag: FAIL_ON_UNKNOWN_PROPERTIES to '${!ignore}'.")
    mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, !ignore)
  }

  def getIgnoreUnknown: Boolean = {
    logger.debug(s"Retrieve a value of flag: FAIL_ON_UNKNOWN_PROPERTIES.")
    !mapper.isEnabled(FAIL_ON_UNKNOWN_PROPERTIES)
  }

  def enableNullForPrimitives(enable: Boolean): Unit = {
    logger.debug(s"Set a value of flag: FAIL_ON_NULL_FOR_PRIMITIVES to '${!enable}'.")
    mapper.configure(FAIL_ON_NULL_FOR_PRIMITIVES, !enable)
  }

  def nullForPrimitivesIsEnabled: Boolean = {
    logger.debug(s"Retrieve a value of flag: FAIL_ON_NULL_FOR_PRIMITIVES.")
    !mapper.isEnabled(FAIL_ON_NULL_FOR_PRIMITIVES)
  }
}
