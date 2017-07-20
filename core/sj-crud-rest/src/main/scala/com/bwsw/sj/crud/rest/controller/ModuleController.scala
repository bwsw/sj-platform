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
package com.bwsw.sj.crud.rest.controller

import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si._
import com.bwsw.sj.common.si.model.module.ModuleMetadata
import com.bwsw.sj.common.si.result.{Created, Deleted, DeletionError, NotCreated}
import com.bwsw.sj.common.utils.{EngineLiterals, MessageResourceUtils}
import com.bwsw.sj.crud.rest.model.module.{ModuleMetadataApi, SpecificationApiCreator}
import com.bwsw.sj.crud.rest.utils.{FileMetadataUtils, JsonDeserializationErrorMessageCreator}
import com.bwsw.sj.crud.rest.{ModuleJar, ModulesResponseEntity, RelatedToModuleResponseEntity, SpecificationResponseEntity}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.{Failure, Success, Try}

class ModuleController(implicit injector: Injector) {

  private val messageResourceUtils = inject[MessageResourceUtils]
  private val jsonDeserializationErrorMessageCreator = inject[JsonDeserializationErrorMessageCreator]
  private val fileMetadataUtils = inject[FileMetadataUtils]

  import messageResourceUtils.createMessage

  private val serviceInterface = inject[ModuleSI]

  def create(entity: ModuleMetadataApi): RestResponse = {
    val apiErrors = entity.validate
    if (apiErrors.isEmpty) {
      Try(entity.to()) match {
        case Success(moduleMetadata) =>
          serviceInterface.create(moduleMetadata) match {
            case Created =>
              OkRestResponse(
                MessageResponseEntity(
                  createMessage("rest.modules.module.uploaded", moduleMetadata.filename)))
            case NotCreated(errors) =>
              BadRequestRestResponse(
                MessageResponseEntity(
                  createMessage("rest.modules.module.cannot.upload", moduleMetadata.filename, errors.mkString(";"))))
          }

        case Failure(exception: JsonDeserializationException) =>
          val error = jsonDeserializationErrorMessageCreator(exception)
          BadRequestRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.module.cannot.upload", entity.filename.get, error)))

        case Failure(exception: Throwable) =>
          BadRequestRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.module.cannot.upload", entity.filename.get, exception.getMessage)))
      }

    } else {
      BadRequestRestResponse(
        MessageResponseEntity(
          createMessage("rest.modules.module.cannot.upload", entity.filename.get, apiErrors.mkString(";"))))
    }
  }

  def get(moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    processModule(moduleType, moduleName, moduleVersion) { moduleMetadata =>
      val source = fileMetadataUtils.fileToSource(moduleMetadata.file.get)
      ModuleJar(moduleMetadata.filename, source)
    }
  }

  def getAll: RestResponse = {
    OkRestResponse(
      ModulesResponseEntity(
        serviceInterface.getAll.map(fileMetadataUtils.toModuleInfo)))
  }

  def getByType(moduleType: String): RestResponse = {
    serviceInterface.getByType(moduleType) match {
      case Right(modules) =>
        OkRestResponse(
          ModulesResponseEntity(
            modules.map(fileMetadataUtils.toModuleInfo)))
      case Left(error) =>
        BadRequestRestResponse(MessageResponseEntity(error))
    }
  }

  def getAllTypes: RestResponse =
    OkRestResponse(TypesResponseEntity(EngineLiterals.beToFeModulesTypes.map(x => Type(x._1, x._2)).toSeq))

  def getSpecification(moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    processModuleWithoutFile(moduleType, moduleName, moduleVersion) { moduleMetadata =>
      OkRestResponse(
        SpecificationResponseEntity(
          inject[SpecificationApiCreator].from(moduleMetadata.specification)))
    }
  }

  def delete(moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    processModule(moduleType, moduleName, moduleVersion) { metadata =>
      serviceInterface.delete(metadata) match {
        case Deleted =>
          OkRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.module.deleted", metadata.signature)))

        case DeletionError(error) =>
          UnprocessableEntityRestResponse(MessageResponseEntity(error))
      }
    }
  }

  private def processModule(moduleType: String, moduleName: String, moduleVersion: String)
                           (f: ModuleMetadata => RestResponse): RestResponse = {
    serviceInterface.get(moduleType, moduleName, moduleVersion) match {
      case Right(moduleMetadata) => f(moduleMetadata)
      case Left(error) => NotFoundRestResponse(MessageResponseEntity(error))
    }
  }

  private def processModuleWithoutFile(moduleType: String, moduleName: String, moduleVersion: String)
                                      (f: ModuleMetadata => RestResponse): RestResponse = {
    serviceInterface.getMetadataWithoutFile(moduleType, moduleName, moduleVersion) match {
      case Right(moduleMetadata) => f(moduleMetadata)
      case Left(error) => BadRequestRestResponse(MessageResponseEntity(error))
    }
  }

  def getRelated(moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    processModuleWithoutFile(moduleType, moduleName, moduleVersion) { metadata =>
      OkRestResponse(
        RelatedToModuleResponseEntity(
          serviceInterface.getRelatedInstances(metadata)))
    }
  }
}
