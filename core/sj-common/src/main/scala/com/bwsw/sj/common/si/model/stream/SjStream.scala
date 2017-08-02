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

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.model.stream._
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest.utils.ValidationUtils.validateName
import com.bwsw.sj.common.utils.{MessageResourceUtils, StreamLiterals}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class SjStream(val streamType: String,
               val name: String,
               val service: String,
               val tags: Array[String],
               val force: Boolean,
               val description: String,
               val creationDate: String)
              (implicit injector: Injector) {

  protected val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils.createMessage

  protected val connectionRepository = inject[ConnectionRepository]

  protected var serviceDomain: ServiceDomain = _

  protected val serviceType: String = StreamLiterals.typeToServiceType(streamType)

  def to(): StreamDomain = ???

  /**
    * Validates stream
    *
    * @return empty array if stream is correct, validation errors otherwise
    */
  def validate(): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()

    errors ++= validateGeneralFields()

    val (serviceDomain, extractedErrors) = extractServiceByName(service, serviceType)
    if (serviceDomain.isEmpty)
      errors ++= extractedErrors
    else {
      this.serviceDomain = serviceDomain.get
      errors ++= validateSpecificFields()
    }

    errors
  }

  def validateSpecificFields(): ArrayBuffer[String] = ArrayBuffer[String]()

  /**
    * Creates structure in storage, used by stream
    */
  def create(): Unit = {}

  /**
    * Deletes structure in storage, used by stream
    */
  def delete(): Unit = {}

  /**
    * Validates fields which common for all types of stream
    *
    * @return empty array if fields is correct, validation errors otherwise
    */
  protected def validateGeneralFields(): ArrayBuffer[String] = {
    val streamDAO = connectionRepository.getStreamRepository
    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(name) match {
      case Some("") | None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        errors ++= validateStreamName(name)

        val streamObj = streamDAO.get(x)
        if (streamObj.isDefined) {
          errors += createMessage("entity.error.already.exists", "Stream", x)
        }
    }

    // 'streamType' field
    Option(streamType) match {
      case Some("") | None =>
        errors += createMessage("entity.error.attribute.required", "Type")
      case Some(t) =>
        if (!StreamLiterals.types.contains(t)) {
          errors += createMessage("entity.error.unknown.type.must.one.of", t, "stream", StreamLiterals.types.mkString("[", ", ", "]"))
        }
    }

    errors
  }

  protected def extractServiceByName(serviceName: String, serviceType: String): (Option[ServiceDomain], ArrayBuffer[String]) = {
    val errors = new ArrayBuffer[String]()
    var serviceDomain: Option[ServiceDomain] = None
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
            if (someService.serviceType != serviceType) {
              errors += createMessage("entity.error.must.one.type.other.given",
                s"Service for '$serviceType' stream",
                serviceType,
                someService.serviceType)
            } else {
              serviceDomain = Some(someService)
            }
        }
    }

    (serviceDomain, errors)
  }

  protected def validateStreamName(name: String): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    if (!validateName(name)) {
      errors += createMessage("entity.error.incorrect.name", "Stream", name, "stream")
    }

    errors
  }
}

class StreamCreator {

  def from(streamDomain: StreamDomain)(implicit injector: Injector): SjStream = streamDomain.streamType match {
    case StreamLiterals.`tstreamsType` =>
      val tStreamStream = streamDomain.asInstanceOf[TStreamStreamDomain]

      new TStreamStream(
        tStreamStream.name,
        tStreamStream.service.name,
        tStreamStream.partitions,
        tStreamStream.tags,
        tStreamStream.force,
        tStreamStream.streamType,
        tStreamStream.description,
        tStreamStream.creationDate.toString
      )

    case StreamLiterals.`restType` =>
      val restStream = streamDomain.asInstanceOf[RestStreamDomain]

      new RestStream(
        restStream.name,
        restStream.service.name,
        restStream.tags,
        restStream.force,
        restStream.streamType,
        restStream.description,
        restStream.creationDate.toString
      )

    case StreamLiterals.`kafkaType` =>
      val kafkaStream = streamDomain.asInstanceOf[KafkaStreamDomain]

      new KafkaStream(
        kafkaStream.name,
        kafkaStream.service.name,
        kafkaStream.partitions,
        kafkaStream.replicationFactor,
        kafkaStream.tags,
        kafkaStream.force,
        kafkaStream.streamType,
        kafkaStream.description,
        kafkaStream.creationDate.toString
      )

    case StreamLiterals.`jdbcType` =>
      val jdbcStream = streamDomain.asInstanceOf[JDBCStreamDomain]

      new JDBCStream(
        jdbcStream.name,
        jdbcStream.service.name,
        jdbcStream.primary,
        jdbcStream.tags,
        jdbcStream.force,
        jdbcStream.streamType,
        jdbcStream.description,
        jdbcStream.creationDate.toString
      )

    case StreamLiterals.`elasticsearchType` =>
      val esStream = streamDomain.asInstanceOf[ESStreamDomain]

      new ESStream(
        esStream.name,
        esStream.service.name,
        esStream.tags,
        esStream.force,
        esStream.streamType,
        esStream.description,
        esStream.creationDate.toString
      )
  }
}
