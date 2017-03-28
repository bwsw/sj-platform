package com.bwsw.common

import java.lang.reflect.{ParameterizedType, Type}

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory

object JsonSerializer {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

  def serialize(value: Any): String = {
    import java.io.StringWriter

    logger.debug(s"Serialize a value of class: '${value.getClass}' to string.")
    val writer = new StringWriter()
    mapper.writeValue(writer, value)
    writer.toString
  }

  def deserialize[T: Manifest](value: String): T = {
    logger.debug(s"Deserialize a value: '$value' to object.")
    mapper.readValue(value, typeReference[T])
  }

  private def typeReference[T: Manifest] = new TypeReference[T] {
    override def getType = typeFromManifest(manifest[T])
  }

  private def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) {
      m.runtimeClass
    }
    else new ParameterizedType {
      def getRawType = m.runtimeClass

      def getActualTypeArguments = m.typeArguments.map(typeFromManifest).toArray

      def getOwnerType = null
    }
  }
}
