package com.bwsw.sj.common.engine

import com.bwsw.common.AvroSerializer
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory

/**
  * Serializer from [[org.apache.avro.generic.GenericRecord GenericRecord]] and vice versa.
  *
  * @param schema avro schema for deserialization
  * @author Pavel Tomskikh
  */
class AvroEnvelopeDataSerializer(classLoader: ClassLoader, schema: Option[Schema] = None)
  extends DefaultEnvelopeDataSerializer(classLoader) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val avroSerializer = new AvroSerializer(schema)

  override def serialize(data: AnyRef): Array[Byte] = {
    logger.debug(s"Serialize an object of class: '${data.getClass}' to a byte array.")

    data match {
      case record: GenericRecord => avroSerializer.serialize(record)
      case _ => super.serialize(data)
    }
  }

  override def deserialize(bytes: Array[Byte]) = {
    logger.debug("Deserialize a byte array to an object.")

    if (schema.nonEmpty) avroSerializer.deserialize(bytes)
    else super.deserialize(bytes)
  }
}
