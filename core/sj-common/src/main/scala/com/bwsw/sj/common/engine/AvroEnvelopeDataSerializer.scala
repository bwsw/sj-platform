package com.bwsw.sj.common.engine

import com.bwsw.common.AvroSerializer
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

/**
  * Serializer from [[org.apache.avro.generic.GenericRecord GenericRecord]] and vice versa.
  *
  * @param schema avro schema for deserialization
  * @author Pavel Tomskikh
  */
class AvroEnvelopeDataSerializer(classLoader: ClassLoader, schema: Option[Schema] = None)
  extends DefaultEnvelopeDataSerializer(classLoader) {

  private val avroSerializer = new AvroSerializer(schema)

  override def serialize(data: AnyRef): Array[Byte] = data match {
    case record: GenericRecord => avroSerializer.serialize(record)
    case _ => super.serialize(data)
  }

  override def deserialize(bytes: Array[Byte]) = {
    if (schema.nonEmpty) avroSerializer.deserialize(bytes)
    else super.deserialize(bytes)
  }
}
