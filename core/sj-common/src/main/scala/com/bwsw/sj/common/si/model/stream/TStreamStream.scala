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
package com.bwsw.sj.common.si.model.stream

import com.bwsw.sj.common.dal.model.service.TStreamServiceDomain
import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.bwsw.tstreams.storage.StorageClient
import com.bwsw.tstreams.streams.Stream
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class TStreamStream(name: String,
                    service: String,
                    val partitions: Int,
                    tags: Array[String],
                    force: Boolean,
                    streamType: String,
                    description: String)
                   (implicit injector: Injector)
  extends SjStream(streamType, name, service, tags, force, description) {

  import messageResourceUtils.createMessage

  override def to(): TStreamStreamDomain = {
    val serviceRepository = connectionRepository.getServiceRepository

    new TStreamStreamDomain(
      name,
      serviceRepository.get(service).get.asInstanceOf[TStreamServiceDomain],
      partitions,
      description,
      force,
      tags)
  }

  override def create(): Unit = {
    val storageClient = getStorageClient
    if (doesStreamHaveForcedCreation(storageClient)) {
      storageClient.deleteStream(name)
      createTStream(storageClient)
    } else if (!doesTopicExist(storageClient)) {
      createTStream(storageClient)
    }

    storageClient.shutdown()
  }

  override def delete(): Unit = {
    val storageClient = getStorageClient
    if (storageClient.checkStreamExists(name))
      storageClient.deleteStream(name)

    storageClient.shutdown()
  }

  private def getStorageClient: StorageClient = {
    val serviceDAO = connectionRepository.getServiceRepository
    val service = serviceDAO.get(this.service).get.asInstanceOf[TStreamServiceDomain]
    val factory = new TStreamsFactory()
    factory.setProperty(ConfigurationOptions.Common.authenticationKey, service.token)
      .setProperty(ConfigurationOptions.Coordination.endpoints, service.provider.getConcatenatedHosts())
      .setProperty(ConfigurationOptions.Coordination.path, service.prefix)

    val client = factory.getStorageClient()
    factory.close()

    client
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralFields()

    //partitions
    if (partitions <= 0)
      errors += createMessage("entity.error.attribute.required", "Partitions") + ". " +
        createMessage("entity.error.attribute.must.be.positive.integer", "Partitions")

    Option(service) match {
      case Some("") | None =>
        errors += createMessage("entity.error.attribute.required", "Service")
      case Some(x) =>
        val serviceDAO = connectionRepository.getServiceRepository
        val serviceObj = serviceDAO.get(x)
        serviceObj match {
          case None =>
            errors += createMessage("entity.error.doesnot.exist", "Service", x)
          case Some(someService) =>
            if (someService.serviceType != ServiceLiterals.tstreamsType) {
              errors += createMessage("entity.error.must.one.type.other.given",
                s"Service for '${StreamLiterals.tstreamType}' stream",
                ServiceLiterals.tstreamsType,
                someService.serviceType)
            } else {
              if (errors.isEmpty) {
                errors ++= checkStreamPartitionsOnConsistency(someService.asInstanceOf[TStreamServiceDomain])
              }
            }
        }
    }

    errors
  }

  private def checkStreamPartitionsOnConsistency(service: TStreamServiceDomain): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val tstreamFactory = new TStreamsFactory()
    tstreamFactory.setProperty(ConfigurationOptions.Common.authenticationKey, service.token)
      .setProperty(ConfigurationOptions.Coordination.endpoints, service.provider.getConcatenatedHosts())
      .setProperty(ConfigurationOptions.Coordination.path, service.prefix)

    val storageClient = tstreamFactory.getStorageClient()

    if (storageClient.checkStreamExists(name) && !force) {
      val tStream = storageClient.loadStream(name)
      if (tStream.partitionsCount != partitions) {
        errors += createMessage("entity.error.mismatch.partitions", name, s"$partitions", s"${tStream.partitionsCount}")
      }
    }

    storageClient.shutdown()

    errors
  }

  private def doesStreamHaveForcedCreation(storageClient: StorageClient): Boolean =
    doesTopicExist(storageClient) && force

  private def doesTopicExist(storageClient: StorageClient): Boolean =
    storageClient.checkStreamExists(name)

  private def createTStream(storageClient: StorageClient): Stream =
    storageClient.createStream(name, partitions, StreamLiterals.ttl, description)
}
