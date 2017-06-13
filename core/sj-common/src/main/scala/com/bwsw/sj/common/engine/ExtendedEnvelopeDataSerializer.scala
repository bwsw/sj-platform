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
package com.bwsw.sj.common.engine

import com.bwsw.common.AvroSerializer
import com.bwsw.sj.common.si.model.instance.{BatchInstance, Instance, OutputInstance, RegularInstance}
import com.bwsw.sj.common.utils.AvroRecordUtils
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
  private val schema = AvroRecordUtils.jsonToSchema {
    instance match {
      case batchInstance: BatchInstance => batchInstance.inputAvroSchema
      case regularInstance: RegularInstance => regularInstance.inputAvroSchema
      case outputInstance: OutputInstance => outputInstance.inputAvroSchema
      case _ => "{}"
    }
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
