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
package com.bwsw.sj.crud.rest.instance.validator

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.model.service.{ServiceDomain, ZKServiceDomain}
import com.bwsw.sj.common.dal.model.stream.{KafkaStreamDomain, StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.si.model.module.Specification
import com.bwsw.sj.common.utils.{MessageResourceUtils, ServiceLiterals, StreamLiterals}
import com.bwsw.sj.crud.rest.utils.CompletionUtils
import org.slf4j.{Logger, LoggerFactory}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

/**
  * Trait provides common methods for [[com.bwsw.sj.common.si.model.instance.Instance]] validation
  *
  * @author Kseniya Tomskikh
  */
abstract class InstanceValidator(implicit val injector: Injector) extends CompletionUtils {

  protected val messageResourceUtils: MessageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils.createMessage

  type T <: Instance
  protected val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  var serviceRepository: GenericMongoRepository[ServiceDomain] = connectionRepository.getServiceRepository
  var instanceRepository: GenericMongoRepository[InstanceDomain] = connectionRepository.getInstanceRepository
  val serializer: JsonSerializer = new JsonSerializer

  /**
    * Validating input parameters for streaming module
    *
    * @param instance - input parameters for running module
    * @return - List of errors
    */
  def validate(instance: T, specification: Specification): Seq[String] = {
    validateGeneralOptions(instance) ++ validateStreamOptions(instance, specification)
  }

  /**
    * Validation of base instance parameters
    *
    */
  private def validateGeneralOptions(instance: Instance): ArrayBuffer[String] = {
    logger.debug(s"Instance: ${instance.name}. General options validation.")
    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(instance.name) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "Name")
        }
        else {
          if (instanceRepository.get(x).isDefined) {
            errors += createMessage("rest.modules.instances.instance.exists", x)
          }

          if (!validateName(x)) {
            errors += createMessage("entity.error.incorrect.name", "Instance", x, "instance")
          }
        }
    }

    // 'per-task-cores' field
    if (instance.perTaskCores <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "perTaskCores")
    }

    // 'per-task-ram' field
    if (instance.perTaskRam <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "perTaskRam")
    }

    // 'performance-reporting-interval' field
    if (instance.performanceReportingInterval <= 0) {
      errors += createMessage("rest.validator.attribute.must.greater.than.zero", "performanceReportingInterval")
    }

    // 'coordination-service' field
    Option(instance.coordinationService) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "coordinationService")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("rest.validator.attribute.required", "coordinationService")
        }
        else {
          val coordService = serviceRepository.get(x)
          if (coordService.isDefined) {
            if (!coordService.get.isInstanceOf[ZKServiceDomain]) {
              errors += createMessage("rest.validator.attribute.not", "coordinationService", ServiceLiterals.zookeeperType)
            }
          } else {
            errors += createMessage("rest.validator.not.exist", s"'coordinationService' $x")
          }
        }
    }

    errors
  }

  /**
    * Validation of instance streams parameters and related parameters
    *
    */
  protected def validateStreamOptions(instance: T, specification: Specification): Seq[String] = Seq()

  protected def doesContainDuplicates(list: Array[String]): Boolean = {
    list.map(x => (x, 1)).groupBy(_._1).map(x => x._2.reduce { (a, b) => (a._1, a._2 + b._2) }).exists(x => x._2 > 1)
  }

  protected def getStreams(streamNames: Array[String]): Array[StreamDomain] = {
    val streamRepository = connectionRepository.getStreamRepository
    streamNames.flatMap(x => streamRepository.get(x))
  }

  /**
    * Getting service names for all streams
    *
    * @param streams All streams
    * @return List of service-names
    */
  def getStreamServices(streams: Seq[StreamDomain]): List[String] = {
    streams.map(s => (s.service.name, 1)).groupBy(_._1).keys.toList
  }

  protected def getStreamsPartitions(streams: Seq[StreamDomain]): Seq[Int] = {
    streams.map { stream =>
      stream.streamType match {
        case StreamLiterals.`tstreamsType` =>
          stream.asInstanceOf[TStreamStreamDomain].partitions
        case StreamLiterals.`kafkaType` =>
          stream.asInstanceOf[KafkaStreamDomain].partitions
        case _ => 0
      }
    }
  }

  protected def validateParallelism(parallelism: Any, minimumNumberOfPartitions: Int): ArrayBuffer[String] = {
    logger.debug(s"Validate a parallelism parameter.")
    val errors = new ArrayBuffer[String]()
    Option(parallelism) match {
      case None =>
        errors += createMessage("rest.validator.attribute.required", "Parallelism")
      case Some(x) =>
        x match {
          case dig: Int =>
            if (dig <= 0) {
              errors += createMessage("rest.validator.attribute.must.greater.than.zero", "Parallelism")
            }
            if (dig > minimumNumberOfPartitions) {
              errors += createMessage("rest.validator.attribute.must.greater.than.parallelism", s"$dig", s"$minimumNumberOfPartitions")
            }
          case s: String =>
            if (!s.equals("max")) {
              errors += createMessage("rest.validator.parameter.unknown.type", "parallelism", "digit or 'max'")
            }
          case _ =>
            errors += createMessage("rest.validator.parameter.unknown.type", "parallelism", "digit or 'max'")
        }
    }

    errors
  }
}
