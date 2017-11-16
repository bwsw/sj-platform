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
package com.bwsw.sj.test.module.output.es

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.engine.core.entities.TStreamEnvelope
import com.bwsw.sj.common.engine.core.environment.OutputEnvironmentManager
import com.bwsw.sj.common.engine.core.output.{Entity, OutputStreamingExecutor}
import com.bwsw.sj.engine.core.output.types.es.{ElasticsearchEntityBuilder, IntegerField}
import com.typesafe.scalalogging.Logger


/**
  * @author Pavel Tomskikh
  */
class Executor(manager: OutputEnvironmentManager) extends OutputStreamingExecutor[Integer](manager) {
  private val logger = Logger(getClass)
  private val serializer = new JsonSerializer(ignoreUnknown = true, enableNullForPrimitives = true)
  private val property =
    serializer.deserialize[Map[String, Any]](manager.options)
      .getOrElse(Data.propertyField, "value").asInstanceOf[String]

  override def onMessage(envelope: TStreamEnvelope[Integer]): List[Data] = {
    logger.debug(s"Got envelope ${envelope.id} with elements ${envelope.data.toList.mkString(", ")}")

    envelope.data.toList.map(i => new Data(property, i))
  }

  override def getOutputEntity: Entity[String] = {
    new ElasticsearchEntityBuilder()
      .field(new IntegerField(property))
      .build()
  }
}
