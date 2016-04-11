package com.bwsw.sj.crud.rest.api

import java.io.File

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{RequestContext, Directives}
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.entities.Response
import com.bwsw.sj.crud.rest.SjCrudValidator
import akka.http.scaladsl.model.headers._
import org.apache.commons.io.FileUtils

import akka.stream.scaladsl._

trait SjModulesApi extends Directives with SjCrudValidator {

  val modulesApi = {
    pathPrefix("modules") {
      pathEndOrSingleSlash {
        post {
          uploadedFile("jar") {
            case (metadata: FileInfo, file: File) =>
              if (metadata.fileName.endsWith(".jar")) {
                val specification = checkJarFile(file)
                val uploadingFile = new File(metadata.fileName)
                FileUtils.copyFile(file, uploadingFile)
                storage.put(uploadingFile, metadata.fileName, specification, "module")
                complete(HttpEntity(
                  `application/json`,
                  serializer.serialize(Response("200", null, "Jar file has been uploaded"))
                ))
              } else {
                file.delete()
                throw new BadRecordWithKey(s"File: ${metadata.fileName} hasn't the .jar extension", metadata.fileName)
              }
          }
        } ~
        get {
          val files = fileMetadataDAO.retrieveAllByFiletype("module")
          var msg = ""
          if (files.nonEmpty) {
            msg = s"Uploaded modules: ${files.map(_.metadata.get("metadata").get.name).mkString(", ")}"
          } else {
            msg = s"Uploaded modules have not been found "
          }
          complete(HttpEntity(
            `application/json`,
            serializer.serialize(Response("200", null, msg))
          ))
        }
      } ~
      pathPrefix(Segment) { (typeName: String) =>
        validate(checkModuleType(typeName), s"Module type $typeName is not exist") {
          pathPrefix(Segment) { (name: String) =>
            pathPrefix(Segment) { (version: String) =>
              pathSuffix("instance") {
                post { (ctx: RequestContext) =>
                  //todo validating module
                  ctx.complete(HttpEntity(
                    `application/json`,
                    serializer.serialize(Response("200", null, s"Module validated"))
                  ))
                }
              } ~
              pathSuffix("execute") {
                  post {
                    //todo executing module
                    complete(HttpEntity(
                      `application/json`,
                      serializer.serialize(Response("200", null, s"Module validated"))
                    ))
                  }
                } ~
              pathEndOrSingleSlash {
                val fileMetadata = fileMetadataDAO.retrieve(name, typeName, version)
                validate(fileMetadata != null, s"Module not found") {
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
                        serializer.serialize(Response("200", null, s"Module $name for type $typeName has been deleted"))
                      ))
                    } else {
                      throw new BadRecordWithKey(s"Module $name hasn't been found", name)
                    }
                  }
                }
              }
            }
          } ~
          get {
            val files = fileMetadataDAO.retrieveAllByModuleType(typeName)
            var msg = ""
            if (files.nonEmpty) {
              msg = s"Uploaded modules for type $typeName: ${files.map(_.metadata.get("metadata").get.name).mkString(", ")}"
            } else {
              msg = s"Uploaded modules for type $typeName have not been found "
            }
            complete(HttpEntity(
              `application/json`,
              serializer.serialize(Response("200", null, msg))
            ))
          }
        }
      }
    }
  }
}
