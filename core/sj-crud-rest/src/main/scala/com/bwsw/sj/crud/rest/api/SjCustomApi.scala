package com.bwsw.sj.crud.rest.api

import java.io.{FileOutputStream, File}
import java.text.MessageFormat

import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.DAL.model.ConfigSetting
import com.bwsw.sj.crud.rest.entities.ProtocolResponse
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import org.apache.commons.io.FileUtils

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Rest-api for custom jars
 *
 * Created: 08/04/2016
 *
 * @author Kseniya Tomskikh
 */
trait SjCustomApi extends Directives with SjCrudValidator {

  val customApi = {
    pathPrefix("custom") {
      pathPrefix("jars") {
        pathPrefix(Segment) { (name: String) =>
          pathEndOrSingleSlash {
            get {
              val jarFile = storage.get(name, s"tmp/rest/$name")
              if (jarFile != null && jarFile.exists()) {
                complete(HttpResponse(
                  headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> name))),
                  entity = HttpEntity.Chunked.fromData(`application/java-archive`, Source.file(jarFile))
                ))
              } else {
                throw new BadRecordWithKey(MessageFormat.format(
                  messages.getString("rest.custom.jars.file.notfound"), name), name)
              }
            }
          } ~
          pathSuffix(Segment) { (version: String) =>
            pathEndOrSingleSlash {
              val fileMetadatas = fileMetadataDAO.getByParameters(Map("specification.name" -> name, "specification.version" -> version))
              if (fileMetadatas.isEmpty) {
                throw new BadRecordWithKey(MessageFormat.format(
                  messages.getString("rest.custom.jars.file.notfound"), name), name)
              }
              val filename = fileMetadatas.head.filename
              get {
                val jarFile = storage.get(filename, s"tmp/$filename")
                if (jarFile != null && jarFile.exists()) {
                  complete(HttpResponse(
                    headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))),
                    entity = HttpEntity.Chunked.fromData(`application/java-archive`, Source.file(jarFile))
                  ))
                } else {
                  throw new BadRecordWithKey(MessageFormat.format(
                    messages.getString("rest.custom.jars.file.notfound"), name), name)
                }
              } ~
              delete {
                if (storage.delete(filename)) {
                  configService.delete("system" + "." + name + "-" + version)
                  val response = ProtocolResponse(
                    200,
                    Map("message" -> MessageFormat.format(
                      messages.getString("rest.custom.jars.file.deleted"), name, version
                    ))
                  )
                  complete(HttpEntity(
                    `application/json`,
                    serializer.serialize(response)
                  ))
                } else {
                  throw new BadRecordWithKey(MessageFormat.format(
                    messages.getString("rest.custom.jars.file.cannot.delete"), name, version),
                    name)
                }
              }
            }
          }
        } ~
        pathEndOrSingleSlash {
          post {
            uploadedFile("jar") {
              case (metadata: FileInfo, file: File) =>
                val specification = checkCustomJarFile(file)
                val uploadingFile = new File(metadata.fileName)
                FileUtils.copyFile(file, uploadingFile)
                storage.put(uploadingFile, metadata.fileName, specification, "custom")
                val customJarConfigElement = new ConfigSetting(
                  "system" + "." + specification("name").toString + "-" + specification("version").toString,
                  metadata.fileName,
                  "system"
                )
                configService.save(customJarConfigElement)
                val response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                  messages.getString("rest.custom.jars.file.uploaded"),
                  metadata.fileName)
                ))

                complete(HttpEntity(
                  `application/json`,
                  serializer.serialize(response)
                ))
            }
          } ~
          get {
            val files = fileMetadataDAO.getByParameters(Map("filetype" -> "custom"))
            var response: ProtocolResponse = null
            if (files.nonEmpty) {
              val entity = Map("custom-jars" -> files.map(metadata =>
                Map("name" -> metadata.specification.name,
                  "version" -> metadata.specification.version))
              )
              response = ProtocolResponse(200, entity)
            } else {
              response = ProtocolResponse(200, Map("message" -> messages.getString("rest.custom.jars.notfound")))
            }
            complete(HttpEntity(
              `application/json`,
              serializer.serialize(response)
            ))
          }
        }
      } ~
      pathPrefix("files") {
        pathEndOrSingleSlash {
          post {
            entity(as[Multipart.FormData]) { (entity: Multipart.FormData) =>
              val parts: Source[BodyPart, Any] = entity.parts
              var filename = ""
              var description = ""
              val partsResult = parts.runForeach { (part: BodyPart) =>
                if (part.name.equals("file")) {
                  filename = part.filename.get
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
                } else if (part.name.equals("description")) {
                  val bytes = part.entity.dataBytes

                  def writeContent(array: Array[Byte], byteString: ByteString): Array[Byte] = {
                    val byteArray: Array[Byte] = byteString.toArray
                    description = new String(byteArray)
                    array ++ byteArray
                  }

                  val future = bytes.runFold(Array[Byte]())(writeContent)
                  Await.result(future, 30.seconds)
                }
              }
              Await.result(partsResult, 30.seconds)

              val uploadingFile = new File(filename)
              val spec = Map("description" -> description)
              storage.put(uploadingFile, filename, spec, "custom-file")
              uploadingFile.delete()

              val response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                messages.getString("rest.custom.files.file.uploaded"), filename
              )))

              complete(HttpEntity(
                `application/json`,
                serializer.serialize(response)
              ))
            }
          } ~
          get {
            val files = fileMetadataDAO.getByParameters(Map("filetype" -> "custom-file"))
            var response: ProtocolResponse = null
            if (files.nonEmpty) {
              val entity = Map("custom-files" -> files.map(metadata =>
                Map("name" -> metadata.filename,
                  "description" -> metadata.specification.description,
                  "upload-date" -> metadata.uploadDate.toString))
              )
              response = ProtocolResponse(200, entity)
            } else {
              response = ProtocolResponse(200, Map("message" -> messages.getString("rest.custom.files.notfound")))
            }
            complete(HttpEntity(
              `application/json`,
              serializer.serialize(response)
            ))
          }
        } ~
        pathPrefix(Segment) { (name: String) =>
          val file = storage.get(name, s"tmp/rest/$name")
          if (file == null || !file.exists()) {
            throw new BadRecordWithKey(MessageFormat.format(
              messages.getString("rest.custom.files.file.notfound"), name),
              name)
          }
          pathEndOrSingleSlash {
            get {
              complete(HttpResponse(
                headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> name))),
                entity = HttpEntity.Chunked.fromData(`application/x-www-form-urlencoded`, Source.file(file))
              ))
            } ~
            delete {
              storage.delete(name)
              val response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                messages.getString("rest.custom.files.file.deleted"), name
              )))

              complete(HttpEntity(
                `application/json`,
                serializer.serialize(response)
              ))
            }
          }
        }
      }
    }
  }
}
