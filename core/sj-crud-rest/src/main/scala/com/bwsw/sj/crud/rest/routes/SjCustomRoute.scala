package com.bwsw.sj.crud.rest.routes

import java.io.{File, FileOutputStream}
import java.nio.file.Paths

import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.crud.rest.controller.CustomJarsController
import com.bwsw.sj.crud.rest.exceptions.CustomJarNotFound
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import org.apache.commons.io.FileUtils

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Rest-api for sj-platform executive units and custom files
  *
  * @author Kseniya Tomskikh
  */
trait SjCustomRoute extends Directives with SjCrudValidator {
  private val customJarsController = new CustomJarsController()
  private val previousFilesNames: ListBuffer[String] = ListBuffer[String]()

  private def fileUpload(filename: String, part: BodyPart) = {
    val fileOutput = new FileOutputStream(filename)
    val bytes = part.entity.dataBytes

    def writeFileOnLocal(array: Array[Byte], byteString: ByteString): Array[Byte] = {
      val byteArray: Array[Byte] = byteString.toArray
      fileOutput.write(byteArray)
      array ++ byteArray
    }

    val future = bytes.runFold(Array[Byte]())(writeFileOnLocal)
    Await.result(future, 30.seconds)
    fileOutput.close()
  }

  private def deletePreviousFiles() = {
    previousFilesNames.foreach(filename => {
      val file = new File(filename)
      if (file.exists()) file.delete()
    })
  }

  val customRoute = {
    pathPrefix("custom") {
      pathPrefix("jars") {
        pathPrefix(Segment) { (name: String) =>
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(customJarsController.get(name)))
            }
          } ~
            delete {
              complete(restResponseToHttpResponse(customJarsController.delete(name)))
            } ~
            pathSuffix(Segment) { (version: String) =>
              pathEndOrSingleSlash {
                val fileMetadatas = fileMetadataDAO.getByParameters(Map("filetype" -> "custom",
                  "specification.name" -> name,
                  "specification.version" -> version)
                )
                if (fileMetadatas.isEmpty) {
                  throw CustomJarNotFound(createMessage("rest.custom.jars.file.notfound", s"$name-$version"), s"$name-$version")
                }
                val filename = fileMetadatas.head.filename
                get {
                  deletePreviousFiles()
                  val jarFile = storage.get(filename, "/tmp/" + filename)
                  previousFilesNames.append(jarFile.getAbsolutePath)
                  val source = FileIO.fromPath(Paths.get(jarFile.getAbsolutePath))
                  complete(HttpResponse(
                    headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))),
                    entity = HttpEntity.Chunked.fromData(MediaTypes.`application/java-archive`, source)
                  ))
                } ~
                  delete {
                    var response: RestResponse = InternalServerErrorRestResponse(
                      MessageResponseEntity(s"Can't delete jar '$filename' for some reason. It needs to be debugged")
                    )

                    if (storage.delete(filename)) {
                      configService.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, name + "-" + version))
                      response = OkRestResponse(
                        MessageResponseEntity(createMessage("rest.custom.jars.file.deleted", name, version))
                      )
                    }

                    complete(restResponseToHttpResponse(response))
                  }
              }
            }
        } ~
          pathEndOrSingleSlash {
            post {
              uploadedFile("jar") {
                case (metadata: FileInfo, file: File) =>
                  val fileMetadataApi = new FileMetadataApi(metadata.fileName, Some(file))
                  complete(restResponseToHttpResponse(customJarsController.create(fileMetadataApi)))
              }
            } ~
              get {
                complete(restResponseToHttpResponse(customJarsController.getAll()))
              }
          }
      } ~
        pathPrefix("files") {
          pathEndOrSingleSlash {
            post {
              entity(as[Multipart.FormData]) { formData =>
                var filename: Option[String] = None
                val file = File.createTempFile("restCustomFile", "")
                val parts = formData.parts.mapAsync[(String, Any)](1) {

                  case b: BodyPart if b.name == "file" =>
                    filename = b.filename
                    b.entity.dataBytes.runWith(FileIO.toPath(file.toPath)).map(_ => b.name -> file)

                  case b: BodyPart =>
                    b.toStrict(2.seconds).map(strict => b.name -> strict.entity.data.utf8String)

                }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)

                onComplete(parts) {
                  case Success(allParts) =>
                    var response: RestResponse = BadRequestRestResponse(MessageResponseEntity(
                      getMessage("rest.custom.files.file.missing")))

                    if (filename.isDefined) {
                      val file = allParts("file").asInstanceOf[File]
                      val description = if (allParts.isDefinedAt("description")) {
                        allParts("description").asInstanceOf[String]
                      } else ""
                      response = ConflictRestResponse(MessageResponseEntity(
                        createMessage("rest.custom.files.file.exists", filename.get)))

                      if (!storage.exists(filename.get)) {
                        val uploadingFile = new File(filename.get)
                        FileUtils.copyFile(file, uploadingFile)
                        storage.put(uploadingFile, filename.get, Map("description" -> description), "custom-file")
                        uploadingFile.delete()

                        response = OkRestResponse(MessageResponseEntity(
                          createMessage("rest.custom.files.file.uploaded", filename.get)))
                      }
                      file.delete()
                    }

                    complete(restResponseToHttpResponse(response))

                  case Failure(throwable) =>
                    file.delete()
                    throw throwable
                }
              }
            } ~
              get {
                val files = fileMetadataDAO.getByParameters(Map("filetype" -> "custom-file"))
                val response = OkRestResponse(CustomFilesResponseEntity())
                if (files.nonEmpty) {
                  val filesInfo = files.map(metadata =>
                    CustomFileInfo(metadata.filename, metadata.specification.description, metadata.uploadDate.toString, metadata.length))
                  response.entity = CustomFilesResponseEntity(filesInfo)
                }

                complete(restResponseToHttpResponse(response))
              }
          } ~
            pathPrefix(Segment) { (filename: String) =>
              pathEndOrSingleSlash {
                get {
                  if (storage.exists(filename)) {
                    deletePreviousFiles()
                    val jarFile = storage.get(filename, "/tmp/" + filename)
                    previousFilesNames.append(jarFile.getAbsolutePath)
                    val source = FileIO.fromPath(Paths.get(jarFile.getAbsolutePath))
                    complete(HttpResponse(
                      headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))),
                      entity = HttpEntity.Chunked.fromData(ContentTypes.`application/octet-stream`, source)
                    ))
                  } else {
                    val response: RestResponse = NotFoundRestResponse(MessageResponseEntity(
                      createMessage("rest.custom.files.file.notfound", filename)))

                    complete(restResponseToHttpResponse(response))
                  }
                } ~
                  delete {
                    var response: RestResponse = NotFoundRestResponse(MessageResponseEntity(
                      createMessage("rest.custom.files.file.notfound", filename)))

                    if (storage.delete(filename)) {
                      response = OkRestResponse(MessageResponseEntity(createMessage("rest.custom.files.file.deleted", filename)))
                    }

                    complete(restResponseToHttpResponse(response))
                  }
              }
            }
        }
    }
  }
}
