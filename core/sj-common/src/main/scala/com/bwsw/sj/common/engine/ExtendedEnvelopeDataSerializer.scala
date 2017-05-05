package com.bwsw.sj.common.engine

import com.bwsw.common.AvroSerializer
import com.bwsw.sj.common.dal.model.module.{AvroSchemaForInstance, Instance}
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory

/**
  * Serializer that extend the default functionality for [[org.apache.avro.generic.GenericRecord GenericRecord]]
  *
  * @author Pavel Tomskikh
  */
class ExtendedEnvelopeDataSerializer(classLoader: ClassLoader, instance: Instance)
  extends DefaultEnvelopeDataSerializer(classLoader) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val schema = instance match {
    case i: AvroSchemaForInstance => i.getInputAvroSchema
    case _ => None
  }

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
