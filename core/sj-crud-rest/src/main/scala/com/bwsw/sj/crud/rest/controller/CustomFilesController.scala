package com.bwsw.sj.crud.rest.controller

import java.io.File

import com.bwsw.sj.common.si.CustomFilesSI
import com.bwsw.sj.common.utils.MessageResourceUtils.{createMessage, getMessage}
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import com.bwsw.sj.crud.rest.{CustomFile, CustomFilesResponseEntity}
import java.nio.file.Paths

import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.bwsw.sj.common.rest._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class CustomFilesController(implicit val materializer: ActorMaterializer, implicit val executor: ExecutionContextExecutor) extends Controller {
  override val serviceInterface = new CustomFilesSI()

  def create(entity: FileMetadataApi): RestResponse = {
    var filename: Option[String] = None
    val file = File.createTempFile("restCustomFile", "")
    val parts: Future[Map[String, Any]] = entity.formData.get.parts.mapAsync[(String, Any)](1) {

      case b: BodyPart if b.name == "file" =>
        filename = b.filename
        b.entity.dataBytes.runWith(FileIO.toPath(file.toPath)).map(_ => b.name -> file)

      case b: BodyPart =>
        b.toStrict(2.seconds).map(strict => b.name -> strict.entity.data.utf8String)

    }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)

    var response: RestResponse = BadRequestRestResponse(MessageResponseEntity(
      getMessage("rest.custom.files.file.missing")))

    parts.onComplete {
      case Success(allParts) =>
        if (filename.isDefined) {
          val file = allParts("file").asInstanceOf[File]
          entity.file = Some(file)
          entity.filename = filename.get
          if (allParts.isDefinedAt("description")) entity.description = allParts("description").asInstanceOf[String]

          val created = serviceInterface.create(entity.to())

          response = created match {
            case Right(_) =>
              OkRestResponse(MessageResponseEntity(
                createMessage("rest.custom.files.file.uploaded", entity.filename)))
            case Left(_) => ConflictRestResponse(MessageResponseEntity(
              createMessage("rest.custom.files.file.exists", entity.filename)))
          }

          file.delete()
        }

      case Failure(throwable) =>
        file.delete()
        throw throwable
    }

    response
  }

  override def getAll(): RestResponse = {
    val response = OkRestResponse(CustomFilesResponseEntity())
    val fileMetadata = serviceInterface.getAll()
    if (fileMetadata.nonEmpty) {
      response.entity = CustomFilesResponseEntity(fileMetadata.map(m => FileMetadataApi.toCustomFileInfo(m)))
    }

    response
  }

  override def get(name: String): RestResponse = {
    val service = serviceInterface.get(name)

    val response = service match {
      case Some(x) =>
        val source = FileIO.fromPath(Paths.get(x.file.get.getAbsolutePath))

        CustomFile(name, source)
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.custom.files.file.notfound", name)))
    }

    response
  }

  override def delete(name: String): RestResponse = {
    val deleteResponse = serviceInterface.delete(name)
    val response: RestResponse = deleteResponse match {
      case Right(isDeleted) =>
        if (isDeleted)
          OkRestResponse(MessageResponseEntity(createMessage("rest.custom.files.file.deleted", name)))
        else
          NotFoundRestResponse(MessageResponseEntity(createMessage("rest.custom.files.file.notfound", name)))
      case Left(message) =>
        UnprocessableEntityRestResponse(MessageResponseEntity(message))
    }

    response
  }

  override def create(serializedEntity: String): RestResponse = ???
}