package com.bwsw.sj.crud.rest.api

import java.io.{FileNotFoundException, File}

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{RequestContext, Directives}
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.entities.{FileMetadata, Response}
import com.bwsw.sj.common.module.SparkStreamingValidator
import com.bwsw.sj.common.module.entities.LaunchParameters
import com.bwsw.sj.crud.rest.SjCrudValidator
import akka.http.scaladsl.model.headers._
import org.apache.commons.io.FileUtils

import akka.stream.scaladsl._

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

/**
  * Rest-api for module-jars
  *
  * Created: 04/08/2016
  * @author Kseniya Tomskikh
  */
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
                  serializer.serialize(Response(200, null, "Jar file has been uploaded"))
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
            serializer.serialize(Response(200, null, msg))
          ))
        }
      } ~
      pathPrefix(Segment) { (typeName: String) =>
        validate(checkModuleType(typeName), s"Module type $typeName is not exist") {
          pathPrefix(Segment) { (name: String) =>
            pathPrefix(Segment) { (version: String) =>
              val fileMetadata: FileMetadata = fileMetadataDAO.retrieve(name, typeName, version)
              validate(fileMetadata != null, s"Module not found") {
                val specification = fileMetadata.metadata.get("metadata").get
                val filename = fileMetadata.filename

                pathSuffix("instance") {
                  post { (ctx: RequestContext) =>
                    val options = serializer.deserialize[LaunchParameters](getEntityFromContext(ctx))
                    val validatorClassName = specification.validateClass
                    val jarFile = storage.get(filename, s"tmp/$filename")

                    var msg = ""
                    if (jarFile != null && jarFile.exists()) {
                      if (moduleInstance(jarFile, validatorClassName, options)) {
                        msg = s"Module is instance"
                      } else {
                        msg = s"Cannot instance of module"
                      }
                    } else {
                      throw new FileNotFoundException("Jar not found in storage")
                    }

                    ctx.complete(HttpEntity(
                      `application/json`,
                      serializer.serialize(Response(200, null, msg))
                    ))
                  }
                } ~
                pathSuffix("execute") {
                    post {
                      //todo executing module
                      complete(HttpEntity(
                        `application/json`,
                        serializer.serialize(Response(200, null, s"Module executed"))
                      ))
                    }
                  } ~
                pathSuffix("specification") {
                  pathEndOrSingleSlash {
                    get {
                      complete(HttpEntity(
                        `application/json`,
                        serializer.serialize(specification)
                      ))
                    }
                  }
                } ~
                pathEndOrSingleSlash {
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
                        serializer.serialize(Response(200, null, s"Module $name for type $typeName has been deleted"))
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
              msg = s"Uploaded modules for type $typeName: ${files.map(_.metadata.get("metadata").get.name).mkString(",\n")}"
            } else {
              msg = s"Uploaded modules for type $typeName have not been found "
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

  /**
    * Create instance of module
    *
    * @param file - jar-file
    * @param validateClassName - validator classname of module
    * @param options - start options for module
    * @return - true, if options for module is valid
    */
  def moduleInstance(file: File, validateClassName: String, options: LaunchParameters) = {
    try {
      val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
      val clazz = loader.loadClass(validateClassName)
      val inst = clazz.newInstance()
      val validator = inst.asInstanceOf[SparkStreamingValidator]
      validator.validate(options)
    } catch {
      case ex: Exception =>
        println(ex)
        false
    }
  }
}
