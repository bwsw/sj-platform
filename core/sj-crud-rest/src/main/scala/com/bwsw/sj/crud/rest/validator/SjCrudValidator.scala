package com.bwsw.sj.crud.rest.validator

import java.io._
import java.net.URLClassLoader
import java.util.jar.JarFile

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.RequestContext
import akka.stream.Materializer
import com.bwsw.common.file.utils.FileStorage
import com.bwsw.common.traits.Serializer
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.engine.{StreamingExecutor, StreamingValidator}
import com.bwsw.sj.common.utils.{EngineLiterals, MessageResourceUtils, StreamLiterals}
import com.bwsw.sj.crud.rest.utils.CompletionUtils

import scala.concurrent.{Await, ExecutionContextExecutor}

/**
  * Trait for validation of crud-rest-api
  * and contains common methods for routes
  *
  * @author Kseniya Tomskikh
  */
trait SjCrudValidator extends CompletionUtils with JsonValidator with MessageResourceUtils {
  val logger: LoggingAdapter

  implicit val materializer: Materializer
  implicit val system: ActorSystem

  implicit def executor: ExecutionContextExecutor

  val serializer: Serializer
  val fileMetadataDAO: GenericMongoService[FileMetadata]
  val storage: FileStorage
  val instanceDAO: GenericMongoService[Instance]
  val serviceDAO: GenericMongoService[Service]
  val streamDAO: GenericMongoService[SjStream]
  val providerDAO: GenericMongoService[Provider]
  val configService: GenericMongoService[ConfigurationSetting]
  val restHost: String
  val restPort: Int

  import EngineLiterals._
  import StreamLiterals._

  /**
    * Getting entity from HTTP-request
    *
    * @param ctx - request context
    * @return - entity from http-request as string
    */
  def getEntityFromContext(ctx: RequestContext): String = {
    getEntityAsString(ctx.request.entity)
  }

  private def getEntityAsString(entity: HttpEntity): String = {
    import scala.concurrent.duration._
    Await.result(entity.toStrict(1.second), 1.seconds).data.decodeString("UTF-8")
  }

  def validateContextWithSchema(ctx: RequestContext, schema: String) = {
    checkContext(ctx)
    val entity = checkEntity(ctx)
    validateWithSchema(entity, schema)
  }

  private def checkContext(ctx: RequestContext) = {
    if (ctx.request.entity.isKnownEmpty()) {
      throw new Exception(createMessage("rest.errors.empty.entity"))
    }
  }

  private def checkEntity(ctx: RequestContext) = {
    val entity = getEntityFromContext(ctx)
    if (!isJSONValid(entity)) {
      val message = createMessage("rest.errors.entity.invalid.json")
      logger.error(message)
      throw new Exception(message)
    }

    entity
  }

  /**
    * Check specification of uploading jar file
    *
    * @param jarFile - input jar file
    * @return - content of specification.json
    */
  def checkJarFile(jarFile: File) = {
    val configService = ConnectionRepository.getConfigService
    val classLoader = new URLClassLoader(Array(jarFile.toURI.toURL), ClassLoader.getSystemClassLoader)
    val specificationJson = getSpecificationFromJar(jarFile)
    validateJson(specificationJson)
    validateWithSchema(specificationJson, "schema.json")
    val specification = serializer.deserialize[Map[String, Any]](specificationJson)
    val moduleType = specification("module-type").asInstanceOf[String]
    val inputs = specification("inputs").asInstanceOf[Map[String, Any]]
    val inputCardinality = inputs("cardinality").asInstanceOf[List[Int]]
    val inputTypes = inputs("types").asInstanceOf[List[String]]
    val outputs = specification("outputs").asInstanceOf[Map[String, Any]]
    val outputCardinality = outputs("cardinality").asInstanceOf[List[Int]]
    val outputTypes = outputs("types").asInstanceOf[List[String]]
    val validatorClass = specification("validator-class").asInstanceOf[String]
    val executorClass = specification("executor-class").asInstanceOf[String]
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

      case `regularStreamingType` | `windowedStreamingType` =>
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
        if (outputTypes.isEmpty || !doesSourceTypesConsistOf(outputTypes, Set(esOutputType, jdbcOutputType))) {
          throw new Exception(createMessage("rest.validator.specification.sources.es.jdbc", moduleType, "outputs"))
        }
    }

