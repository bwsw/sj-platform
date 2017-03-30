package com.bwsw.sj.common.engine

import com.bwsw.common.AvroSerializer
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory

/**
  * Serializer that extend the default functionality for [[org.apache.avro.generic.GenericRecord GenericRecord]]
  *
  * @param schema avro schema for deserialization
  * @author Pavel Tomskikh
  */
class ExtendedEnvelopeDataSerializer(classLoader: ClassLoader, schema: Option[Schema] = None)
  extends DefaultEnvelopeDataSerializer(classLoader) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val avroSerializer = new AvroSerializer(schema)

  override def serialize(data: AnyRef): Array[Byte] = {
    logger.debug(s"Serialize an object of class: '${data.getClass}' to a byte array.")

    data match {
      case record: GenericRecord => avroSerializer.serialize(record)
      case _: Serializable => super.serialize(data)
      case wrongClass => throw new NotImplementedError(s"Method serialize isn't defined for ${wrongClass.getClass}. " +
        s"It should have a GenericRecord type or it should be serializable (implement a Serializable trait)")
    }
  }

  override def deserialize(bytes: Array[Byte]) = {
    logger.debug("Deserialize a byte array to an object.")

    if (schema.nonEmpty) avroSerializer.deserialize(bytes)
    else super.deserialize(bytes)
  }
}
