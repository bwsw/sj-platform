package com.bwsw.common

import java.io.ByteArrayOutputStream

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
class AvroSerializer(schema: Option[Schema] = None) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val writerOutput = new ByteArrayOutputStream()
  private val encoder = EncoderFactory.get().binaryEncoder(writerOutput, null)

  def serialize(record: GenericRecord): Array[Byte] = {
    logger.debug(s"Serialize an avro record to a byte array.")

    val writer = new GenericDatumWriter[GenericRecord](record.getSchema)
    writer.write(record, encoder)
    encoder.flush()
    val serialized = writerOutput.toByteArray
    writerOutput.reset()
    serialized
  }

  def deserialize(bytes: Array[Byte]): GenericRecord = {
    require(schema.nonEmpty, "avro schema must be defined")
    logger.debug("Deserialize a byte array to an avro record.")

    val decoder = DecoderFactory.get().binaryDecoder(bytes, null)
    val reader = new GenericDatumReader[GenericRecord](schema.get)
    reader.read(null, decoder)
  }
}
