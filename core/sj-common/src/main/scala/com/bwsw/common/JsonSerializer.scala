package com.bwsw.common

import java.lang.reflect.{ParameterizedType, Type}

import com.bwsw.common.exceptions._
import com.bwsw.common.traits.Serializer
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonMappingException, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

class JsonSerializer extends Serializer {

  def this(ignore: Boolean) = {
    this()
    this.setIgnoreUnknown(ignore)
  }

  private val logger = LoggerFactory.getLogger(this.getClass)

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

  def serialize(value: Any): String = {
    import java.io.StringWriter

    logger.debug(s"Serialize a value of class: '${value.getClass}' to string.")
    val writer = new StringWriter()
    mapper.writeValue(writer, value)
    writer.toString
  }

  def deserialize[T: Manifest](value: String): T = {
    logger.debug(s"Deserialize a value: '$value' to object.")
    try {
      mapper.readValue(value, typeReference[T])
    } catch {
      case e: UnrecognizedPropertyException =>
        throw new JsonUnrecognizedPropertyException(getProblemProperty(e))
      case e: JsonMappingException =>
        if (e.getMessage.startsWith("No content"))
          throw new JsonDeserializationException("Empty JSON")
        else
          throw new JsonIncorrectValueException(getProblemProperty(e))
      case e: JsonParseException =>
        val position = e.getProcessor.getTokenLocation.getCharOffset.toInt
        val leftBound = Math.max(0, position - 16)
        val rightBound = Math.min(position + 16, value.length)
        throw new JsonNotParsedException(value.substring(leftBound, rightBound))
      case _: NullPointerException =>
        throw new JsonDeserializationException("JSON is null")
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

  override def setIgnoreUnknown(ignore: Boolean): Unit = {
    logger.debug(s"Set a value of flag: FAIL_ON_UNKNOWN_PROPERTIES to '$ignore'.")
    if (ignore) {
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    } else {
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
    }
  }

  override def getIgnoreUnknown(): Boolean = {
    logger.debug(s"Retrieve a value of flag: FAIL_ON_UNKNOWN_PROPERTIES.")
    !((mapper.getDeserializationConfig.getDeserializationFeatures & DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.getMask) == DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.getMask)
  }
}
