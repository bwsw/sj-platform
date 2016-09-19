package com.bwsw.sj.crud.rest.api

import java.io.File

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.server.{Directives, RequestContext}
import akka.stream.scaladsl._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.engine.StreamingValidator
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.module._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.crud.rest.exceptions._
import com.bwsw.sj.crud.rest.instance.{InstanceDestroyer, InstanceStarter, InstanceStopper}
import com.bwsw.sj.crud.rest.utils.CompletionUtils
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.module.StreamingModuleValidator
import org.apache.commons.io.FileUtils

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

trait SjModulesApi extends Directives with SjCrudValidator with CompletionUtils {

  import EngineLiterals._

  val modulesApi = {
    pathPrefix("modules") {
      pathEndOrSingleSlash {
        post {
          uploadedFile("jar") {
            case (metadata: FileInfo, file: File) =>
              var response: RestResponse = BadRequestRestResponse(Map("message" ->
                createMessage("rest.modules.modules.extension.unknown", metadata.fileName)))

              if (metadata.fileName.endsWith(".jar")) {
                val specification = checkJarFile(file)
                response = ConflictRestResponse(Map("message" ->
                  createMessage("rest.modules.module.exists", metadata.fileName)))

                if (!doesModuleExist(specification)) {
                  response = ConflictRestResponse(Map("message" ->
                    createMessage("rest.modules.module.file.exists", metadata.fileName)))

                  if (!storage.exists(metadata.fileName)) {
                    val uploadingFile = new File(metadata.fileName)
                    FileUtils.copyFile(file, uploadingFile)
                    storage.put(uploadingFile, metadata.fileName, specification, "module")
                    response = OkRestResponse(Map("message" ->
                      createMessage("rest.modules.module.uploaded", metadata.fileName)))
                  }
                }
              }
              file.delete()

              complete(restResponseToHttpResponse(response))
          }
        } ~
          get {
            val files = fileMetadataDAO.getByParameters(Map("filetype" -> "module"))
            var response: RestResponse = NotFoundRestResponse(Map("message" -> getMessage("rest.modules.notfound")))

            if (files.nonEmpty) {
              val entity = Map("modules" -> files.map(f => Map("module-type" -> f.specification.moduleType,
                "module-name" -> f.specification.name,
                "module-version" -> f.specification.version)))
              response = OkRestResponse(entity)
            }

            complete(restResponseToHttpResponse(response))
          }
      } ~
        pathPrefix("instances") {
          pathEndOrSingleSlash {
            get {
              val allInstances = instanceDAO.getAll

              var response: RestResponse = NotFoundRestResponse(Map("message" -> getMessage("rest.modules.instances.notfound")))
              if (allInstances.nonEmpty) {
                val entity = Map("instances" -> allInstances.map(x => ShortInstanceMetadata(x.name,
                  x.moduleType,
                  x.moduleName,
                  x.moduleVersion,
                  x.description,
                  x.status)))

                response = OkRestResponse(entity)
              }

              complete(restResponseToHttpResponse(response))
            }
          }
        } ~
        pathPrefix(Segment) { (moduleType: String) =>
          if (!checkModuleType(moduleType)) {
            throw UnknownModuleType(createMessage("rest.modules.type.unknown", moduleType), moduleType)
          }
          pathPrefix(Segment) { (moduleName: String) =>
            pathPrefix(Segment) { (moduleVersion: String) =>
              val fileMetadatas = fileMetadataDAO.getByParameters(Map("specification.name" -> moduleName,
                "specification.module-type" -> moduleType,
                "specification.version" -> moduleVersion)
              )
              if (fileMetadatas.isEmpty) {
                throw ModuleNotFound(
                  createMessage("rest.modules.module.notfound", s"$moduleType-$moduleName-$moduleVersion"),
                  s"$moduleType - $moduleName - $moduleVersion")
              }
              val fileMetadata = fileMetadatas.head
              val fileSpecification = fileMetadata.specification
              val specification = fileSpecification.asSpecificationData()
              val filename = fileMetadata.filename

              if (!storage.exists(filename)) {
                throw ModuleJarNotFound(
                  createMessage("rest.modules.module.jar.notfound", s"$moduleType-$moduleName-$moduleVersion"),
                  filename
                )
              }

              pathPrefix("instance") {
                pathEndOrSingleSlash {
                  post { (ctx: RequestContext) =>
                    val instanceMetadata = deserializeOptions(getEntityFromContext(ctx), moduleType)
                    val errors = validateInstance(instanceMetadata, specification, moduleType)
                    val optionsPassValidation = validateInstanceOptions(specification, filename, instanceMetadata.options)
                    var response: RestResponse = BadRequestRestResponse(Map("message" ->
                      createMessage("rest.modules.instances.instance.cannot.create", errors.mkString(";"))))

                    if (errors.isEmpty) {
                      if (optionsPassValidation) {
                        instanceMetadata.prepareInstance(
                          moduleType,
                          moduleName,
                          moduleVersion,
                          specification.engineName,
                          specification.engineVersion
                        )
                        instanceMetadata.createStreams()
                        instanceDAO.save(instanceMetadata.asModelInstance())

                        response = CreatedRestResponse(Map("message" ->
                          createMessage("rest.modules.instances.instance.created", instanceMetadata.name, s"$moduleType-$moduleName-$moduleVersion")))
                      } else {
                        response = BadRequestRestResponse(Map("message" ->
                          getMessage("rest.modules.instances.instance.cannot.create.incorrect.options")))
                      }
                    }

                    ctx.complete(restResponseToHttpResponse(response))
                  } ~
                    get {
                      val instances = instanceDAO.getByParameters(Map(
                        "module-name" -> moduleName,
                        "module-type" -> moduleType,
                        "module-version" -> moduleVersion)
                      )
                      var response: RestResponse = NotFoundRestResponse(Map("message" ->
                        createMessage("rest.modules.module.instances.notfound", s"$moduleType-$moduleName-$moduleVersion"))
                      )
                      if (instances.nonEmpty) {
                        response = OkRestResponse(Map("instances" -> instances.map(_.asProtocolInstance())))
                      }

                      complete(restResponseToHttpResponse(response))
                    }
                } ~
                  pathPrefix(Segment) { (instanceName: String) =>
                    val instance = instanceDAO.get(instanceName)
                    if (instance.isEmpty) {
                      throw InstanceNotFound(createMessage("rest.modules.module.instances.instance.notfound", instanceName), instanceName)
                    }
                    pathEndOrSingleSlash {
                      get {
                        val response = OkRestResponse(Map("instance" -> instance.get.asProtocolInstance()))
                        complete(restResponseToHttpResponse(response))
                      } ~
                        delete {
                          var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
                            createMessage("rest.modules.instances.instance.cannot.delete", instanceName)))

                          if (instance.get.status.equals(stopped) || instance.get.status.equals(failed)) {
                            instance.get.status = deleting
                            instanceDAO.save(instance.get)
                            destroyInstance(instance.get)
                            response = OkRestResponse(Map("message" ->
                              createMessage("rest.modules.instances.instance.deleting", instanceName)))
                          } else if (instance.get.status.equals(ready)) {
                            instanceDAO.delete(instanceName)
                            response = OkRestResponse(Map("message" ->
                              createMessage("rest.modules.instances.instance.deleted", instanceName)))
                          }

                          complete(restResponseToHttpResponse(response))
                        }
                    } ~
                      path("start") {
                        pathEndOrSingleSlash {
                          get {
                            var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
                              createMessage("rest.modules.instances.instance.cannot.start", instanceName)))
                            if (instance.get.status.equals(ready) ||
                              instance.get.status.equals(stopped) ||
                              instance.get.status.equals(failed)) {
                              instance.get.status = starting
                              instanceDAO.save(instance.get)
                              startInstance(instance.get)
                              response = OkRestResponse(Map("message" ->
                                createMessage("rest.modules.instances.instance.starting", instanceName)))
                            }

                            complete(restResponseToHttpResponse(response))
                          }
                        }
                      } ~
                      path("stop") {
                        pathEndOrSingleSlash {
                          get {
                            var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
                              createMessage("rest.modules.instances.instance.cannot.stop", instanceName)))

                            if (instance.get.status.equals(started)) {
                              instance.get.status = stopping
                              instanceDAO.save(instance.get)
                              stopInstance(instance.get)
                              response = OkRestResponse(Map("message" ->
                                createMessage("rest.modules.instances.instance.stopping", instanceName)))
                            }

                            complete(restResponseToHttpResponse(response))
                          }
                        }
                      }
                  }
              } ~
                pathSuffix("specification") {
                  pathEndOrSingleSlash {
                    get {
                      val response = OkRestResponse(Map("specification" -> specification))
                      complete(restResponseToHttpResponse(response))
                    }
                  }
                } ~
                pathEndOrSingleSlash {
                  get {
                    val jarFile = storage.get(filename, s"tmp/$filename")
                    complete(HttpResponse(
                      headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))),
                      entity = HttpEntity.Chunked.fromData(`application/java-archive`, Source.file(jarFile))
                    ))
                  } ~
                    delete {
                      var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
                        createMessage("rest.modules.module.cannot.delete", s"$moduleType-$moduleName-$moduleVersion")))

                      val instances = instanceDAO.getByParameters(Map(
                        "module-name" -> moduleName,
                        "module-type" -> moduleType,
                        "module-version" -> moduleVersion)
                      )
                      if (instances.isEmpty) {
                        storage.delete(filename)
                        response = OkRestResponse(Map("message" ->
                          createMessage("rest.modules.module.deleted", s"$moduleType-$moduleName-$moduleVersion"))
                        )
                      }

                      complete(restResponseToHttpResponse(response))
                    }
                }
            }
          } ~
            pathEndOrSingleSlash {
              get {
                val files = fileMetadataDAO.getByParameters(Map("specification.module-type" -> moduleType))
                var response: RestResponse = NotFoundRestResponse(Map("message" ->
                  createMessage("rest.modules.type.notfound", moduleType))
                )
                if (files.nonEmpty) {
                  val entity = Map("message" -> s"Uploaded modules for type $moduleType",
                    "modules" -> files.map(f => Map("module-type" -> f.specification.moduleType,
                      "module-name" -> f.specification.name,
                      "module-version" -> f.specification.version)))
                  response = OkRestResponse(entity)
                }

                complete(restResponseToHttpResponse(response))
              }
            }
        }
    }
  }

  private def doesModuleExist(specification: Map[String, Any]) = {
    fileMetadataDAO.getByParameters(
      Map("specification.name" ->
        specification("name").asInstanceOf[String],
        "specification.module-type" -> specification("module-type").asInstanceOf[String],
        "specification.version" -> specification("version").asInstanceOf[String]
      )).nonEmpty
  }

  private def checkModuleType(typeName: String) = {
    moduleTypes.contains(typeName)
  }

  /**
   * Deserialization json string to object
   *
   * @param options - json-string
   * @param moduleType - type name of module
   * @return - json as object InstanceMetadata
   */
  private def deserializeOptions(options: String, moduleType: String) = {
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
  private def validateInstance(options: InstanceMetadata, specification: SpecificationData, moduleType: String) = {
    val validatorClassName = configService.get(s"system.$moduleType-validator-class").get.value
    val validatorClass = Class.forName(validatorClassName)
    val validator = validatorClass.newInstance().asInstanceOf[StreamingModuleValidator]
    validator.validate(options, specification)
  }

  private def validateInstanceOptions(specification: SpecificationData, filename: String, options: Map[String, Any]): Boolean = {
    val validatorClassName = specification.validateClass
    val file = storage.get(filename, s"tmp/$filename")
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(validatorClassName)
    val validator = clazz.newInstance().asInstanceOf[StreamingValidator]
    validator.validate(options)
  }

  /**
   * Starting generators (or scaling) for streams and framework for instance on mesos
   *
   * @param instance - Starting instance
   * @return
   */
  private def startInstance(instance: Instance) = {
    logger.debug(s"Starting application of instance ${instance.name}")
    new Thread(new InstanceStarter(instance, 1000)).start()
  }

  /**
   * Stopping instance application on mesos
   *
   * @param instance - Instance for stopping
   * @return - Message about successful stopping
   */
  private def stopInstance(instance: Instance) = {
    logger.debug(s"Stopping application of instance ${instance.name}")
    new Thread(new InstanceStopper(instance, 1000)).start()
  }

  /**
   * Destroying application on mesos
   *
   * @param instance - Instance for destroying
   * @return - Message of destroying instance
   */
  private def destroyInstance(instance: Instance) = {
    logger.debug(s"Destroying application of instance ${instance.name}")
    new Thread(new InstanceDestroyer(instance, 1000)).start()
  }
}