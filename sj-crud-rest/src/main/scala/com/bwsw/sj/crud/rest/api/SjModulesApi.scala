package com.bwsw.sj.crud.rest.api

import java.io.{FileNotFoundException, File}

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{RequestContext, Directives}
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.common.exceptions.BadRecordWithKey
import com.bwsw.sj.common.entities.{FileMetadata, Response}
import com.bwsw.sj.common.module.StreamingValidator
import com.bwsw.sj.crud.rest.{ModuleValidator, SjCrudValidator}
import akka.http.scaladsl.model.headers._
import org.apache.commons.io.FileUtils

import akka.stream.scaladsl._

import scala.reflect.internal.util.ScalaClassLoader
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

/**
  * Rest-api for module-jars
  *
  * Created: 04/08/2016
 *
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

                pathPrefix("instance") {
                  pathEndOrSingleSlash {
                    post { (ctx: RequestContext) =>
                      val options = serializer.deserialize[Map[String, Any]](getEntityFromContext(ctx))
                      val validatorClassName = specification.validateClass
                      val jarFile = storage.get(filename, s"tmp/$filename")

                      var msg = ""
                      if (jarFile != null && jarFile.exists()) {
                        if (paramsValidate(options, typeName)
                          && moduleValidate(jarFile, validatorClassName, options.get("options").get.asInstanceOf[Map[String, Any]])) {
                          msg = s"Module is instanced"
                        } else {
                          msg = s"Cannot instancing of module"
                        }
                      } else {
                        throw new FileNotFoundException("Jar not found in storage")
                      }

                      ctx.complete(HttpEntity(
                        `application/json`,
                        serializer.serialize(Response(200, null, msg))
                      ))
                    } ~
                    get {
                      complete(HttpEntity(
                        `application/json`,
                        serializer.serialize(Response(200, null, "Ok"))
                      ))
                    }
                  } ~
                  path(Segment) { (instanceName: String) =>
                    pathSuffix("start") {
                      get {
                        //todo
                        complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(Response(200, null, "Ok"))
                        ))
                      }
                    } ~
                    pathSuffix("stop") {
                      get {
                        //todo
                        complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(Response(200, null, "Ok"))
                        ))
                      }
                    } ~
                    get {
                      //todo get this instance
                      complete(HttpEntity(
                        `application/json`,
                        serializer.serialize(Response(200, null, "Ok"))
                      ))
                    } ~
                    delete {
                      //todo delete this instance
                      complete(HttpEntity(
                        `application/json`,
                        serializer.serialize(Response(200, null, "Ok"))
                      ))
                    }

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

  def paramsValidate(options: Map[String, Any], moduleType: String) = {
    val validatorClassName = conf.getString("modules.validator." + moduleType + ".scala")
    val clazz = Class.forName(validatorClassName)
    val validator = clazz.newInstance().asInstanceOf[ModuleValidator]
    val errors = validator.validate(options)
    errors.isEmpty
  }

  /**
    * Create instance of module
    *
    * @param file - jar-file
    * @param validateClassName - validator classname of module
    * @param options - start options for module
    * @return - true, if options for module is valid
    */
  def moduleValidate(file: File, validateClassName: String, options: Map[String, Any]) = {
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(validateClassName)
    val validator = clazz.newInstance().asInstanceOf[StreamingValidator]
    validator.validate(options)
  }
}
