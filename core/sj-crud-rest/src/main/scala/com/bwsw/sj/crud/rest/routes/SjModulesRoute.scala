package com.bwsw.sj.crud.rest.routes

import java.io.{File, FileNotFoundException}
import java.lang.reflect.Type
import java.net.URLClassLoader

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.StreamingValidator
import com.bwsw.sj.common.si.JsonValidator
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.StreamLiterals._
import com.bwsw.sj.crud.rest.controller.{InstanceController, ModuleController}
import com.bwsw.sj.crud.rest.model.module.ModuleMetadataApi
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

import scala.util.{Failure, Success, Try}

trait SjModulesRoute extends Directives with SjCrudValidator with JsonValidator {

  private val moduleController = new ModuleController
  private val instanceController = new InstanceController

  import EngineLiterals._

  val modulesRoute =
    pathPrefix("modules") {
      pathEndOrSingleSlash {
        post {
          uploadedFile("jar") {
            case (metadata: FileInfo, file: File) =>
              val moduleMetadataApi = new ModuleMetadataApi(metadata.fileName, file)
              complete(restResponseToHttpResponse(moduleController.create(moduleMetadataApi)))
          }
        } ~
          get {
            complete(restResponseToHttpResponse(moduleController.getAll))
          }
      } ~
        pathPrefix("instances") {
          get {
            complete(restResponseToHttpResponse(instanceController.getAll))
          }
        } ~
        pathPrefix("_types") {
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(moduleController.getAllTypes))
            }
          }
        } ~
        pathPrefix(Segment) { (moduleType: String) =>
          pathPrefix(Segment) { (moduleName: String) =>
            pathPrefix(Segment) { (moduleVersion: String) =>
              pathPrefix("instance") {
                pathEndOrSingleSlash {
                  post {
                    entity(as[String]) { entity =>
                      complete(
                        restResponseToHttpResponse(
                          instanceController.create(entity, moduleType, moduleName, moduleVersion)))
                    }
                  } ~
                    get {
                      complete(
                        restResponseToHttpResponse(
                          instanceController.getByModule(moduleType, moduleName, moduleVersion)))
                    }
                } ~
                  pathPrefix(Segment) { (instanceName: String) =>
                    pathEndOrSingleSlash {
                      get {
                        complete(restResponseToHttpResponse(instanceController.get(instanceName)))
                      } ~
                        complete(restResponseToHttpResponse(instanceController.delete(instanceName)))
                    } ~
                      path("start") {
                        pathEndOrSingleSlash {
                          complete(restResponseToHttpResponse(instanceController.start(instanceName)))
                        }
                      } ~
                      path("stop") {
                        pathEndOrSingleSlash {
                          complete(restResponseToHttpResponse(instanceController.stop(instanceName)))
                        }
                      } ~
                      pathPrefix("tasks") {
                        pathEndOrSingleSlash {
                          complete(restResponseToHttpResponse(instanceController.tasks(instanceName)))
                        }
                      }
                  }
              } ~
                pathPrefix("specification") {
                  pathEndOrSingleSlash {
                    get {
                      complete(
                        restResponseToHttpResponse(
                          moduleController.getSpecification(moduleType, moduleName, moduleVersion)))
                    }
                  }
                } ~
                pathEndOrSingleSlash {
                  get {
                    complete(
                      restResponseToHttpResponse(
                        moduleController.get(moduleType, moduleName, moduleVersion)))
                  } ~
                    delete {
                      complete(
                        restResponseToHttpResponse(
                          moduleController.delete(moduleType, moduleName, moduleVersion)))
                    }
                } ~
                pathPrefix("related") {
                  pathEndOrSingleSlash {
                    get {
                      complete(
                        restResponseToHttpResponse(
                          moduleController.getRelated(moduleType, moduleName, moduleVersion)))
                    }
                  }
                }
            }
          } ~
            pathEndOrSingleSlash {
              get {
                complete(restResponseToHttpResponse(moduleController.getByType(moduleType)))
              }
            }
        }
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
