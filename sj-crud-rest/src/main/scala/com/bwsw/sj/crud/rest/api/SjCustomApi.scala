package com.bwsw.sj.crud.rest.api

import java.io.File

import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.Source
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.DAL.model.ConfigElement
import com.bwsw.sj.crud.rest.entities.Response
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import org.apache.commons.io.FileUtils

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
              throw new BadRecordWithKey(s"Jar '$name' not found", name)
            }
          }
        } ~
        pathSuffix(Segment) { (version: String) =>
          pathEndOrSingleSlash {
            val fileMetadata = fileMetadataDAO.getByParameters(Map("specification.name" -> name, "specification.version" -> version)).head
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
          uploadedFile("jar") {
            case (metadata: FileInfo, file: File) =>
              val specification = checkCustomJarFile(file)
              val uploadingFile = new File(metadata.fileName)
              FileUtils.copyFile(file, uploadingFile)
              storage.put(uploadingFile, metadata.fileName, specification, "custom")
              val customJarConfigElement = new ConfigElement(
                specification("name").toString + "-" + specification("version").toString,
                metadata.fileName
              )
              configFileService.save(customJarConfigElement)
              complete(HttpEntity(
                `application/json`,
                serializer.serialize(Response(200, null, s"Custom jar is uploaded."))
              ))
          }
        } ~
        get {
          val files = fileMetadataDAO.getByParameters(Map("filetype" -> "custom"))
          var msg = ""
          if (files.nonEmpty) {
            msg = s"Uploaded custom jars: ${files.map(_.specification.name).mkString(", ")}"
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
