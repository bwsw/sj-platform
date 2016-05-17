package com.bwsw.sj.crud.rest.api

import java.io.{FileNotFoundException, File}

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{RequestContext, Directives}
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.common.exceptions.{InstanceException, BadRecordWithKey}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.module.StreamingValidator
import com.bwsw.sj.crud.rest.entities._
import akka.http.scaladsl.model.headers._
import com.bwsw.sj.crud.rest.runner.{InstanceDestroyer, InstanceStopper, InstanceStarter}
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.module.StreamingModuleValidator
import org.apache.commons.io.FileUtils

import akka.stream.scaladsl._

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

/**
  * Rest-api for module-jars
  *
  * Created: 08/04/2016
  *
  * @author Kseniya Tomskikh
  */
trait SjModulesApi extends Directives with SjCrudValidator {
  import com.bwsw.sj.common.ModuleConstants._
  import com.bwsw.sj.crud.rest.utils.ConvertUtil._

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
          val files = fileMetadataDAO.getByParameters(Map("filetype" -> "module"))
          var msg = ""
          if (files.nonEmpty) {
            msg = s"Uploaded modules: ${files.map(
              s => s"${s.specification.moduleType} - ${s.specification.name} - ${s.specification.version}"
            ).mkString(",\n")}"
          } else {
            msg = s"Uploaded modules have not been found "
          }
          complete(HttpEntity(
            `application/json`,
            serializer.serialize(Response(200, null, msg))
          ))
        }
      } ~
      pathPrefix(Segment) { (moduleType: String) =>
        validate(checkModuleType(moduleType), s"Module type $moduleType is not exist") {
          pathPrefix(Segment) { (moduleName: String) =>
            pathPrefix(Segment) { (moduleVersion: String) =>
              val fileMetadatas = fileMetadataDAO.getByParameters(Map("specification.name" -> moduleName,
                "specification.module-type" -> moduleType,
                "specification.version" -> moduleVersion)
              )
              val fileMetadata = fileMetadatas.head

              validate(fileMetadata != null, s"Module $moduleType-$moduleName-$moduleVersion not found") {
                val fileSpecification = fileMetadata.specification
                val specification = specificationToSpecificationData(fileSpecification)
                val filename = fileMetadata.filename

                pathPrefix("instance") {
                  pathEndOrSingleSlash {
                    post { (ctx: RequestContext) =>
                      val instanceMetadata = deserializeOptions(getEntityFromContext(ctx), moduleType)
                      val (errors, validatedInstance) = validateOptions(instanceMetadata, moduleType)
                      if (errors.isEmpty) {
                        val validatorClassName = specification.validateClass
                        val jarFile = storage.get(filename, s"tmp/$filename")
                        if (jarFile != null && jarFile.exists()) {
                          if (moduleValidate(jarFile, validatorClassName, validatedInstance.options)) {
                            val nameInstance = saveInstance(validatedInstance, moduleType, moduleName, moduleVersion)
                            ctx.complete(HttpEntity(
                              `application/json`,
                              serializer.serialize(Response(200, nameInstance, s"Instance for module $moduleType-$moduleName-$moduleVersion is created"))
                            ))
                          } else {
                            throw new InstanceException(s"Cannot create instance of module. Request has incrorrect options attrubute",
                              s"$moduleType-$moduleName-$moduleVersion")
                          }
                        } else {
                          throw new FileNotFoundException(s"Jar for module $moduleType-$moduleName-$moduleVersion not found in storage")
                        }
                      } else {
                        throw new InstanceException(s"Cannot create instance of module. Errors: ${errors.mkString("\n")}",
                          s"$moduleType-$moduleName-$moduleVersion")
                      }
                    } ~
                    get {
                      val instances = instanceDAO.getByParameters(Map("module-name" -> moduleName,
                        "module-type" -> moduleType,
                        "module-version" -> moduleVersion)
                      )
                      var msg = ""
                      if (instances.nonEmpty) {
                        msg = serializer.serialize(instances.map(i => convertModelInstanceToApiInstance(i)))
                      } else {
                        msg =  serializer.serialize(Response(200, s"$moduleType-$moduleName-$moduleVersion",
                          s"Instances for $moduleType-$moduleName-$moduleVersion not found"))
                      }
                      complete(HttpEntity(`application/json`, msg))
                    }
                  } ~
                  pathPrefix(Segment) { (instanceName: String) =>
                    val instance = instanceDAO.get(instanceName)
                    validate(instance != null, s"Instance for name $instanceName has not been found!") {
                      pathEndOrSingleSlash {
                        get {
                          complete(HttpEntity(
                            `application/json`,
                            serializer.serialize(convertModelInstanceToApiInstance(instance))
                          ))
                        } ~
                        delete {
                          var msg = ""
                          if (instance.status.equals(stopped) || instance.status.equals(failed)) {
                            instance.status = deleting
                            instanceDAO.save(instance)
                            destroyInstance(instance)
                            msg = s"Instance $instanceName is deleting"
                          } else if (instance.status.equals(ready)) {
                            instanceDAO.delete(instanceName)
                            msg = s"Instance $instanceName has been deleted"
                          } else {
                            msg = "Cannot deleting of instance. Instance is not stopped, failed or ready."
                          }
                          complete(HttpEntity(
                            `application/json`,
                            serializer.serialize(Response(200, instanceName, msg))
                          ))
                        }
                      } ~
                      path("start") {
                        pathEndOrSingleSlash {
                          get {
                            var msg = ""
                            if (instance.status.equals(ready) ||
                              instance.status.equals(stopped) ||
                              instance.status.equals(failed)) {
                              instance.status = starting
                              instanceDAO.save(instance)
                              startInstance(instance)
                              msg = "Instance is starting"
                            } else {
                              msg = "Cannot starting of instance. Instance already started."
                            }
                            complete(HttpEntity(
                              `application/json`,
                              serializer.serialize(Response(200, null, msg))
                            ))
                          }
                        }
                      } ~
                      path("stop") {
                        pathEndOrSingleSlash {
                          get {
                            var msg = ""
                            if (instance.status.equals(started)) {
                              instance.status = stopping
                              instanceDAO.save(instance)
                              stopInstance(instance)
                              msg = "Instance is stopping"
                            } else {
                              msg = "Cannot stopping of instance. Instance is not started."
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
                      throw new BadRecordWithKey(s"Jar '$moduleType-$moduleName-$moduleVersion' not found",
                        s"$moduleType - $moduleName - $moduleVersion")
                    }
                  } ~
                  delete {
                    val instances = instanceDAO.getByParameters(Map("module-name" -> moduleName,
                      "module-type" -> moduleType,
                      "module-version" -> moduleVersion)
                    )
                    if (instances.isEmpty) {
                      if (storage.delete(filename)) {
                        complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(Response(200, s"$moduleType-$moduleName-$moduleVersion",
                            s"Module $moduleName-$moduleVersion for type $moduleType has been deleted"))
                        ))
                      } else {
                        throw new BadRecordWithKey(s"Module $moduleType-$moduleName-$moduleVersion hasn't been found",
                          s"$moduleType-$moduleName-$moduleVersion")
                      }
                    } else {
                      throw new BadRecordWithKey(s"Cannot delete module $moduleType-$moduleName-$moduleVersion. Module has instances",
                        s"$moduleType-$moduleName-$moduleVersion")
                    }
                  }
                }
              }
            }
          } ~
          pathEndOrSingleSlash {
            get {
              val files = fileMetadataDAO.getByParameters(Map("specification.module-type" -> moduleType))
              var msg = ""
              if (files.nonEmpty) {
                msg = s"Uploaded modules for type $moduleType: ${
                  files.map(
                    s => s"${s.specification.name}-${s.specification.version}"
                  ).mkString(",\n")
                }"
              } else {
                msg = s"Uploaded modules for type $moduleType have not been found "
              }
              complete(HttpEntity(
                `application/json`,
                serializer.serialize(Response(200, null, msg))
              ))
            }
          }
        }
      } ~
      pathSuffix("instances") {
        get {
          val allInstances = instanceDAO.getAll
          if (allInstances.isEmpty) {
            complete(HttpEntity(
              `application/json`,
              serializer.serialize(Response(200, null, "Instances have not been found"))
            ))
          }
          complete(HttpEntity(
            `application/json`,
            serializer.serialize(allInstances.map(x => ShortInstanceMetadata(x.name,
              x.moduleType,
              x.moduleName,
              x.moduleVersion,
              x.description,
              x.status)))
          ))
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
    if (moduleType.equals(windowedType)) {
      serializer.deserialize[WindowedInstanceMetadata](options)
    } else {
      serializer.deserialize[InstanceMetadata](options)
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
    val validatorClassName = conf.getString(s"modules.$moduleType.validator-class")
    val instanceClassName = conf.getString(s"modules.$moduleType.entity-class")
    val validatorClass = Class.forName(validatorClassName)
    val validator = validatorClass.newInstance().asInstanceOf[StreamingModuleValidator]
    val instanceClass = Class.forName(instanceClassName)
    validator.validate(options, instanceClass.newInstance().asInstanceOf[RegularInstance])
  }

  /**
    * Save instance of module to db
    *
    * @param instance - entity of instance, which saving to db
    * @param moduleType - type name of module
    * @param moduleName - name of module
    * @param moduleVersion - version of module
    * @return - name of created entity
    */
  def saveInstance(instance: RegularInstance,
                     moduleType: String,
                     moduleName: String,
                     moduleVersion: String) = {
    instance.moduleName = moduleName
    instance.moduleVersion = moduleVersion
    instance.moduleType = moduleType
    instance.status = ready
    instanceDAO.save(instance)
    instance.name
  }

  /**
    * Create instance of module
    *
    * @param file - jar-file
    * @param validateClassName - validator classname of module
    * @param options - start options for module
    * @return - true, if options for module is valid
    */
  def moduleValidate(file: File, validateClassName: String, options: String) = {
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(validateClassName)
    val validator = clazz.newInstance().asInstanceOf[StreamingValidator]
    validator.validate(serializer.deserialize[Map[String, Any]](options))
  }

  /**
    * Starting generators (or scaling) for streams and framework for instance on mesos
    *
    * @param instance - Starting instance
    * @return
    */
  def startInstance(instance: RegularInstance) = {
    logger.debug(s"Starting application of instance ${instance.name}")
    new Thread(new InstanceStarter(instance, 1000)).start()
  }

  /**
    * Stopping instance application on mesos
    *
    * @param instance - Instance for stopping
    * @return - Message about successful stopping
    */
  def stopInstance(instance: RegularInstance) = {
    logger.debug(s"Stopping application of instance ${instance.name}")
    new Thread(new InstanceStopper(instance, 1000)).start()
  }

  /**
    * Destroying application on mesos
    *
    * @param instance - Instance for destroying
    * @return - Message of destroying instance
    */
  def destroyInstance(instance: RegularInstance) = {
    logger.debug(s"Destroying application of instance ${instance.name}")
    new Thread(new InstanceDestroyer(instance, 1000)).start()
  }

}
