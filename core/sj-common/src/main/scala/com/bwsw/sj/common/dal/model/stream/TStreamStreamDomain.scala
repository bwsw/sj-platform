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
package com.bwsw.sj.common.dal.model.stream

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.bwsw.tstreams.storage.StorageClient

/**
  * protected methods and variables need for testing purposes
  */
class TStreamStreamDomain(override val name: String,
                          override val service: TStreamServiceDomain,
                          val partitions: Int,
                          override val description: String = RestLiterals.defaultDescription,
                          override val force: Boolean = false,
                          override val tags: Array[String] = Array())
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.tstreamType) {

  private val factory = new TStreamsFactory()
  factory.setProperty(ConfigurationOptions.Coordination.path, this.service.prefix)
    .setProperty(ConfigurationOptions.Coordination.endpoints, this.service.provider.getConcatenatedHosts())
    .setProperty(ConfigurationOptions.Common.authenticationKey, this.service.token)

  protected def createClient(): StorageClient = factory.getStorageClient()

  override def create(): Unit = {
    val storageClient: StorageClient = createClient()

    if (!storageClient.checkStreamExists(this.name)) {
      storageClient.createStream(
        this.name,
        this.partitions,
        StreamLiterals.ttl,
        this.description
      )
    }

    storageClient.shutdown()
  }
}
