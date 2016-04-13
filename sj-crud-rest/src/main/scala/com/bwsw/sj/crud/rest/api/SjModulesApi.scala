package com.bwsw.sj.crud.rest.api

import java.io.{FileNotFoundException, File}


import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{RequestContext, Directives}
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.common.exceptions.{InstanceException, BadRecordWithKey}
import com.bwsw.sj.common.DAL.ConnectionRepository
import com.bwsw.sj.common.entities._
import com.bwsw.sj.common.module.StreamingValidator
import com.bwsw.sj.crud.rest.SjCrudValidator
import akka.http.scaladsl.model.headers._
import com.bwsw.sj.crud.rest.validator.StreamingModuleValidator
import org.apache.commons.io.FileUtils

import akka.stream.scaladsl._

import scala.reflect.ManifestFactory
import scala.reflect.runtime.universe
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
              validate(fileMetadata != null, s"Module $typeName-$name-$version not found") {
                val specification = fileMetadata.metadata.get("metadata").get
                val filename = fileMetadata.filename

                pathPrefix("instance") {
                  pathEndOrSingleSlash {
                    post { (ctx: RequestContext) =>
                      val options = deserializeOptions(getEntityFromContext(ctx), typeName)
                      val errors = validateOptions(options, typeName)
                      if (errors.nonEmpty) {
                        val validatorClassName = specification.validateClass
                        val jarFile = storage.get(filename, s"tmp/$filename")
                        if (jarFile != null && jarFile.exists()) {
                          if (moduleValidate(jarFile, validatorClassName, options.options)) {
                            val nameInstance = saveInstance(options, typeName, name, version)
                            ctx.complete(HttpEntity(
                              `application/json`,
                              serializer.serialize(Response(200, nameInstance, s"Instance for module $typeName-$name-$version is created"))
                            ))
                          } else {
                            throw new InstanceException(s"Cannot create instance of module. Request has incrorrect options attrubute", s"$typeName-$name-$version")
                          }
                        } else {
                          throw new FileNotFoundException(s"Jar for module $typeName-$name-$version not found in storage")
                        }
                      } else {
                        throw new InstanceException(s"Cannot create instance of module. Errors: ${errors.mkString("\n")}", s"$typeName-$name-$version")
                      }
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

  /**
    * Deserialization json string to object
    *
    * @param options - json-string
    * @param moduleType - type name of module
    * @return - json as object InstanceMetadata
    */
  def deserializeOptions(options: String, moduleType: String) = {
    /*val entityClassName = conf.getString("modules." + moduleType + ".entity-class")
    val mirror = universe.runtimeMirror(getClass.getClassLoader)
    val entityClazz = Class.forName(entityClassName)*/
    if (moduleType.equals("regular-streaming")) {
      serializer.deserialize[RegularInstanceMetadata](options)
    } else {
      serializer.deserialize[WindowedInstanceMetadata](options)
    }
  }

  /**
    * Validation of options for created module instance
    *
    * @param options - options for instance
    * @param moduleType - type name of module
    * @return - list of errors
    */
  def validateOptions(options: InstanceMetadata, moduleType: String) = {
    val validatorClassName = conf.getString("modules." + moduleType + ".validator-class")
    val collectionName = conf.getString("modules." + moduleType + ".collection-name")
    val validatorClazz = Class.forName(validatorClassName)
    val validator = validatorClazz.newInstance().asInstanceOf[StreamingModuleValidator]
    validator.validate(options, collectionName)
  }

  /**
    * Save instance of module to mongo db
    *
    * @param options - options for instance
    * @param moduleType - type name of module
    * @param moduleName - name of module
    * @param moduleVersion - version of module
    * @return - name of created entity
    */
  def saveInstance(options: InstanceMetadata, moduleType: String, moduleName: String, moduleVersion: String) = {
    val collectionName = conf.getString("modules." + moduleType + ".collection-name")
    val instanceDAO = ConnectionRepository.getInstanceDAO(collectionName)
    options.uuid = java.util.UUID.randomUUID().toString
    options.moduleName = moduleName
    options.moduleVersion = moduleVersion
    instanceDAO.create(options)
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
