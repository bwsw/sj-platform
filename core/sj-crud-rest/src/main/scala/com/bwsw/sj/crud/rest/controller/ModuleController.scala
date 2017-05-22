package com.bwsw.sj.crud.rest.controller

import java.nio.file.Paths

import akka.stream.scaladsl.FileIO
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ModuleSI
import com.bwsw.sj.common.si.model.module.ModuleMetadata
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import com.bwsw.sj.crud.rest.model.module.{ModuleMetadataApi, SpecificationApi}
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import com.bwsw.sj.crud.rest.{ModuleJar, ModulesResponseEntity, RelatedToModuleResponseEntity, SpecificationResponseEntity}

import scala.util.{Failure, Success, Try}

class ModuleController {

  private val serviceInterface = new ModuleSI

  def create(entity: ModuleMetadataApi): RestResponse = {
    Try(entity.to()) match {
      case Success(moduleMetadata) =>
        serviceInterface.create(moduleMetadata) match {
          case Right(_) =>
            OkRestResponse(
              MessageResponseEntity(
                createMessage("rest.modules.module.uploaded", moduleMetadata.filename)))
          case Left(errors) =>
            BadRequestRestResponse(
              MessageResponseEntity(
                createMessage("rest.modules.module.cannot.upload", moduleMetadata.filename, errors.mkString(";"))))
        }

      case Failure(exception: JsonDeserializationException) =>
        val error = JsonDeserializationErrorMessageCreator(exception)
        BadRequestRestResponse(
          MessageResponseEntity(
            createMessage("rest.modules.module.cannot.upload", entity.filename.get, error)))

      case Failure(exception: Throwable) =>
        BadRequestRestResponse(
          MessageResponseEntity(
            createMessage("rest.modules.module.cannot.upload", entity.filename.get, exception.getMessage)))
    }
  }

  def get(moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    processModule(moduleType, moduleName, moduleVersion) { moduleMetadata =>
      val source = FileIO.fromPath(Paths.get(moduleMetadata.file.get.getAbsolutePath))
      ModuleJar(moduleMetadata.filename, source)
    }
  }

  def getAll: RestResponse = {
    OkRestResponse(
      ModulesResponseEntity(
        serviceInterface.getAll.map(ModuleMetadataApi.toModuleInfo)))
  }

  def getByType(moduleType: String): RestResponse = {
    serviceInterface.getByType(moduleType) match {
      case Right(modules) =>
        OkRestResponse(
          ModulesResponseEntity(
            modules.map(ModuleMetadataApi.toModuleInfo)))
      case Left(error) =>
        BadRequestRestResponse(MessageResponseEntity(error))
    }
  }

  def getAllTypes: RestResponse =
    OkRestResponse(TypesResponseEntity(EngineLiterals.moduleTypes))

  def getSpecification(moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    processModuleWithoutFile(moduleType, moduleName, moduleVersion) { moduleMetadata =>
      OkRestResponse(
        SpecificationResponseEntity(
          SpecificationApi.from(moduleMetadata.specification)))
    }
  }

  def delete(moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    processModule(moduleType, moduleName, moduleVersion) { metadata =>
      serviceInterface.delete(metadata) match {
        case Right(_) =>
          OkRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.module.deleted", metadata.signature)))

        case Left(error) =>
          BadRequestRestResponse(MessageResponseEntity(error))
      }
    }
  }

  private def processModule(moduleType: String, moduleName: String, moduleVersion: String)
                           (f: ModuleMetadata => RestResponse): RestResponse = {
    serviceInterface.get(moduleType, moduleName, moduleVersion) match {
      case Right(moduleMetadata) => f(moduleMetadata)
      case Left(error) => BadRequestRestResponse(MessageResponseEntity(error))
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
