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

import java.io.{ByteArrayOutputStream, EOFException}

import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData.Record
import org.apache.avro.generic.{GenericDatumWriter, GenericRecord}
import org.apache.avro.io.EncoderFactory
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests for [[AvroSerializer]]
  *
  * @author Pavel Tomskikh
  */
class AvroSerializerTests extends FlatSpec with Matchers with TableDrivenPropertyChecks {
  val innerField1 = "innerField1"
  val innerField2 = "innerField2"
  val innerSchema = SchemaBuilder.record("inner").fields()
    .name(innerField1).`type`().booleanType().booleanDefault(false)
    .name(innerField2).`type`().doubleType().noDefault()
    .endRecord()

  val field1 = "field1"
  val field2 = "field2"
  val schema = SchemaBuilder.record("record").fields()
    .name(field1).`type`().intType().noDefault()
    .name(field2).`type`(innerSchema).noDefault()
    .endRecord()

  val records = Table(
    "record",
    createRecord(1, innerValue1 = true, 1),
    createRecord(5, innerValue1 = true, 1),
    createRecord(-7, innerValue1 = true, 1),
    createRecord(1, innerValue1 = false, 1),
    createRecord(1, innerValue1 = true, 10.101),
    createRecord(1, innerValue1 = true, 55.55),
    createRecord(1, innerValue1 = true, -68.112))

  val serializer = new AvroSerializer

  "AvroSerializer" should "serialize correct records properly" in {
    forAll(records) { record =>
      serializer.serialize(record) shouldBe getBytes(record)
    }
  }

  it should "not serialize record with missed fields" in {
    val record = new Record(schema)

    a[NullPointerException] shouldBe thrownBy(serializer.serialize(record))
  }

  it should "deserialize correct records properly" in {
    forAll(records) { record =>
      serializer.deserialize(getBytes(record), schema) shouldBe record
    }
  }

  it should "not deserialize incorrect bytes" in {
    val bytes = Array[Byte](33, 33, 24)

    a[EOFException] shouldBe thrownBy(serializer.deserialize(bytes, schema))
  }


  def createRecord(value1: Int, innerValue1: Boolean, innerValue2: Double): Record = {
    val innerRecord = new Record(innerSchema)
    innerRecord.put(innerField1, innerValue1)
    innerRecord.put(innerField2, innerValue2)
    val record = new Record(schema)
    record.put(field1, value1)
    record.put(field2, innerRecord)
    record
  }

  def getBytes(record: GenericRecord): Array[Byte] = {
    val writerOutput = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(writerOutput, null)
    val writer = new GenericDatumWriter[GenericRecord](record.getSchema)
    writer.write(record, encoder)
    encoder.flush()
    writerOutput.toByteArray
  }
}
