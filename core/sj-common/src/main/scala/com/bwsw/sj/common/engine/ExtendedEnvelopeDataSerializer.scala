package com.bwsw.sj.common.engine

import com.bwsw.common.AvroSerializer
import com.bwsw.sj.common.si.model.instance.{BatchInstance, Instance, OutputInstance, RegularInstance}
import org.apache.avro.generic.GenericRecord
import org.slf4j.LoggerFactory

/**
  * Serializer that extends the default functionality for [[org.apache.avro.generic.GenericRecord]]
  *
  * @author Pavel Tomskikh
  */
class ExtendedEnvelopeDataSerializer(classLoader: ClassLoader, instance: Instance)
  extends DefaultEnvelopeDataSerializer(classLoader) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val schema = instance match {
    case batchInstance: BatchInstance => batchInstance.inputAvroSchema
    case regularInstance: RegularInstance => regularInstance.inputAvroSchema
    case outputInstance: OutputInstance => outputInstance.inputAvroSchema
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

  override def deserialize(bytes: Array[Byte]): Object = {
    logger.debug("Deserialize a byte array to an object.")

    if (schema.nonEmpty) avroSerializer.deserialize(bytes)
    else super.deserialize(bytes)
  }
}
