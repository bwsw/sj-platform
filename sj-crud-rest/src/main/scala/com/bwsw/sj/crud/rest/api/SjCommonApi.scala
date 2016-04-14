package com.bwsw.sj.crud.rest.api

import java.io.File

import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model.{HttpResponse, HttpEntity}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.Source
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.entities.Response
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import org.apache.commons.io.FileUtils

/**
  * Rest-api for common jars
  *
  * Created: 08/04/2016
  * @author Kseniya Tomskikh
  */
trait SjCommonApi extends Directives with SjCrudValidator {

  val commonApi = {
    pathPrefix("common") {
      path(Segment) { (name: String) =>
        pathSuffix(Segment) { (version: String) =>
          pathEndOrSingleSlash {
            val fileMetadata = fileMetadataDAO.retrieve(name, version)
            validate(fileMetadata != null, s"Jar not found") {
              val filename = fileMetadata.filename
              get {
                val jarFile = storage.get(filename, s"tmp/$filename")
                if (jarFile != null && jarFile.exists()) {
                  complete(HttpResponse(
                    headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))),
                    entity = HttpEntity.Chunked.fromData(`application/java-archive`, Source.file(jarFile))
                  ))
                } else {
                  throw new BadRecordWithKey(s"Jar '$name' not found", name)
                }
              } ~
              delete {
                if (storage.delete(filename)) {
                  complete(HttpEntity(
                    `application/json`,
                    serializer.serialize(Response(200, null, s"Jar by name $name and version $version has been deleted"))
                  ))
                } else {
                  throw new BadRecordWithKey(s"Jar name $name and version $version hasn't been found", name)
                }
              }
            }
          }
        }
      } ~
      pathEndOrSingleSlash {
        post {
          entity(as[FormData]) { formData =>
            uploadedFile("jar") {
              case (metadata: FileInfo, file: File) =>
                val uploadingFile = new File(metadata.fileName)
                FileUtils.copyFile(file, uploadingFile)
                storage.put(uploadingFile, metadata.fileName, null, "custom")
                complete(HttpEntity(
                  `application/json`,
                  serializer.serialize(Response(200, null, s"Ok"))
                ))
            }
          }
        } ~
        get {
          val files = fileMetadataDAO.retrieveAllByFiletype("custom")
          var msg = ""
          if (files.nonEmpty) {
            msg = s"Uploaded custom jars: ${files.map(_.metadata.get("metadata").get.name).mkString(", ")}"
          } else {
            msg = s"Uploaded custom jars have not been found "
          }
          complete(HttpEntity(
            `application/json`,
            serializer.serialize(Response(200, null, msg))
          ))
        }
      }
    }
  }
}
