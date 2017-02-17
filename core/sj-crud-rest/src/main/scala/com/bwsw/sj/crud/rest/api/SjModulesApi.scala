package com.bwsw.sj.crud.rest.api

import java.io.File
import java.nio.file.Paths

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.server.{Directives, RequestContext}
import akka.stream.scaladsl.FileIO
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.engine.StreamingValidator
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.module._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.crud.rest.RestLiterals
import com.bwsw.sj.crud.rest.exceptions._
import com.bwsw.sj.crud.rest.instance.{InstanceDestroyer, InstanceStarter, InstanceStopper}
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.instance.InstanceValidator
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

trait SjModulesApi extends Directives with SjCrudValidator {

  private val previousFilesNames: ListBuffer[String] = ListBuffer[String]()

  import EngineLiterals._

  val modulesApi =
    pathPrefix("modules") {
      pathEndOrSingleSlash {
        creationOfModule ~
          gettingListOfAllModules
      } ~
        pathPrefix("instances") {
          gettingAllInstances
        } ~
        pathPrefix("_types") {
          pathEndOrSingleSlash {
            get {
              val response = OkRestResponse(Map("types" -> EngineLiterals.moduleTypes))

              complete(restResponseToHttpResponse(response))
            }
          }
        } ~
        pathPrefix(Segment) { (moduleType: String) =>
          checkModuleType(moduleType)
          pathPrefix(Segment) { (moduleName: String) =>
            pathPrefix(Segment) { (moduleVersion: String) =>
              checkModuleOnExistence(moduleType, moduleName, moduleVersion)
              val specification = getSpecification(moduleType, moduleName, moduleVersion)
              val filename = getFileName(moduleType, moduleName, moduleVersion)
              pathPrefix("instance") {
                pathEndOrSingleSlash {
                  creationOfInstance(moduleType, moduleName, moduleVersion, specification, filename) ~
                    gettingModuleInstances(moduleType, moduleName, moduleVersion)
                } ~
                  pathPrefix(Segment) { (instanceName: String) =>
                    checkInstanceOnExistence(instanceName)
                    val instance = instanceDAO.get(instanceName).get
                    pathEndOrSingleSlash {
                      gettingInstance(instance) ~
                        deletingInstance(instance)
                    } ~
                      path("start") {
                        pathEndOrSingleSlash {
                          launchingOfInstance(instance)
                        }
                      } ~
                      path("stop") {
                        pathEndOrSingleSlash {
                          stoppingOfInstance(instance)
                        }
                      }
                  }
              } ~
                pathPrefix("specification") {
                  pathEndOrSingleSlash {
                    gettingSpecification(specification)
                  }
                } ~
                pathEndOrSingleSlash {
                  gettingModule(filename) ~
                    deletingModule(moduleType, moduleName, moduleVersion, filename)
                } ~
                pathPrefix("related") {
                  pathEndOrSingleSlash {
                    gettingRelatedInstances(moduleType, moduleName, moduleVersion)
                  }
                }
            }
          } ~
            pathEndOrSingleSlash {
              gettingModulesByType(moduleType)
            }
        }
    }

  private val creationOfModule = post {
    uploadedFile("jar") {
      case (metadata: FileInfo, file: File) =>
        try {
          var response: RestResponse = BadRequestRestResponse(Map("message" ->
            createMessage("rest.modules.modules.extension.unknown", metadata.fileName)))

          if (metadata.fileName.endsWith(".jar")) {
            val specification = validateSpecification(file)
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
          complete(restResponseToHttpResponse(response))
        } finally {
          file.delete()
        }
    }
  }

  private val gettingListOfAllModules = get {
    val files = fileMetadataDAO.getByParameters(Map("filetype" -> "module"))
    val response = OkRestResponse(Map("modules" -> mutable.Buffer()))

    if (files.nonEmpty) {
      response.entity = Map("modules" -> files.map(f => Map("moduleType" -> f.specification.moduleType,
        "moduleName" -> f.specification.name,
        "moduleVersion" -> f.specification.version)))
    }

    complete(restResponseToHttpResponse(response))
  }

  private val gettingAllInstances = pathEndOrSingleSlash {
    get {
      val allInstances = instanceDAO.getAll

      val response = OkRestResponse(Map("instances" -> mutable.Buffer()))
      if (allInstances.nonEmpty) {
        response.entity = Map("instances" -> allInstances.map(instance => ShortInstanceMetadata(instance.name,
          instance.moduleType,
          instance.moduleName,
          instance.moduleVersion,
          instance.description,
          instance.status,
          instance.restAddress)))
      }

      complete(restResponseToHttpResponse(response))
    }
  }

  private val creationOfInstance = (moduleType: String,
                                    moduleName: String,
                                    moduleVersion: String,
                                    specification: SpecificationData,
                                    filename: String) => post { (ctx: RequestContext) =>
    validateContextWithSchema(ctx, "instanceSchema.json")
    val instanceMetadata = deserializeOptions(getEntityFromContext(ctx), moduleType)
    val errors = validateInstance(instanceMetadata, specification, moduleType)
    val instancePassedValidation = validateInstance(specification, filename, instanceMetadata)
    var response: RestResponse = BadRequestRestResponse(Map("message" ->
      createMessage("rest.modules.instances.instance.cannot.create", errors.mkString(";"))))

    if (errors.isEmpty) {
      if (instancePassedValidation) {
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
          getMessage("rest.modules.instances.instance.cannot.create.incorrect.parameters")))
      }
    }

