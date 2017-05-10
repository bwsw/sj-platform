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
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.dal.model.ConfigurationSetting
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.crud.rest.RestLiterals
import com.bwsw.sj.crud.rest.exceptions.CustomJarNotFound
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import org.apache.commons.io.FileUtils

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * Rest-api for sj-platform executive units and custom files
  *
  * @author Kseniya Tomskikh
  */
trait SjCustomRoute extends Directives with SjCrudValidator {
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

  private def doesCustomJarExist(specification: Map[String, Any]) = {
    fileMetadataDAO.getByParameters(
      Map("filetype" -> "custom",
        "specification.name" -> specification("name").asInstanceOf[String],
        "specification.version" -> specification("version").asInstanceOf[String]
      )).nonEmpty
  }

  val customApi = {
    pathPrefix("custom") {
      pathPrefix("jars") {
        pathPrefix(Segment) { (name: String) =>
          pathEndOrSingleSlash {
            get {
              if (storage.exists(name)) {
                deletePreviousFiles()
                val jarFile = storage.get(name, RestLiterals.tmpDirectory + name)
                previousFilesNames.append(jarFile.getAbsolutePath)
                val source = FileIO.fromPath(Paths.get(jarFile.getAbsolutePath))
                complete(HttpResponse(
                  headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> name))),
                  entity = HttpEntity.Chunked.fromData(MediaTypes.`application/java-archive`, source)
                ))
              } else {
                val response = NotFoundRestResponse(MessageResponseEntity(createMessage("rest.custom.jars.file.notfound", name)))
                complete(restResponseToHttpResponse(response))
              }
            } ~
              delete {
                val fileMetadatas = fileMetadataDAO.getByParameters(Map("filetype" -> "custom", "filename" -> name))
                if (fileMetadatas.isEmpty) {
                  throw CustomJarNotFound(createMessage("rest.custom.jars.file.notfound", s"$name"), s"$name")
                }
                val fileMetadata = fileMetadatas.head

                var response: RestResponse = InternalServerErrorRestResponse(
                  MessageResponseEntity(s"Can't delete jar '$name' for some reason. It needs to be debugged")
                )

                if (storage.delete(name)) {
                  configService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, fileMetadata.specification.name + "-" + fileMetadata.specification.version))
                  response = OkRestResponse(
                    MessageResponseEntity(createMessage("rest.custom.jars.file.deleted.by.filename", name))
                  )
                }

                complete(restResponseToHttpResponse(response))
              }
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
                  val jarFile = storage.get(filename, RestLiterals.tmpDirectory + filename)
                  previousFilesNames.append(jarFile.getAbsolutePath)
                  val source = FileIO.fromPath(Paths.get(jarFile.getAbsolutePath))
                  complete(HttpResponse(
                    headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))),
                    entity = HttpEntity.Chunked.fromData(MediaTypes.`application/java-archive`, source)
                  ))
                } ~
                  delete {
                    var response: RestResponse = InternalServerErrorRestResponse(
                      MessageResponseEntity(s"Can't delete jar '${filename}' for some reason. It needs to be debuged")
                    )

                    if (storage.delete(filename)) {
                      configService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, name + "-" + version))
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
                  val result = Try {
                    var response: RestResponse = ConflictRestResponse(MessageResponseEntity(
                      createMessage("rest.custom.jars.file.exists", metadata.fileName)))

                    if (!storage.exists(metadata.fileName)) {
                      response = BadRequestRestResponse(MessageResponseEntity(getMessage("rest.errors.invalid.specification")))

                      if (checkCustomFileSpecification(file)) {
                        val specification = getSpecification(file)
                        response = ConflictRestResponse(MessageResponseEntity(
                          createMessage("rest.custom.jars.exists", metadata.fileName)))

                        if (!doesCustomJarExist(specification)) {
                          val uploadingFile = new File(metadata.fileName)
                          FileUtils.copyFile(file, uploadingFile)
                          storage.put(uploadingFile, metadata.fileName, specification, "custom")
                          val name = specification("name").toString + "-" + specification("version").toString
                          val customJarConfigElement = new ConfigurationSetting(
                            createConfigurationSettingName(ConfigLiterals.systemDomain, name),
                            metadata.fileName,
                            ConfigLiterals.systemDomain
                          )
                          configService.save(customJarConfigElement)
                          response = OkRestResponse(MessageResponseEntity(
                            createMessage("rest.custom.jars.file.uploaded", metadata.fileName)))
                        }
                      }
                    }

                    response
                  }
                  file.delete()
                  result match {
                    case Success(response) => complete(restResponseToHttpResponse(response))
                    case Failure(e) => throw e
                  }
              }
            } ~
              get {
                val files = fileMetadataDAO.getByParameters(Map("filetype" -> "custom"))
                val response = OkRestResponse(CustomJarsResponseEntity())
                if (files.nonEmpty) {
                  val jarsInfo = files.map(metadata =>
                    CustomJarInfo(metadata.specification.name, metadata.specification.version, metadata.length))
                  response.entity = CustomJarsResponseEntity(jarsInfo)
                }

                complete(restResponseToHttpResponse(response))
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
                    val jarFile = storage.get(filename, RestLiterals.tmpDirectory + filename)
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
