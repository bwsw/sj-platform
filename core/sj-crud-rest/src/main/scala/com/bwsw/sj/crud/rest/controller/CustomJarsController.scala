package com.bwsw.sj.crud.rest.controller

import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si._
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import com.bwsw.sj.crud.rest.utils.FileMetadataUtils
import com.bwsw.sj.crud.rest.{CustomJar, CustomJarsResponseEntity}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.util.Try

class CustomJarsController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]
  private val fileMetadataUtils = inject[FileMetadataUtils]

  import messageResourceUtils._

  override val serviceInterface = inject[CustomJarsSI]

  protected val entityDeletedMessage: String = "rest.custom.jars.file.deleted.by.filename"
  protected val entityNotFoundMessage: String = "rest.custom.jars.file.notfound"

  def create(entity: FileMetadataApi): RestResponse = {
    val triedCustomJar = Try {
      val created = serviceInterface.create(entity.to())

      val response = created match {
        case Created =>
          OkRestResponse(MessageResponseEntity(
            createMessage("rest.custom.jars.file.uploaded", entity.filename.get)))
        case NotCreated(errors) =>
          BadRequestRestResponse(MessageResponseEntity(
            createMessageWithErrors("rest.custom.jars.cannot.upload", errors)))
      }

      response
    }

    entity.file.get.delete()

    triedCustomJar.get
  }

  override def getAll(): RestResponse = {
    val fileMetadata = serviceInterface.getAll().map(fileMetadataUtils.toCustomJarInfo)

    OkRestResponse(CustomJarsResponseEntity(fileMetadata))
  }

  override def get(name: String): RestResponse = {
    val fileMetadata = serviceInterface.get(name)

    fileMetadata match {
      case Some(x) =>
        val source = fileMetadataUtils.fileToSource(x.file.get)

        CustomJar(name, source)
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }
  }

  def getBy(name: String, version: String): RestResponse = {
    serviceInterface.getBy(name, version) match {
      case Some(x) =>
        val source = fileMetadataUtils.fileToSource(x.file.get)

        CustomJar(x.filename, source)

      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, s"$name-$version")))
    }
  }

  def deleteBy(name: String, version: String): RestResponse = {
    serviceInterface.deleteBy(name, version) match {
      case Deleted =>
        OkRestResponse(MessageResponseEntity(createMessage("rest.custom.jars.file.deleted", name, version)))
      case EntityNotFound =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, s"$name-$version")))
      case DeletionError(message) =>
        UnprocessableEntityRestResponse(MessageResponseEntity(message))
    }
  }

  override def create(serializedEntity: String): RestResponse = ???
}