    //'validator-class' field
    val validatorClassInterfaces = getClassInterfaces(validatorClass, classLoader)
    if (!validatorClassInterfaces.exists(x => x.equals(classOf[StreamingValidator]))) {
      throw new Exception(createMessage("rest.validator.specification.class.should.implement", moduleType, "validator-class", "StreamingValidator"))
    }

    //'executor-class' field
    val executorClassInterfaces = getExecutorClassInterfaces(executorClass, classLoader)
    if (!executorClassInterfaces.exists(x => x.equals(classOf[StreamingExecutor]))) {
      throw new Exception(createMessage("rest.validator.specification.class.should.implement", moduleType, "executor-class", "StreamingExecutor"))
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

  private def doesSourceTypesConsistOf(sourceTypes: List[String], types: Set[String]) = {
    sourceTypes.forall(_type => types.contains(_type))
  }

  private def isNonZeroCardinality(cardinality: List[Int]): Boolean = {
    cardinality.head > 0 && (cardinality.last >= cardinality.head)
  }

  private def isSingleCardinality(cardinality: List[Int]): Boolean = {
    cardinality.head == 1 && cardinality.last == 1
  }

  private def getClassInterfaces(className: String, classLoader: URLClassLoader) = {
    try {
      classLoader.loadClass(className).getAnnotatedInterfaces.map(x => x.getType)
    } catch {
      case _: ClassNotFoundException =>
        throw new Exception(createMessage("rest.validator.specification.class.not.found", "validator-class", className))
    }
  }

  private def getExecutorClassInterfaces(className: String, classLoader: URLClassLoader) = {
    try {
      classLoader.loadClass(className).getAnnotatedSuperclass.getType.asInstanceOf[Class[Object]]
        .getAnnotatedInterfaces.map(x => x.getType)
    } catch {
      case _: ClassNotFoundException =>
        throw new Exception(createMessage("rest.validator.specification.class.not.found", "executor-class", className))
    }
  }


  /**
    * Check specification of uploading custom jar file
    *
    * @param jarFile - input jar file
    * @return - content of specification.json
    */
  def checkSpecification(jarFile: File): Boolean = {
    val json = getSpecificationFromJar(jarFile)
    if (isEmptyOrNullString(json)) {
      return false
    }

    validateWithSchema(json, "customschema.json")
  }

  def getSpecification(jarFile: File) = {
    val json = getSpecificationFromJar(jarFile)

    serializer.deserialize[Map[String, Any]](json)
  }

  /**
    * Return content of specification.json file from root of jar
    *
    * @param file - Input jar file
    * @return - json-string from specification.json
    */
  private def getSpecificationFromJar(file: File): String = {
    val builder = new StringBuilder
    val jar = new JarFile(file)
    val enu = jar.entries()
    while (enu.hasMoreElements) {
      val entry = enu.nextElement
      if (entry.getName.equals("specification.json")) {
        val reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))
        try {
          var line = reader.readLine
          while (line != null) {
            builder.append(line + "\n")
            line = reader.readLine
          }
        } finally {
          reader.close()
        }
      }
    }
    builder.toString()
  }

  def validateJson(specificationJson: String) = {
    if (isEmptyOrNullString(specificationJson)) {
      val message = createMessage("rest.modules.specification.json.not.found")
      logger.error(message)
      throw new FileNotFoundException(message)
    }
    if (!isJSONValid(specificationJson)) {
      val message = createMessage("rest.modules.specification.json.invalid")
      logger.error(message)
      throw new FileNotFoundException(message)
    }
  }
}
