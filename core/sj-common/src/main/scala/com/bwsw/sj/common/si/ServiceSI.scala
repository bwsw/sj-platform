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
package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.service.ServiceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.service.{Service, ServiceCreator}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable

/**
  * Provides methods to access [[com.bwsw.sj.common.si.model.service.Service]]s
  * in [[com.bwsw.sj.common.dal.repository.GenericMongoRepository]]
  */
class ServiceSI(implicit injector: Injector) extends ServiceInterface[Service, ServiceDomain] {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils.createMessage

  private val connectionRepository = inject[ConnectionRepository]

  override protected val entityRepository: GenericMongoRepository[ServiceDomain] = connectionRepository.getServiceRepository

  private val streamRepository = connectionRepository.getStreamRepository
  private val instanceRepository = connectionRepository.getInstanceRepository
  private val createService = inject[ServiceCreator]

  override def create(entity: Service): CreationResult = {
    val errors = entity.validate()

    if (errors.isEmpty) {
      entityRepository.save(entity.to())

      Created
    } else {
      NotCreated(errors)
    }
  }

  def getAll(): mutable.Buffer[Service] = {
    entityRepository.getAll.map(x => createService.from(x))
  }

  def get(name: String): Option[Service] = {
    entityRepository.get(name).map(createService.from)
  }

  override def delete(name: String): DeletionResult = {
    if (getRelatedStreams(name).nonEmpty)
      DeletionError(createMessage("rest.services.service.cannot.delete.due.to.streams", name))
    else {
      if (getRelatedInstances(name).nonEmpty)
        DeletionError(createMessage("rest.services.service.cannot.delete.due.to.instances", name))
      else {
        entityRepository.get(name) match {
          case Some(_) =>
            entityRepository.delete(name)

            Deleted
          case None =>
            EntityNotFound
        }
      }
    }
  }

  /**
    * Returns [[com.bwsw.sj.common.si.model.stream.SjStream]]s and [[com.bwsw.sj.common.dal.model.instance.InstanceDomain]]s
    * related with [[com.bwsw.sj.common.si.model.service.Service]]
    *
    * @param name name of service
    * @return Right((streams, instances)) if service exists, Left(false) otherwise
    */
  def getRelated(name: String): Either[Boolean, (mutable.Buffer[String], mutable.Buffer[String])] = {
    val service = entityRepository.get(name)

    service match {
      case Some(_) => Right((getRelatedStreams(name), getRelatedInstances(name)))
      case None => Left(false)
    }
  }

  private def getRelatedStreams(serviceName: String): mutable.Buffer[String] = {
    streamRepository.getAll.filter(
      s => s.service.name.equals(serviceName)
    ).map(_.name)
  }

  private def getRelatedInstances(serviceName: String): mutable.Buffer[String] = {
    instanceRepository.getAll.filter(
      s => s.coordinationService.name.equals(serviceName)
    ).map(_.name)
  }
}