    ctx.complete(restResponseToHttpResponse(response))
  }

  private val gettingModuleInstances = (moduleType: String,
                                        moduleName: String,
                                        moduleVersion: String) => get {
    val instances = instanceDAO.getByParameters(Map(
      "module-name" -> moduleName,
      "module-type" -> moduleType,
      "module-version" -> moduleVersion)
    )
    val response = OkRestResponse(Map("instances" -> mutable.Buffer()))
    if (instances.nonEmpty) {
      response.entity = Map("instances" -> instances.map(_.asProtocolInstance()))
    }

    complete(restResponseToHttpResponse(response))
  }

  private val gettingInstance = (instance: Instance) => get {
    val t = instance.asProtocolInstance()
    val response = OkRestResponse(Map("instance" -> t))
    complete(restResponseToHttpResponse(response))
  }

  private val deletingInstance = (instance: Instance) => delete {
    val instanceName = instance.name
    var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
      createMessage("rest.modules.instances.instance.cannot.delete", instanceName)))

    if (instance.status.equals(stopped) || instance.status.equals(failed) || instance.status.equals(error)) {
      destroyInstance(instance)
      response = OkRestResponse(Map("message" ->
        createMessage("rest.modules.instances.instance.deleting", instanceName)))
    } else if (instance.status.equals(ready)) {
      instanceDAO.delete(instanceName)
      response = OkRestResponse(Map("message" ->
        createMessage("rest.modules.instances.instance.deleted", instanceName)))
    }

    complete(restResponseToHttpResponse(response))
  }

  private val launchingOfInstance = (instance: Instance) => get {
    val instanceName = instance.name
    var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
      createMessage("rest.modules.instances.instance.cannot.start", instanceName)))
    if (instance.status.equals(ready) ||
      instance.status.equals(stopped) ||
      instance.status.equals(failed)) {
      startInstance(instance)
      response = OkRestResponse(Map("message" ->
        createMessage("rest.modules.instances.instance.starting", instanceName)))
    }

    complete(restResponseToHttpResponse(response))
  }

  private val stoppingOfInstance = (instance: Instance) => get {
    val instanceName = instance.name
    var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
      createMessage("rest.modules.instances.instance.cannot.stop", instanceName)))

    if (instance.status.equals(started)) {
      stopInstance(instance)
      response = OkRestResponse(Map("message" ->
        createMessage("rest.modules.instances.instance.stopping", instanceName)))
    }

    complete(restResponseToHttpResponse(response))
  }

  private val gettingSpecification = (specification: SpecificationData) => get {
    val response = OkRestResponse(Map("specification" -> specification))
    complete(restResponseToHttpResponse(response))
  }

  private val gettingModule = (filename: String) => get {
    deletePreviousFiles()
    val jarFile = storage.get(filename, RestLiterals.tmpDirectory + filename)
    previousFilesNames.append(jarFile.getAbsolutePath)
    val source = FileIO.fromPath(Paths.get(jarFile.getAbsolutePath))
    complete(HttpResponse(
      headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))),
      entity = HttpEntity.Chunked.fromData(MediaTypes.`application/java-archive`, source)
    ))
  }

  private def deletePreviousFiles() = {
    previousFilesNames.foreach(filename => {
      val file = new File(filename)
      if (file.exists()) file.delete()
    })
  }

  private val deletingModule = (moduleType: String,
                                moduleName: String,
                                moduleVersion: String,
                                filename: String) => delete {
    var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
      createMessage("rest.modules.module.cannot.delete", s"$moduleType-$moduleName-$moduleVersion")))

    val instances = getRelatedInstances(moduleType, moduleName, moduleVersion)

    if (instances.isEmpty) {
      storage.delete(filename)
      response = OkRestResponse(Map("message" ->
        createMessage("rest.modules.module.deleted", s"$moduleType-$moduleName-$moduleVersion"))
      )
    }

    complete(restResponseToHttpResponse(response))
  }

  private val gettingRelatedInstances = (moduleType: String,
                                         moduleName: String,
                                         moduleVersion: String) => get {
    val response = OkRestResponse(Map("instances" -> getRelatedInstances(moduleType, moduleName, moduleVersion)))

    complete(restResponseToHttpResponse(response))
  }

  private def getRelatedInstances(moduleType: String, moduleName: String, moduleVersion: String) = {
    instanceDAO.getByParameters(Map(
      "module-name" -> moduleName,
      "module-type" -> moduleType,
      "module-version" -> moduleVersion)
    ).map(_.name)
  }

  private val gettingModulesByType = (moduleType: String) => get {
    val files = fileMetadataDAO.getByParameters(Map("filetype" -> "module", "specification.module-type" -> moduleType))
    val response = OkRestResponse(Map("modules" -> mutable.Buffer()))
    if (files.nonEmpty) {
      response.entity = Map("message" -> s"Uploaded modules for type $moduleType",
        "modules" -> files.map(f => Map("module-type" -> f.specification.moduleType,
          "module-name" -> f.specification.name,
          "module-version" -> f.specification.version)))
    }

    complete(restResponseToHttpResponse(response))
  }

  private def doesModuleExist(specification: Map[String, Any]) = {
    getFilesMetadata(specification("module-type").asInstanceOf[String],
      specification("name").asInstanceOf[String],
      specification("version").asInstanceOf[String]
    ).nonEmpty
  }

  private def checkModuleType(moduleType: String) = {
    if (!doesModuleTypeExist(moduleType)) {
      throw UnknownModuleType(createMessage("rest.modules.type.unknown", moduleType), moduleType)
    }
  }

  private def doesModuleTypeExist(typeName: String) = {
    moduleTypes.contains(typeName)
  }

  private def checkModuleOnExistence(moduleType: String, moduleName: String, moduleVersion: String) = {
    val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)
    if (filesMetadata.isEmpty) {
      throw ModuleNotFound(
        createMessage("rest.modules.module.notfound", s"$moduleType-$moduleName-$moduleVersion"),
        s"$moduleType - $moduleName - $moduleVersion")
    }

    val filename = filesMetadata.head.filename
    if (!storage.exists(filename)) {
      throw ModuleJarNotFound(
        createMessage("rest.modules.module.jar.notfound", s"$moduleType-$moduleName-$moduleVersion"),
        filename
      )
    }
  }

  private def getSpecification(moduleType: String, moduleName: String, moduleVersion: String) = {
    val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)
    val fileMetadata = filesMetadata.head
    val fileSpecification = fileMetadata.specification

    fileSpecification.asSpecificationData()
  }


  private def getFileName(moduleType: String, moduleName: String, moduleVersion: String) = {
    val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)

    filesMetadata.head.filename
  }

  private def getFilesMetadata(moduleType: String, moduleName: String, moduleVersion: String) = {
    fileMetadataDAO.getByParameters(Map("filetype" -> "module",
      "specification.name" -> moduleName,
      "specification.module-type" -> moduleType,
      "specification.version" -> moduleVersion)
    )
  }

  private def checkInstanceOnExistence(instanceName: String) = {
    val instance = instanceDAO.get(instanceName)
    if (instance.isEmpty) {
      throw InstanceNotFound(createMessage("rest.modules.module.instances.instance.notfound", instanceName), instanceName)
    }
  }

  /**
    * Deserialization json string to object
    *
    * @param options    - json-string
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
    * @param options    - options for instance
    * @param moduleType - type name of module
    * @return - list of errors
    */
  private def validateInstance(options: InstanceMetadata, specification: SpecificationData, moduleType: String) = {
    val validatorClassName = configService.get(s"system.$moduleType-validator-class") match {
      case Some(configurationSetting) => configurationSetting.value
      case None => throw new ConfigSettingNotFound(
        createMessage("rest.config.setting.notfound", ConfigLiterals.systemDomain, s"$moduleType-validator-class"))
    }
    val validatorClass = Class.forName(validatorClassName)
    val validator = validatorClass.newInstance().asInstanceOf[InstanceValidator]
    validator.validate(options, specification)
  }

  private def validateInstance(specification: SpecificationData, filename: String, instanceMetadata: InstanceMetadata): Boolean = {
    val validatorClassName = specification.validateClass
    val file = storage.get(filename, s"tmp/$filename")
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(validatorClassName)
    val validator = clazz.newInstance().asInstanceOf[StreamingValidator]
    validator.validate(instanceMetadata) && validator.validate(instanceMetadata.options)
  }

  /**
    * Starting generators (or scaling) for streams and framework for instance on mesos
    *
    * @param instance - Starting instance
    * @return
    */
  private def startInstance(instance: Instance) = {
    logger.debug(s"Starting application of instance ${instance.name}.")
    new Thread(new InstanceStarter(instance)).start()
  }

  /**
    * Stopping instance application on mesos
    *
    * @param instance - Instance for stopping
    * @return - Message about successful stopping
    */
  private def stopInstance(instance: Instance) = {
    logger.debug(s"Stopping application of instance ${instance.name}.")
    new Thread(new InstanceStopper(instance)).start()
  }

  /**
    * Destroying application on mesos
    *
    * @param instance - Instance for destroying
    * @return - Message of destroying instance
    */
  private def destroyInstance(instance: Instance) = {
    logger.debug(s"Destroying application of instance ${instance.name}.")
    new Thread(new InstanceDestroyer(instance)).start()
  }
}