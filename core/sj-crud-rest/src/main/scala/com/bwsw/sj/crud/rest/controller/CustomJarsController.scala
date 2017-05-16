package com.bwsw.sj.crud.rest.controller

import java.nio.file.Paths

import akka.stream.scaladsl.FileIO
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.CustomJarsSI
import com.bwsw.sj.common.utils.MessageResourceUtils.{createMessage, createMessageWithErrors}
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import com.bwsw.sj.crud.rest.{CustomJar, CustomJarsResponseEntity}

import scala.util.{Failure, Success, Try}

class CustomJarsController extends Controller {
  override val serviceInterface = new CustomJarsSI()

  def create(entity: FileMetadataApi): RestResponse = {
    val triedCustomJar = Try {
      val created = serviceInterface.create(entity.to())

      val response = created match {
        case Right(_) =>
          OkRestResponse(MessageResponseEntity(
            createMessage("rest.custom.jars.file.uploaded", entity.filename)))
        case Left(errors) => BadRequestRestResponse(MessageResponseEntity(
          createMessageWithErrors("rest.custom.jars.cannot.upload", errors)
        ))
      }

      response
    }

    entity.file.get.delete()

    triedCustomJar match {
      case Success(response) => response
      case Failure(e) => throw e
    }
  }

  override def getAll(): RestResponse = {
    val response = OkRestResponse(CustomJarsResponseEntity())
    val fileMetadata = serviceInterface.getAll()
    if (fileMetadata.nonEmpty) {
      response.entity = CustomJarsResponseEntity(fileMetadata.map(m => FileMetadataApi.from(m)))
    }

    response
  }

  override def get(name: String): RestResponse = {
    val service = serviceInterface.get(name)

    val response = service match {
      case Some(x) =>
        val source = FileIO.fromPath(Paths.get(x.getAbsolutePath))

        CustomJar(name, source)
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.custom.jars.file.notfound", name)))
    }

    response
  }

//  override def delete(name: String): RestResponse = {
//    val fileMetadatas = fileMetadataDAO.getByParameters(Map("filetype" -> "custom", "filename" -> name))
//    if (fileMetadatas.isEmpty) {
//      throw CustomJarNotFound(createMessage("rest.custom.jars.file.notfound", s"$name"), s"$name")
//    }
//    val fileMetadata = fileMetadatas.head
//
//    var response: RestResponse = InternalServerErrorRestResponse(
//      MessageResponseEntity(s"Can't delete jar '$name' for some reason. It needs to be debugged")
//    )
//
//    if (storage.delete(name)) {
//      configService.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, fileMetadata.specification.name + "-" + fileMetadata.specification.version))
//      response = OkRestResponse(
//        MessageResponseEntity(createMessage("rest.custom.jars.file.deleted.by.filename", name))
//      )
//    }
//
//
//
//
//    val deleteResponse = serviceInterface.delete(name)
//    val response: RestResponse = deleteResponse match {
//      case Right(isDeleted) =>
//        if (isDeleted)
//          OkRestResponse(MessageResponseEntity(createMessage("rest.services.service.deleted", name)))
//        else
//          NotFoundRestResponse(MessageResponseEntity(createMessage("rest.services.service.notfound", name)))
//      case Left(message) =>
//        UnprocessableEntityRestResponse(MessageResponseEntity(message))
//    }
//
//    response
//  }
//
//  def getBy(name: String, version: String): RestResponse = {
//    val service = serviceInterface.get(name, version)
//
//    val response = service match {
//      case Some(x) =>
//        OkRestResponse(ServiceResponseEntity(ServiceApi.from(x)))
//      case None =>
//        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.services.service.notfound", name)))
//    }
//
//    response
//  }
//
//  def deleteBy(name: String, version: String): RestResponse = {
//    val deleteResponse = serviceInterface.delete(name, version)
//    val response: RestResponse = deleteResponse match {
//      case Right(isDeleted) =>
//        if (isDeleted)
//          OkRestResponse(MessageResponseEntity(createMessage("rest.services.service.deleted", name)))
//        else
//          NotFoundRestResponse(MessageResponseEntity(createMessage("rest.services.service.notfound", name)))
//      case Left(message) =>
//        UnprocessableEntityRestResponse(MessageResponseEntity(message))
//    }
//
//    response
//  }
  override def create(serializedEntity: String): RestResponse = ???

  override def delete(name: String): RestResponse = ???
}
