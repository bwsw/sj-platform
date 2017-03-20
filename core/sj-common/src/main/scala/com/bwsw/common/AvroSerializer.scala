package com.bwsw.common

import java.io.ByteArrayOutputStream

import com.bwsw.common.traits.Serializer
import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.slf4j.LoggerFactory

/**
  * Serializer from [[org.apache.avro.generic.GenericRecord GenericRecord]] and vice versa.
  *
  * @param schema avro schema for deserialization
  * @author Pavel Tomskikh
  */
class AvroSerializer(schema: Schema = null) extends Serializer {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val writerOutput = new ByteArrayOutputStream()
  private val encoder = EncoderFactory.get().binaryEncoder(writerOutput, null)

  def serialize(record: GenericRecord): Array[Byte] = {
    logger.debug("perform serialization")
    val writer = new GenericDatumWriter[GenericRecord](record.getSchema)
    writer.write(record, encoder)
    encoder.flush()
    val serialized = writerOutput.toByteArray
    writerOutput.reset()
    serialized
  }

  def deserialize(bytes: Array[Byte]): GenericRecord = {
    require(schema != null, "avro schema must be defined")
    logger.debug("perform deserialization")

    val decoder = DecoderFactory.get().binaryDecoder(bytes, null)
    val reader = new GenericDatumReader[GenericRecord](schema)
    reader.read(null, decoder)
  }
}
