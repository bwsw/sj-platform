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

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}
import com.bwsw.sj.common.si.model.instance.{Instance, CreateInstance}
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, Specification}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.{FileClassLoader, MessageResourceUtils}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable

class InstanceSI(implicit injector: Injector) {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils.createMessage

  private val connectionRepository = inject[ConnectionRepository]
  private val entityRepository: GenericMongoRepository[InstanceDomain] = connectionRepository.getInstanceRepository
  private val createInstance = inject[CreateInstance]
  private val tmpDirectory = "/tmp/"

  def create(instance: Instance, moduleMetadata: ModuleMetadata): CreationResult = {
    val instancePassedValidation = validateInstance(moduleMetadata.specification, moduleMetadata.filename, instance)

    if (instancePassedValidation.result) {
      instance.prepareInstance()
      instance.createStreams()
      entityRepository.save(instance.to())

      Created
    } else {
      NotCreated(instancePassedValidation.errors)
    }
  }

  def getAll: mutable.Buffer[Instance] =
    entityRepository.getAll.map(createInstance.from)

  def getByModule(moduleType: String, moduleName: String, moduleVersion: String): Seq[Instance] = {
    entityRepository.getByParameters(
      Map(
        "module-name" -> moduleName,
        "module-type" -> moduleType,
        "module-version" -> moduleVersion))
      .map(createInstance.from)
  }

  def get(name: String): Option[Instance] =
    entityRepository.get(name).map(createInstance.from)

  def delete(name: String): DeletionResult = {
    entityRepository.get(name) match {
      case Some(instance) =>
        instance.status match {
          case `ready` =>
            entityRepository.delete(name: String)

            Deleted
          case `stopped` | `failed` | `error` =>
            WillBeDeleted(createInstance.from(instance))
          case _ =>
            DeletionError(createMessage("rest.modules.instances.instance.cannot.delete", name))
        }

      case None =>
        EntityNotFound
    }
  }

  def canStart(instance: Instance): Boolean =
    Set(ready, stopped, failed).contains(instance.status)

  def canStop(instance: Instance): Boolean =
    instance.status == started

  private def validateInstance(specification: Specification, filename: String, instance: Instance): ValidationInfo = {
    val validatorClassName = specification.validatorClass
    val clazz = inject[FileClassLoader].loadClass(validatorClassName, filename, tmpDirectory)
    val validator = clazz.newInstance().asInstanceOf[StreamingValidator]
    val optionsValidationInfo = validator.validate(instance)
    val instanceValidationInfo = validator.validate(instance.options)

    ValidationInfo(
      optionsValidationInfo.result && instanceValidationInfo.result,
      optionsValidationInfo.errors ++= instanceValidationInfo.errors)
  }
}
