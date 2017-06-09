package com.bwsw.sj.crud.rest.controller

import java.io.File

import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.CustomFilesSI
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import com.bwsw.sj.crud.rest.utils.FileMetadataUtils
import com.bwsw.sj.crud.rest.{CustomFile, CustomFilesResponseEntity}
import scaldi.Injectable.inject
import scaldi.Injector

class CustomFilesController(implicit protected val injector: Injector) extends Controller {
  private val messageResourceUtils = inject[MessageResourceUtils]
  private val fileMetadataUtils = inject[FileMetadataUtils]

  import messageResourceUtils._

  override val serviceInterface = inject[CustomFilesSI]

  protected val entityDeletedMessage: String = "rest.custom.files.file.deleted"
  protected val entityNotFoundMessage: String = "rest.custom.files.file.notfound"

  def create(entity: FileMetadataApi): RestResponse = {
    var response: RestResponse = BadRequestRestResponse(MessageResponseEntity(
      getMessage("rest.custom.files.file.missing")))

    if (entity.filename.isDefined) {
      val file = entity.customFileParts("file").asInstanceOf[File]
      entity.file = Some(file)

      entity.customFileParts.get("description").foreach { description =>
        entity.description = description.asInstanceOf[String]
      }

      val created = serviceInterface.create(entity.to())

      response = created match {
        case Created =>
          OkRestResponse(MessageResponseEntity(
            createMessage("rest.custom.files.file.uploaded", entity.filename.get)))
        case NotCreated(_) =>
          ConflictRestResponse(MessageResponseEntity(
            createMessage("rest.custom.files.file.exists", entity.filename.get)))
      }
      file.delete()
    }
    entity.file.foreach(_.delete())

    response
  }

  override def getAll(): RestResponse = {
    val fileMetadata = serviceInterface.getAll().map(fileMetadataUtils.toCustomFileInfo)

    OkRestResponse(CustomFilesResponseEntity(fileMetadata))
  }

  override def get(name: String): RestResponse = {
    val fileMetadata = serviceInterface.get(name)

    fileMetadata match {
      case Some(x) =>
        val source = fileMetadataUtils.fileToSource(x.file.get)

        CustomFile(name, source)
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage(entityNotFoundMessage, name)))
    }
  }

  override def create(serializedEntity: String): RestResponse = ???
}