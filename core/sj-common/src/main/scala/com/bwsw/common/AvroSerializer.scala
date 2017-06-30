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

import java.io.ByteArrayOutputStream

import org.apache.avro.Schema
import org.apache.avro.generic.{GenericDatumReader, GenericDatumWriter, GenericRecord}
import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.slf4j.LoggerFactory

/**
  * Serializer from [[org.apache.avro.generic.GenericRecord GenericRecord]] and vice versa.
  *
  * @author Pavel Tomskikh
  */
class AvroSerializer {
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

  def deserialize(bytes: Array[Byte], schema: Schema): GenericRecord = {
    logger.debug("Deserialize a byte array to an avro record.")

    val decoder = DecoderFactory.get().binaryDecoder(bytes, null)
    val reader = new GenericDatumReader[GenericRecord](schema)
    reader.read(null, decoder)
  }
}
