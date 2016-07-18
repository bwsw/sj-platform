package com.bwsw.sj.crud.rest.api

import java.io.{FileNotFoundException, File}
import java.text.MessageFormat

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.server.{RequestContext, Directives}
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.common.exceptions.{InstanceException, BadRecordWithKey}
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.module.StreamingValidator
import com.bwsw.sj.crud.rest.entities._
import akka.http.scaladsl.model.headers._
import com.bwsw.sj.crud.rest.entities.module._
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
                val response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                  messages.getString("rest.modules.module.uploaded"),
                  metadata.fileName
                )))

                complete(HttpEntity(
                  `application/json`,
                  serializer.serialize(response)
                ))
              } else {
                file.delete()
                throw new BadRecordWithKey(MessageFormat.format(
                  messages.getString("rest.modules.modules.extension.unknown"),
                  metadata.fileName
                ), metadata.fileName)
              }
          }
        } ~
        get {
          val files = fileMetadataDAO.getByParameters(Map("filetype" -> "module"))
          var response: ProtocolResponse = null
          if (files.nonEmpty) {
            val entity = Map("modules" -> files.map(f => Map("module-type" -> f.specification.moduleType,
              "module-name" -> f.specification.name,
              "module-version" -> f.specification.version)))
            response = ProtocolResponse(200, entity)
          } else {
            response = ProtocolResponse(200, Map("message" -> messages.getString("rest.modules.notfound")))
          }
          complete(HttpEntity(
            `application/json`,
            serializer.serialize(response)
          ))
        }
      } ~
      pathPrefix("instances") {
        pathEndOrSingleSlash {
          get {
            val allInstances = instanceDAO.getAll

            var response: ProtocolResponse = null
            if (allInstances.isEmpty) {
              response = ProtocolResponse(200, Map("message" -> messages.getString("rest.modules.instances.notfound")))
            } else {
              val entity = Map("instances" -> allInstances.map(x => ShortInstanceMetadata(x.name,
                x.moduleType,
                x.moduleName,
                x.moduleVersion,
                x.description,
                x.status)))
              response = ProtocolResponse(200, entity)
            }
            complete(HttpEntity(
              `application/json`,
              serializer.serialize(response)
            ))
          }
        }
      } ~
      pathPrefix(Segment) { (moduleType: String) =>
        if (!checkModuleType(moduleType)) {
          throw new BadRecordWithKey(MessageFormat.format(messages.getString("rest.modules.type.unknown"), moduleType), moduleType)
        }
        pathPrefix(Segment) { (moduleName: String) =>
          pathPrefix(Segment) { (moduleVersion: String) =>
            val fileMetadatas = fileMetadataDAO.getByParameters(Map("specification.name" -> moduleName,
              "specification.module-type" -> moduleType,
              "specification.version" -> moduleVersion)
            )
            if (fileMetadatas.isEmpty) {
              throw new BadRecordWithKey(MessageFormat.format(
                  messages.getString("rest.modules.module.notfound"),
                  s"$moduleType-$moduleName-$moduleVersion"
                ), s"$moduleType - $moduleName - $moduleVersion")
            }
            val fileMetadata = fileMetadatas.head

            val fileSpecification = fileMetadata.specification
            val specification = specificationToSpecificationData(fileSpecification)
            val filename = fileMetadata.filename

            pathPrefix("instance") {
              pathEndOrSingleSlash {
                post { (ctx: RequestContext) =>
                  val instanceMetadata = deserializeOptions(getEntityFromContext(ctx), moduleType)
                  val (errors, validatedInstance) = validateOptions(instanceMetadata, specification, moduleType)
                  if (errors.isEmpty && validatedInstance != null) {
                    val validatorClassName = specification.validateClass
                    val jarFile = storage.get(filename, s"tmp/$filename")
                    if (jarFile != null && jarFile.exists()) {
                      if (moduleValidate(jarFile, validatorClassName, validatedInstance.options)) {
                        val nameInstance = saveInstance(
                          validatedInstance,
                          moduleType,
                          moduleName,
                          moduleVersion,
                          specification.engineName,
                          specification.engineVersion
                        )
                        val response = ProtocolResponse(200,
                          Map("message" -> MessageFormat.format(messages.getString("rest.modules.instances.instance.created"),
                            nameInstance, s"$moduleType-$moduleName-$moduleVersion"
                          )))
                        ctx.complete(HttpEntity(
                          `application/json`,
                          serializer.serialize(response)
                        ))
                      } else {
                        throw new InstanceException(messages.getString("rest.modules.instances.instance.create.incorrect"),
                          s"$moduleType-$moduleName-$moduleVersion")
                      }
                    } else {
                      throw new FileNotFoundException(MessageFormat.format(
                        messages.getString("rest.modules.module.jar.notfound"),
                        s"$moduleType-$moduleName-$moduleVersion"
                      ))
                    }
                  } else {
                    throw new InstanceException(MessageFormat.format(
                      messages.getString("rest.modules.instances.instance.cannot.create"),
                      errors.mkString("\n")
                    ), s"$moduleType-$moduleName-$moduleVersion")
                  }
                } ~
                  get {
                    val instances = instanceDAO.getByParameters(Map("module-name" -> moduleName,
                      "module-type" -> moduleType,
                      "module-version" -> moduleVersion)
                    )
                    var response: ProtocolResponse = null
                    if (instances.nonEmpty) {
                      response = ProtocolResponse(200, Map("instances" -> instances.map(i => convertModelInstanceToApiInstance(i))))
                    } else {
                      response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                        messages.getString("rest.modules.module.instances.notfound"),
                        s"$moduleType-$moduleName-$moduleVersion"
                      )))
                    }
                    complete(HttpEntity(`application/json`, serializer.serialize(response)))
                  }
              } ~
              pathPrefix(Segment) { (instanceName: String) =>
                val instance = instanceDAO.get(instanceName)
                if (instance == null) {
                  throw new BadRecordWithKey(MessageFormat.format(
                    messages.getString("rest.modules.module.instances.instance.notfound"),
                    instanceName
                  ), instanceName)
                }
                pathEndOrSingleSlash {
                  get {
                    val response = new ProtocolResponse(200, Map("instance" -> convertModelInstanceToApiInstance(instance)))
                    complete(HttpEntity(
                      `application/json`,
                      serializer.serialize(response)
                    ))
                  } ~
                  delete {
                    var msg = ""
                    if (instance.status.equals(stopped) || instance.status.equals(failed)) {
                      instance.status = deleting
                      instanceDAO.save(instance)
                      destroyInstance(instance)
                      msg = MessageFormat.format(messages.getString("rest.modules.instances.instance.deleting"), instanceName)
                    } else if (instance.status.equals(ready)) {
                      instanceDAO.delete(instanceName)
                      msg = MessageFormat.format(messages.getString("rest.modules.instances.instance.deleted"), instanceName)
                    } else {
                      msg = MessageFormat.format(messages.getString("rest.modules.instances.instance.cannot.delete"), instanceName)
                    }
                    complete(HttpEntity(
                      `application/json`,
                      serializer.serialize(ProtocolResponse(200, Map("message" -> msg)))
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
                        msg = MessageFormat.format(messages.getString("rest.modules.instances.instance.starting"), instanceName)/*"Instance is starting"*/
                      } else {
                        msg = MessageFormat.format(messages.getString("rest.modules.instances.instance.cannot.start"), instanceName)/*"Cannot starting of instance. Instance already started."*/
                      }
                      complete(HttpEntity(
                        `application/json`,
                        serializer.serialize(ProtocolResponse(200, Map("message" -> msg)))
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
                        msg = MessageFormat.format(messages.getString("rest.modules.instances.instance.stopping"), instanceName)/*"Instance is stopping"*/
                      } else {
                        msg = MessageFormat.format(messages.getString("rest.modules.instances.instance.cannot.stop"), instanceName)/*"Cannot stopping of instance. Instance is not started."*/
                      }
                      complete(HttpEntity(
                        `application/json`,
                        serializer.serialize(ProtocolResponse(200, Map("message" -> msg)))
                      ))
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
                    serializer.serialize(ProtocolResponse(200, Map("specification" -> specification)))
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
                  throw new BadRecordWithKey(MessageFormat.format(
                    messages.getString("rest.modules.module.jar.notfound"),
                    s"$moduleName-$moduleVersion"
                  ), s"$moduleType - $moduleName - $moduleVersion")
                }
              } ~
              delete {
                val instances = instanceDAO.getByParameters(Map("module-name" -> moduleName,
                  "module-type" -> moduleType,
                  "module-version" -> moduleVersion)
                )
                if (instances.isEmpty) {
                  if (storage.delete(filename)) {
                    val response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                      messages.getString("rest.modules.module.deleted"),
                      s"$moduleName-$moduleVersion"
                    )))

                    complete(HttpEntity(
                      `application/json`,
                      serializer.serialize(response)
                    ))
                  } else {
                    throw new BadRecordWithKey( MessageFormat.format(
                      messages.getString("rest.modules.module.notfound"),
                      s"$moduleName-$moduleVersion"
                    ), s"$moduleType-$moduleName-$moduleVersion")
                  }
                } else {
                  throw new BadRecordWithKey( MessageFormat.format(
                    messages.getString("rest.modules.module.cannot.delete"),
                    s"$moduleName-$moduleVersion"
                  ), s"$moduleType-$moduleName-$moduleVersion")
                }
              }
            }
          }
        } ~
        pathEndOrSingleSlash {
          get {
            val files = fileMetadataDAO.getByParameters(Map("specification.module-type" -> moduleType))
            var response: ProtocolResponse = null
            if (files.nonEmpty) {
              val entity = Map("message" -> s"Uploaded modules for type $moduleType",
                "modules" -> files.map(f => Map("module-type" -> f.specification.moduleType,
                  "module-name" -> f.specification.name,
                  "module-version" -> f.specification.version)))
              response = ProtocolResponse(200, entity)
            } else {
              response = ProtocolResponse(200, Map("message" -> MessageFormat.format(
                messages.getString("rest.modules.type.notfound"),
                moduleType)/*s"Uploaded modules for type $moduleType have not been found"*/))
            }
            complete(HttpEntity(
              `application/json`,
              serializer.serialize(response)
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
    if (moduleType.equals(windowedStreamingType)) {
      serializer.deserialize[WindowedInstanceMetadata](options)
    } else if (moduleType.equals(regularStreamingType)) {
      serializer.deserialize[RegularInstanceMetadata](options)
    } else if (moduleType.equals(outputStreamingType)) {
      serializer.deserialize[OutputInstanceMetadata](options)
    } else if (moduleType.equals(inputStreamingType)) {
      serializer.deserialize[InputInstanceMetadata](options)
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
  def validateOptions(options: InstanceMetadata, specification: ModuleSpecification, moduleType: String) = {
    val validatorClassName = configService.get(s"system.$moduleType-validator-class").value
    val validatorClass = Class.forName(validatorClassName)
    val validator = validatorClass.newInstance().asInstanceOf[StreamingModuleValidator]
    validator.validate(options, specification)
  }

  /**
   * Save instance of module to db
   *
   * @param instance - entity of instance, which saving to db
   * @param moduleType - type name of module
   * @param moduleName - name of module
   * @param moduleVersion - version of module
   * @param engineName - name of engine
   * @param engineVersion - version of engine
   * @return - name of created entity
   */
  def saveInstance(instance: Instance,
                   moduleType: String,
                   moduleName: String,
                   moduleVersion: String,
                   engineName: String,
                   engineVersion: String) = {
    instance.engine = engineName + "-" + engineVersion //todo: maybe spaces/commons or some special characters from name/version need to remove
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
  def startInstance(instance: Instance) = {
    logger.debug(s"Starting application of instance ${instance.name}")
    new Thread(new InstanceStarter(instance, 1000)).start()
  }

  /**
   * Stopping instance application on mesos
   *
   * @param instance - Instance for stopping
   * @return - Message about successful stopping
   */
  def stopInstance(instance: Instance) = {
    logger.debug(s"Stopping application of instance ${instance.name}")
    new Thread(new InstanceStopper(instance, 1000)).start()
  }

  /**
   * Destroying application on mesos
   *
   * @param instance - Instance for destroying
   * @return - Message of destroying instance
   */
  def destroyInstance(instance: Instance) = {
    logger.debug(s"Destroying application of instance ${instance.name}")
    new Thread(new InstanceDestroyer(instance, 1000)).start()
  }

}
