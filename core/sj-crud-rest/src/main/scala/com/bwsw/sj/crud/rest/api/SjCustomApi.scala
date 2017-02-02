package com.bwsw.sj.crud.rest.api

import java.io.{File, FileOutputStream}
import java.nio.file.Paths

import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import com.bwsw.sj.common.DAL.model.ConfigurationSetting
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.crud.rest.RestLiterals
import com.bwsw.sj.crud.rest.exceptions.CustomJarNotFound
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Rest-api for sj-platform executive units and custom files
  *
  * @author Kseniya Tomskikh
  */
trait SjCustomApi extends Directives with SjCrudValidator {
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
                val response = NotFoundRestResponse(Map("message" -> createMessage("rest.custom.jars.file.notfound", name)))
                complete(restResponseToHttpResponse(response))
              }
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
                      Map("message" -> s"Can't delete jar '${filename}' for some reason. It needs to be debuged")
                    )

                    if (storage.delete(filename)) {
                      configService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, name + "-" + version))
                      response = OkRestResponse(
                        Map("message" -> createMessage("rest.custom.jars.file.deleted", name, version))
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
                  try {
                    var response: RestResponse = ConflictRestResponse(Map("message" ->
                      createMessage("rest.custom.jars.file.exists", metadata.fileName)))

                    if (!storage.exists(metadata.fileName)) {
                      response = BadRequestRestResponse(Map("message" -> getMessage("rest.errors.invalid.specification")))

                      if (checkSpecification(file)) {
                        val specification = getSpecification(file)
                        response = ConflictRestResponse(Map("message" ->
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
                          response = OkRestResponse(Map("message" ->
                            createMessage("rest.custom.jars.file.uploaded", metadata.fileName)))
                        }
                      }
                    }

                    complete(restResponseToHttpResponse(response))
                  } finally {
                    file.delete()
                  }
              }
            } ~
              get {
                val files = fileMetadataDAO.getByParameters(Map("filetype" -> "custom"))
                val response = OkRestResponse(Map("custom-jars" -> mutable.Buffer()))
                if (files.nonEmpty) {
                  response.entity = Map("custom-jars" -> files.map(metadata =>
                    Map("name" -> metadata.specification.name,
                      "version" -> metadata.specification.version))
                  )
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
                    val file = allParts("file").asInstanceOf[File]
                    val description = if (allParts.isDefinedAt("description")) {
                      allParts("description").asInstanceOf[String]
                    } else ""
                    var response: RestResponse = ConflictRestResponse(Map("message" ->
                      createMessage("rest.custom.files.file.exists", filename.get)))

                    if (!storage.exists(filename.get)) {
                      val uploadingFile = new File(filename.get)
                      FileUtils.copyFile(file, uploadingFile)
                      storage.put(uploadingFile, filename.get, Map("description" -> description), "custom-file")
                      uploadingFile.delete()

                      response = OkRestResponse(Map("message" ->
                        createMessage("rest.custom.files.file.uploaded", filename.get)))
                    }
                    file.delete()

                    complete(restResponseToHttpResponse(response))

                  case Failure(throwable) =>
                    file.delete()
                    throw throwable
                }
              }
            } ~
              get {
                val files = fileMetadataDAO.getByParameters(Map("filetype" -> "custom-file"))
                val response = OkRestResponse(Map("custom-files" -> mutable.Buffer()))
                if (files.nonEmpty) {
                  response.entity = Map("custom-files" -> files.map(metadata =>
                    Map("name" -> metadata.filename,
                      "description" -> metadata.specification.description,
                      "upload-date" -> metadata.uploadDate.toString))
                  )
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
                    val response: RestResponse = NotFoundRestResponse(Map("message" ->
                      createMessage("rest.custom.files.file.notfound", filename)))

                    complete(restResponseToHttpResponse(response))
                  }
                } ~
                  delete {
                    var response: RestResponse = NotFoundRestResponse(Map("message" ->
                      createMessage("rest.custom.files.file.notfound", filename)))

                    if (storage.delete(filename)) {
                      response = OkRestResponse(Map("message" -> createMessage("rest.custom.files.file.deleted", filename)))
                    }

                    complete(restResponseToHttpResponse(response))
                  }
              }
            }
        }
    }
  }
}
