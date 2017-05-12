package com.bwsw.sj.common.rest

import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.rest.model.config.ConfigurationSettingApi
import com.bwsw.sj.common.rest.model.module.{InstanceApi, SpecificationApi}
import com.fasterxml.jackson.annotation.JsonProperty

import scala.collection.mutable

class RestResponse(@JsonProperty("status-code") var statusCode: Int = 0, entity: ResponseEntity = new ResponseEntity {})

case class OkRestResponse(var entity: ResponseEntity) extends RestResponse(200, entity)

case class CreatedRestResponse(var entity: ResponseEntity) extends RestResponse(201, entity)

case class BadRequestRestResponse(var entity: ResponseEntity) extends RestResponse(400, entity)

case class NotFoundRestResponse(var entity: ResponseEntity) extends RestResponse(404, entity)

case class ConflictRestResponse(var entity: ResponseEntity) extends RestResponse(409, entity)

case class UnprocessableEntityRestResponse(var entity: ResponseEntity) extends RestResponse(422, entity)

case class InternalServerErrorRestResponse(var entity: ResponseEntity) extends RestResponse(500, entity)


trait ResponseEntity

case class FrameworkTask(id: String,
                         state: String,
                         stateChange: String,
                         reason: String,
                         node: String,
                         lastNode: String,
                         directories: Seq[String])

case class FrameworkRestEntity(tasks: Seq[FrameworkTask]) extends ResponseEntity

case class MessageResponseEntity(message: String) extends ResponseEntity

case class KeyedMessageResponseEntity(message: String, key: String) extends ResponseEntity


case class TypesResponseEntity(types: Seq[String]) extends ResponseEntity

case class RelatedToStreamResponseEntity(instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity

case class DomainsResponseEntity(domains: Seq[String] = ConfigLiterals.domains) extends ResponseEntity


case class ModuleInfo(moduleType: String, moduleName: String, moduleVersion: String, size: Long)

case class ModulesResponseEntity(modules: mutable.Buffer[ModuleInfo] = mutable.Buffer()) extends ResponseEntity

case class RelatedToModuleResponseEntity(instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity

case class SpecificationResponseEntity(specification: SpecificationApi) extends ResponseEntity

case class ShortInstancesResponseEntity(instances: mutable.Buffer[ShortInstance] = mutable.Buffer()) extends ResponseEntity

case class InstanceResponseEntity(instance: InstanceApi) extends ResponseEntity

case class InstancesResponseEntity(instances: mutable.Buffer[InstanceApi] = mutable.Buffer()) extends ResponseEntity

case class ShortInstance(name: String, moduleType: String, moduleName: String, moduleVersion: String,
                         description: String, status: String, restAddress: String)


case class CustomJarInfo(name: String, version: String, size: Long)

case class CustomJarsResponseEntity(customJars: mutable.Buffer[CustomJarInfo] = mutable.Buffer()) extends ResponseEntity

case class CustomFileInfo(name: String, description: String, uploadDate: String, size: Long) extends ResponseEntity

case class CustomFilesResponseEntity(customFiles: mutable.Buffer[CustomFileInfo] = mutable.Buffer()) extends ResponseEntity