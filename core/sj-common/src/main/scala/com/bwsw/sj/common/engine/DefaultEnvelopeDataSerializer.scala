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

import com.bwsw.common.ObjectSerializer
import org.slf4j.LoggerFactory

/**
  * Provides default implementation of methods to serialize/deserialize envelope data
  */

class DefaultEnvelopeDataSerializer(classLoader: ClassLoader) extends EnvelopeDataSerializer[AnyRef] {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val serializer = new ObjectSerializer(classLoader)

  override def deserialize(bytes: Array[Byte]): Object = {
    logger.debug("Deserialize a byte array to an object.")

    serializer.deserialize(bytes)
  }

  override def serialize(data: AnyRef): Array[Byte] = {
    logger.debug(s"Serialize an object of class: '${data.getClass}' to a byte array.")

    serializer.serialize(data)
  }
}
