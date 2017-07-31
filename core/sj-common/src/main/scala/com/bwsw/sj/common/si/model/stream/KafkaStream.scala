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

import java.util.Date

import com.bwsw.common.KafkaClient
import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.dal.model.stream.KafkaStreamDomain
import com.bwsw.sj.common.utils.{ServiceLiterals, StreamLiterals}
import kafka.common.TopicAlreadyMarkedForDeletionException
import org.apache.kafka.common.errors.TopicExistsException
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class KafkaStream(name: String,
                  service: String,
                  val partitions: Int,
                  val replicationFactor: Int,
                  tags: Array[String],
                  force: Boolean,
                  streamType: String,
                  description: String,
                  creationDate: String)
                 (implicit injector: Injector)
  extends SjStream(streamType, name, service, tags, force, description, creationDate) {

  import messageResourceUtils.createMessage

  private val settingsUtils = inject[SettingsUtils]

  override def to(): KafkaStreamDomain = {
    val serviceRepository = connectionRepository.getServiceRepository

    new KafkaStreamDomain(
      name,
      serviceRepository.get(service).get.asInstanceOf[KafkaServiceDomain],
      partitions,
      replicationFactor = replicationFactor,
      description = description,
      force = force,
      tags = tags,
      creationDate = new Date())
  }

  override def create(): Unit = {
    Try {
      val kafkaClient = createKafkaClient()
      if (doesStreamHaveForcedCreation(kafkaClient)) {
        deleteTopic(kafkaClient)
        createTopic(kafkaClient)
      } else {
        if (!doesTopicExist(kafkaClient)) createTopic(kafkaClient)
      }

      kafkaClient.close()
    } match {
      case Success(_) =>
      case Failure(_: TopicAlreadyMarkedForDeletionException) =>
        throw new Exception(s"Cannot delete a kafka topic '$name'. Topic is already marked for deletion. It means that kafka doesn't support deletion")
      case Failure(_: TopicExistsException) =>
        throw new Exception(s"Cannot create a kafka topic '$name'. Topic is marked for deletion. It means that kafka doesn't support deletion")
      case Failure(e) => throw e
    }
  }

  override def delete(): Unit = {
    Try {
      val kafkaClient = createKafkaClient()
      if (doesTopicExist(kafkaClient)) {
        deleteTopic(kafkaClient)
      }

      kafkaClient.close()
    } match {
      case Success(_) =>
      case Failure(_: TopicAlreadyMarkedForDeletionException) =>
        throw new Exception(s"Cannot delete a kafka topic '$name'. Topic is already marked for deletion. It means that kafka doesn't support deletion")
      case Failure(e) => throw e
    }
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralFields()

    //partitions
    if (partitions <= 0)
      errors += createMessage("entity.error.attribute.required", "Partitions") + ". " +
        createMessage("entity.error.attribute.must.be.positive.integer", "Partitions")

    //replicationFactor
    if (replicationFactor <= 0) {
      errors += createMessage("entity.error.attribute.required", "replicationFactor") + ". " +
        createMessage("entity.error.attribute.must.be.positive.integer", "replicationFactor")
    }

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
            if (someService.serviceType != ServiceLiterals.kafkaType) {
              errors += createMessage("entity.error.must.one.type.other.given",
                s"Service for '${StreamLiterals.kafkaType}' stream",
                ServiceLiterals.kafkaType,
                someService.serviceType)
            } else {
              if (errors.isEmpty)
                errors ++= checkStreamPartitionsOnConsistency(someService.asInstanceOf[KafkaServiceDomain])
            }
        }
    }

    errors
  }

  private def checkStreamPartitionsOnConsistency(service: KafkaServiceDomain): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val kafkaClient = new KafkaClient(service.zkProvider.hosts, settingsUtils.getZkSessionTimeout())
    val topicMetadata = kafkaClient.fetchTopicMetadataFromZk(name)
    if (!topicMetadata.partitionMetadata().isEmpty && topicMetadata.partitionMetadata().size != partitions) {
      errors += createMessage("entity.error.mismatch.partitions", name, s"$partitions", s"${topicMetadata.partitionMetadata().size}")
    }

    errors
  }

  private def doesStreamHaveForcedCreation(kafkaClient: KafkaClient): Boolean =
    doesTopicExist(kafkaClient) && force

  private def doesTopicExist(kafkaClient: KafkaClient): Boolean =
    kafkaClient.topicExists(name)

  private def deleteTopic(kafkaClient: KafkaClient): Unit =
    kafkaClient.deleteTopic(name)

  private def createTopic(kafkaClient: KafkaClient): Unit =
    kafkaClient.createTopic(name, partitions, replicationFactor)
}
