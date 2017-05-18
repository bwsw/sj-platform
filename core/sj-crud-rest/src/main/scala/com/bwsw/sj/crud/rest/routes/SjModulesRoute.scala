package com.bwsw.sj.crud.rest.routes

import java.io.{File, FileNotFoundException}
import java.lang.reflect.Type
import java.net.{URI, URLClassLoader}
import java.nio.file.Paths

import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.FileIO
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.rest.model.module._
import com.bwsw.sj.common.si.JsonValidator
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.StreamLiterals._
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.exceptions._
import com.bwsw.sj.crud.rest.instance.{HttpClient, InstanceDestroyer, InstanceStarter, InstanceStopper}
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.instance.InstanceValidator
import org.apache.commons.io.FileUtils
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.{Failure, Success, Try}

trait SjModulesRoute extends Directives with SjCrudValidator with JsonValidator {

  private val previousFilesNames: ListBuffer[String] = ListBuffer[String]()

  import EngineLiterals._

  val modulesRoute =
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
              val response = OkRestResponse(TypesResponseEntity(EngineLiterals.moduleTypes))

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
                      } ~
                      pathPrefix("tasks") {
                        pathEndOrSingleSlash {
                          getTasksInfo(instance)
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
        val result = Try {
          var response: RestResponse = BadRequestRestResponse(
            MessageResponseEntity(createMessage("rest.modules.modules.extension.unknown", metadata.fileName)))

          if (metadata.fileName.endsWith(".jar")) {
            val specification: Map[String, Any] = validateSpecification(file)
            response = ConflictRestResponse(MessageResponseEntity(
              createMessage("rest.modules.module.exists", metadata.fileName)))

            if (!doesModuleExist(specification)) {
              response = ConflictRestResponse(MessageResponseEntity(
                createMessage("rest.modules.module.file.exists", metadata.fileName)))

              if (!storage.exists(metadata.fileName)) {
                val uploadingFile = new File(metadata.fileName)
                FileUtils.copyFile(file, uploadingFile)
                storage.put(uploadingFile, metadata.fileName, specification, "module")
                response = OkRestResponse(MessageResponseEntity(
                  createMessage("rest.modules.module.uploaded", metadata.fileName)))
              }
            }
          }
          response
        }
        file.delete()
        result match {
          case Success(response) => complete(restResponseToHttpResponse(response))
          case Failure(e) => throw e
        }
    }
  }

  private val gettingListOfAllModules = get {
    val files = fileMetadataDAO.getByParameters(Map("filetype" -> "module"))
    val response = OkRestResponse(ModulesResponseEntity())

    if (files.nonEmpty) {
      val modulesInfo = files.map(f => ModuleInfo(f.specification.moduleType, f.specification.name, f.specification.version, f.length))
      response.entity = ModulesResponseEntity(modulesInfo)
    }

    complete(restResponseToHttpResponse(response))
  }

  private val gettingAllInstances = pathEndOrSingleSlash {
    get {
      val allInstances = instanceDAO.getAll

      val response = OkRestResponse(ShortInstancesResponseEntity())
      if (allInstances.nonEmpty) {
        response.entity = ShortInstancesResponseEntity(allInstances.map(instance =>
          ShortInstance(instance.name,
            instance.moduleType,
            instance.moduleName,
            instance.moduleVersion,
            instance.description,
            instance.status,
            instance.restAddress)
        ))
      }

      complete(restResponseToHttpResponse(response))
    }
  }

  private val creationOfInstance = (moduleType: String,
                                    moduleName: String,
                                    moduleVersion: String,
                                    specification: SpecificationApi,
                                    filename: String) => post {
    entity(as[String]) { entity =>
      var response: Option[RestResponse] = None
      val errors = new ArrayBuffer[String]
      Try(deserializeOptions(entity, moduleType)) match {
        case Success(instanceMetadata) =>
          errors ++= validateInstance(instanceMetadata, specification, moduleType)
          val instancePassedValidation = validateInstance(specification, filename, instanceMetadata)

          if (errors.isEmpty) {
            if (instancePassedValidation.result) {
              instanceMetadata.prepareInstance(
                moduleType,
                moduleName,
                moduleVersion,
                specification.engineName,
                specification.engineVersion
              )
              instanceMetadata.createStreams()
              instanceDAO.save(instanceMetadata.asModelInstance())

            response = Option(
              CreatedRestResponse(
                MessageResponseEntity(
                  createMessage(
                    "rest.modules.instances.instance.created",
                    instanceMetadata.name,
                    s"$moduleType-$moduleName-$moduleVersion"
                  ))))
          } else {
            response = Option(
              BadRequestRestResponse(
                MessageResponseEntity(
                  createMessageWithErrors(
                    "rest.modules.instances.instance.cannot.create.incorrect.parameters",
                    instancePassedValidation.errors))))
          }
        }

        case Failure(e: JsonDeserializationException) =>
          errors += JsonDeserializationErrorMessageCreator(e)
        case Failure(e) => throw e
      }

      if (errors.nonEmpty) {
        response = Option(
          BadRequestRestResponse(
            MessageResponseEntity(
              createMessageWithErrors("rest.modules.instances.instance.cannot.create", errors))))
      }

      complete(restResponseToHttpResponse(response.get))
    }
  }

  private val gettingModuleInstances = (moduleType: String,
                                        moduleName: String,
                                        moduleVersion: String) => get {
    val instances = instanceDAO.getByParameters(Map(
      "module-name" -> moduleName,
      "module-type" -> moduleType,
      "module-version" -> moduleVersion)
    )
    val response = OkRestResponse(InstancesResponseEntity())
    if (instances.nonEmpty) {
      response.entity = InstancesResponseEntity(instances.map(_.asProtocolInstance()))
    }

    complete(restResponseToHttpResponse(response))
  }

  private val gettingInstance = (instance: InstanceDomain) => get {
    val response = OkRestResponse(InstanceResponseEntity(instance.asProtocolInstance()))
    complete(restResponseToHttpResponse(response))
  }

  private val deletingInstance = (instance: InstanceDomain) => delete {
    val instanceName = instance.name
    var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
      createMessage("rest.modules.instances.instance.cannot.delete", instanceName)))

    if (instance.status.equals(stopped) || instance.status.equals(failed) || instance.status.equals(error)) {
      destroyInstance(instance)
      response = OkRestResponse(MessageResponseEntity(
        createMessage("rest.modules.instances.instance.deleting", instanceName)))
    } else if (instance.status.equals(ready)) {
      instanceDAO.delete(instanceName)
      response = OkRestResponse(MessageResponseEntity(
        createMessage("rest.modules.instances.instance.deleted", instanceName)))
    }

    complete(restResponseToHttpResponse(response))
  }

  private val launchingOfInstance = (instance: InstanceDomain) => get {
    val instanceName = instance.name
    var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
      createMessage("rest.modules.instances.instance.cannot.start", instanceName)))
    if (instance.status.equals(ready) ||
      instance.status.equals(stopped) ||
      instance.status.equals(failed)) {
      startInstance(instance)
      response = OkRestResponse(MessageResponseEntity(
        createMessage("rest.modules.instances.instance.starting", instanceName)))
    }

    complete(restResponseToHttpResponse(response))
  }

  private val stoppingOfInstance = (instance: InstanceDomain) => get {
    val instanceName = instance.name
    var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
      getMessage("rest.modules.instances.instance.cannot.stop")))

    if (instance.status.equals(started)) {
      stopInstance(instance)
      response = OkRestResponse(MessageResponseEntity(
        createMessage("rest.modules.instances.instance.stopping", instanceName)))
    }

    complete(restResponseToHttpResponse(response))
  }

  private val getTasksInfo = (instance: InstanceDomain) => get {
    var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
      getMessage("rest.modules.instances.instance.cannot.get.tasks")))

    if (instance.restAddress != "") {
      val client = new HttpClient(3000).client
      val url = new URI(instance.restAddress)
      val httpGet = new HttpGet(url.toString)
      val httpResponse = client.execute(httpGet)
      response = OkRestResponse(
        serializer.deserialize[FrameworkRestEntity](EntityUtils.toString(httpResponse.getEntity, "UTF-8")))
      client.close()
    }

    complete(restResponseToHttpResponse(response))
  }

  private val gettingSpecification = (specification: SpecificationApi) => get {
    val response = OkRestResponse(SpecificationResponseEntity(specification))
    complete(restResponseToHttpResponse(response))
  }

  private val gettingModule = (filename: String) => get {
    deletePreviousFiles()
    val jarFile = storage.get(filename, "/tmp/" + filename)
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
    var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
      createMessage("rest.modules.module.cannot.delete", s"$moduleType-$moduleName-$moduleVersion")))

    val instances = getRelatedInstances(moduleType, moduleName, moduleVersion)

    if (instances.isEmpty) {
      storage.delete(filename)
      response = OkRestResponse(MessageResponseEntity(
        createMessage("rest.modules.module.deleted", s"$moduleType-$moduleName-$moduleVersion"))
      )
    }

    complete(restResponseToHttpResponse(response))
  }

  private val gettingRelatedInstances = (moduleType: String,
                                         moduleName: String,
                                         moduleVersion: String) => get {
    val response = OkRestResponse(RelatedToModuleResponseEntity(getRelatedInstances(moduleType, moduleName, moduleVersion)))

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
    val response = OkRestResponse(ModulesResponseEntity())
    if (files.nonEmpty) {
      val modulesInfo = files.map(f => ModuleInfo(f.specification.moduleType, f.specification.name, f.specification.version, f.length))
      response.entity = ModulesResponseEntity(modulesInfo)
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

    fileSpecification.asSpecification()
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
    if (moduleType.equals(batchStreamingType)) {
      serializer.deserialize[BatchInstanceApi](options)
    } else if (moduleType.equals(regularStreamingType)) {
      serializer.deserialize[RegularInstanceApi](options)
    } else if (moduleType.equals(outputStreamingType)) {
      serializer.deserialize[OutputInstanceApi](options)
    } else if (moduleType.equals(inputStreamingType)) {
      serializer.deserialize[InputInstanceApi](options)
    } else {
      serializer.deserialize[InstanceApi](options)
    }
  }

  /**
    * Validation of options for created module instance
    *
    * @param options    - options for instance
    * @param moduleType - type name of module
    * @return - list of errors
    */
  private def validateInstance(options: InstanceApi, specification: SpecificationApi, moduleType: String) = {
    val validatorClassName = configService.get(s"system.$moduleType-validator-class") match {
      case Some(configurationSetting) => configurationSetting.value
      case None => throw ConfigSettingNotFound(
        createMessage("rest.config.setting.notfound", ConfigLiterals.systemDomain, s"$moduleType-validator-class"))
    }
    val validatorClass = Class.forName(validatorClassName)
    val validator = validatorClass.newInstance().asInstanceOf[InstanceValidator]
    validator.validate(options, specification)
  }

  private def validateInstance(specification: SpecificationApi, filename: String, instanceMetadata: InstanceApi): ValidationInfo = {
    import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

    val validatorClassName = specification.validateClass
    val file = storage.get(filename, s"tmp/$filename")
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    val clazz = loader.loadClass(validatorClassName)
    val validator = clazz.newInstance().asInstanceOf[StreamingValidator]
    val optionsValidationInfo = validator.validate(instanceMetadata)
    val instanceValidationInfo = validator.validate(serializer.serialize(instanceMetadata.options))

    ValidationInfo(
      optionsValidationInfo.result && instanceValidationInfo.result,
      optionsValidationInfo.errors ++= instanceValidationInfo.errors
    )
  }

  /**
    * Starting generators (or scaling) for streams and framework for instance on mesos
    *
    * @param instance - Starting instance
    * @return
    */
  private def startInstance(instance: InstanceDomain) = {
    logger.debug(s"Starting application of instance ${instance.name}.")
    new Thread(new InstanceStarter(instance)).start()
  }

  /**
    * Stopping instance application on mesos
    *
    * @param instance - Instance for stopping
    * @return - Message about successful stopping
    */
  private def stopInstance(instance: InstanceDomain) = {
    logger.debug(s"Stopping application of instance ${instance.name}.")
    new Thread(new InstanceStopper(instance)).start()
  }

  /**
    * Destroying application on mesos
    *
    * @param instance - Instance for destroying
    * @return - Message of destroying instance
    */
  private def destroyInstance(instance: InstanceDomain) = {
    logger.debug(s"Destroying application of instance ${instance.name}.")
    new Thread(new InstanceDestroyer(instance)).start()
  }

  /**
    * Check specification of uploading jar file
    *
    * @param jarFile - input jar file
    * @return - content of specification.json
    */
  def validateSpecification(jarFile: File): Map[String, Any] = {
    val configService = ConnectionRepository.getConfigRepository
    val classLoader = new URLClassLoader(Array(jarFile.toURI.toURL), ClassLoader.getSystemClassLoader)
    val serializedSpecification = FileMetadata.getSpecificationFromJar(jarFile)
    validateSerializedSpecification(serializedSpecification)
    validateWithSchema(serializedSpecification, "schema.json")
    val specification = serializer.deserialize[Map[String, Any]](serializedSpecification)
    val moduleType = specification("module-type").asInstanceOf[String]
    val inputs = specification("inputs").asInstanceOf[Map[String, Any]]
    val inputCardinality = inputs("cardinality").asInstanceOf[List[Int]]
    val inputTypes = inputs("types").asInstanceOf[List[String]]
    val outputs = specification("outputs").asInstanceOf[Map[String, Any]]
    val outputCardinality = outputs("cardinality").asInstanceOf[List[Int]]
    val outputTypes = outputs("types").asInstanceOf[List[String]]
    val validatorClass = specification("validator-class").asInstanceOf[String]
    moduleType match {
      case `inputStreamingType` =>
        //'inputs.cardinality' field
        if (!isZeroCardinality(inputCardinality)) {
          throw new Exception(createMessage("rest.validator.specification.both.input.cardinality", moduleType, "zero"))
        }

        //'inputs.types' field
        if (inputTypes.length != 1 || !inputTypes.contains(inputDummy)) {
          throw new Exception(createMessage("rest.validator.specification.input.type", moduleType, "input"))
        }

        //'outputs.cardinality' field
        if (!isNonZeroCardinality(outputCardinality)) {
          throw new Exception(createMessage("rest.validator.specification.cardinality.left.bound.greater.zero", moduleType, "outputs"))
        }

        //'outputs.types' field
        if (outputTypes.length != 1 || !doesSourceTypesConsistOf(outputTypes, Set(tstreamType))) {
          throw new Exception(createMessage("rest.validator.specification.sources.must.t-stream", moduleType, "outputs"))
        }

      case `outputStreamingType` =>
        //'inputs.cardinality' field
        if (!isSingleCardinality(inputCardinality)) {
          throw new Exception(createMessage("rest.validator.specification.both.input.cardinality", moduleType, "1"))
        }

        //'inputs.types' field
        if (inputTypes.length != 1 || !doesSourceTypesConsistOf(inputTypes, Set(tstreamType))) {
          throw new Exception(createMessage("rest.validator.specification.sources.must.t-stream", moduleType, "inputs"))
        }

        //'outputs.cardinality' field
        if (!isSingleCardinality(outputCardinality)) {
          throw new Exception(createMessage("rest.validator.specification.both.input.cardinality", moduleType, "1"))
        }

        //'outputs.types' field
        if (outputTypes.isEmpty || !doesSourceTypesConsistOf(outputTypes, Set(esOutputType, jdbcOutputType, restOutputType))) {
          throw new Exception(createMessage("rest.validator.specification.sources.es.jdbc.rest", moduleType, "outputs"))
        }


      case _ =>
        //'inputs.cardinality' field
        if (!isNonZeroCardinality(inputCardinality)) {
          throw new Exception(createMessage("rest.validator.specification.cardinality.left.bound.greater.zero", moduleType, "inputs"))
        }

        //'inputs.types' field
        if (inputTypes.isEmpty || !doesSourceTypesConsistOf(inputTypes, Set(tstreamType, kafkaStreamType))) {
          throw new Exception(createMessage("rest.validator.specification.sources.t-stream.kafka", moduleType, "inputs"))
        }

        //'outputs.cardinality' field
        if (!isNonZeroCardinality(outputCardinality)) {
          throw new Exception(createMessage("rest.validator.specification.cardinality.left.bound.greater.zero", moduleType, "outputs"))
        }

        //'outputs.types' field
        if (outputTypes.length != 1 || !doesSourceTypesConsistOf(outputTypes, Set(tstreamType))) {
          throw new Exception(createMessage("rest.validator.specification.sources.must.t-stream", moduleType, "outputs"))
        }

        if (moduleType == EngineLiterals.batchStreamingType) {
          //'batch-collector-class' field
          Option(specification("batch-collector-class").asInstanceOf[String]) match {
            case Some("") | None =>
              throw new Exception(createMessage("rest.validator.specification.batchcollector.should.defined", moduleType, "batch-collector-class"))
            case _ =>
          }
        }
    }

    //'validator-class' field
    val validatorClassInterfaces = getValidatorClassInterfaces(validatorClass, classLoader)
    if (!validatorClassInterfaces.exists(x => x.equals(classOf[StreamingValidator]))) {
      throw new Exception(createMessage("rest.validator.specification.class.should.implement", moduleType, "validator-class", "StreamingValidator"))
    }

    //'engine-name' and 'engine-version' fields
    val engine = specification("engine-name").asInstanceOf[String] + "-" + specification("engine-version").asInstanceOf[String]
    if (configService.get("system." + engine).isEmpty) {
      throw new Exception(createMessage("rest.validator.specification.invalid.engine.params", moduleType))
    }

    specification
  }

  private def isZeroCardinality(cardinality: List[Int]): Boolean = {
    cardinality.head == 0 && cardinality.last == 0
  }

  private def doesSourceTypesConsistOf(sourceTypes: List[String], types: Set[String]): Boolean = {
    sourceTypes.forall(_type => types.contains(_type))
  }

  private def isNonZeroCardinality(cardinality: List[Int]): Boolean = {
    cardinality.head > 0 && (cardinality.last >= cardinality.head)
  }

  private def isSingleCardinality(cardinality: List[Int]): Boolean = {
    cardinality.head == 1 && cardinality.last == 1
  }

  private def getValidatorClassInterfaces(className: String, classLoader: URLClassLoader): Array[Type] = {
    logger.debug("Try to load a validator class from jar that is indicated on specification.")
    Try(classLoader.loadClass(className).getAnnotatedInterfaces.map(x => x.getType)) match {
      case Success(x) => x
      case Failure(_: ClassNotFoundException) =>
        logger.error(s"Specification.json for module has got the invalid 'validator-class' param: " +
          s"class '$className' indicated in the specification isn't found.")
        throw new Exception(createMessage("rest.validator.specification.class.not.found", "validator-class", className))
      case Failure(e) => throw e
    }
  }

  private def validateSerializedSpecification(specificationJson: String): Unit = {
    logger.debug(s"Validate a serialized specification.")
    if (isEmptyOrNullString(specificationJson)) {
      logger.error(s"Specification.json is not found in module jar.")
      val message = createMessage("rest.modules.specification.json.not.found")
      logger.error(message)
      throw new FileNotFoundException(message)
    }
    if (!isJSONValid(specificationJson)) {
      logger.error(s"Specification.json of module is an invalid json.")
      val message = createMessage("rest.modules.specification.json.invalid")
      logger.error(message)
      throw new FileNotFoundException(message)
    }
  }
}