package com.bwsw.sj.crud.rest.validator

import java.io._
import java.util.ResourceBundle
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
import com.bwsw.sj.common.utils.{StreamConstants, EngineConstants}
import org.everit.json.schema.loader.SchemaLoader
import org.json.{JSONObject, JSONTokener}

import scala.concurrent.{Await, ExecutionContextExecutor}

/**
 * Trait for validation of crud-rest-api
 * and contains common methods for routes
 *
 *
 *
 * @author Kseniya Tomskikh
 */
trait SjCrudValidator {
  val messages = ResourceBundle.getBundle("messages")

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
  val configService: GenericMongoService[ConfigSetting]
  val restHost: String
  val restPort: Int

  import EngineConstants._
  import StreamConstants._

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

  /**
   * Check specification of uploading jar file
   *
   * @param jarFile - input jar file
   * @return - content of specification.json
   */
  def checkJarFile(jarFile: File) = {
    val configService = ConnectionRepository.getConfigService
    val json = getSpecificationFromJar(jarFile)
    if (isEmptyOrNullString(json)) {
      logger.debug(s"File specification.json not found in module jar ${jarFile.getName}.")
      throw new FileNotFoundException(s"Specification.json for ${jarFile.getName} is not found!")
    }
    schemaValidate(json, getClass.getClassLoader.getResourceAsStream("schema.json"))
    val specification = serializer.deserialize[Map[String, Any]](json)
    val moduleType = specification("module-type").asInstanceOf[String]
    val inputs = specification("inputs").asInstanceOf[Map[String, Any]]
    val inputCardinality = inputs("cardinality").asInstanceOf[List[Int]]
    val inputTypes = inputs("types").asInstanceOf[List[String]]
    val outputs = specification("outputs").asInstanceOf[Map[String, Any]]
    val outputCardinality = outputs("cardinality").asInstanceOf[List[Int]]
    val outputTypes = outputs("types").asInstanceOf[List[String]]
    moduleType match {
      case `inputStreamingType` =>
        //'inputs.cardinality' field
        if (!isZeroCardinality(inputCardinality)) {
          throw new Exception("Specification.json for input-streaming module has incorrect params: " +
            "both of cardinality of inputs has to be equal zero.")
        }

        //'inputs.types' field
        if (inputTypes.length != 1 || !inputTypes.contains(inputDummy)) {
          throw new Exception("Specification.json for input-streaming module has incorrect params: " +
            "inputs must contain only one string: 'input'.")
        }

        //'outputs.cardinality' field
        if (!isNonZeroCardinality(outputCardinality)) {
          throw new Exception("Specification.json for input-streaming module has incorrect params: " +
            "cardinality of outputs has to be an interval with the left bound that is greater than zero.")
        }

        //'outputs.types' field
        if (outputTypes.length != 1 || !doesSourceTypesConsistOf(outputTypes, Set(tStreamType))) {
          throw new Exception("Specification.json for input-streaming module has incorrect params: " +
            "outputs must have the streams of t-stream type.")
        }

      case `regularStreamingType` =>
        //'inputs.cardinality' field
        if (!isNonZeroCardinality(inputCardinality)) {
          throw new Exception("Specification.json for regular-streaming module has incorrect params: " +
            "cardinality of inputs has to be an interval with the left bound that is greater than zero.")
        }

        //'inputs.types' field
        if (inputTypes.isEmpty || !doesSourceTypesConsistOf(inputTypes, Set(tStreamType, kafkaStreamType))) {
          throw new Exception("Specification.json for regular-streaming module has incorrect params: " +
            "inputs must have the streams of t-stream and kafka type.")
        }

        //'outputs.cardinality' field
        if (!isNonZeroCardinality(outputCardinality)) {
          throw new Exception("Specification.json for regular-streaming module has incorrect params: " +
            "cardinality of outputs has to be an interval with the left bound that is greater than zero.")
        }

        //'outputs.types' field
        if (outputTypes.length != 1 || !doesSourceTypesConsistOf(outputTypes, Set(tStreamType))) {
          throw new Exception("Specification.json for regular-streaming module has incorrect params: " +
            "outputs must have the streams of t-stream type.")
        }

      case `outputStreamingType` =>
        //'inputs.cardinality' field
        if (!isSingleCardinality(inputCardinality)) {
          throw new Exception("Specification.json for output-streaming module has incorrect params: " +
            "both of cardinality of inputs has to be equal 1.")
        }

        //'inputs.types' field
        if (inputTypes.length != 1 || !doesSourceTypesConsistOf(inputTypes, Set(tStreamType))) {
          throw new Exception("Specification.json for output-streaming module has incorrect params: " +
            "inputs must have the streams of t-stream type.")
        }

        //'outputs.cardinality' field
        if (!isSingleCardinality(outputCardinality)) {
          throw new Exception("Specification.json for output-streaming module has incorrect params: " +
            "both of cardinality of outputs has to be equal 1.")
        }

        //'outputs.types' field
        if (outputTypes.isEmpty || !doesSourceTypesConsistOf(outputTypes, Set(esOutputType, jdbcOutputType))) {
          throw new Exception("Specification.json for output-streaming module has incorrect params: " +
            "outputs must have the streams of elasticsearch or jdbc type.")
        }

        //'entity-class' field
        if (specification.get("entity-class").isEmpty) {
          throw new Exception("Specification.json for output-streaming module hasn't got 'entity-class' param.")
        }
    }

    val engine = specification("engine-name").asInstanceOf[String] + "-" + specification("engine-version").asInstanceOf[String]
    if (configService.get("system." + engine).isEmpty) {
      throw new Exception("Specification.json has got the invalid 'engine-name' and 'engine-version' params.")
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

    schemaValidate(json, getClass.getClassLoader.getResourceAsStream("customschema.json"))
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

  /**
   * Check String object
   *
   * @param value - input string
   * @return - boolean result of checking
   */
  private def isEmptyOrNullString(value: String): Boolean = value == null || value.isEmpty

  /**
   * Validate json for such schema
   *
   * @param json - input json
   * @param schemaStream - schema
   * @return - true, if schema is valid
   */
  private def schemaValidate(json: String, schemaStream: InputStream): Boolean = {
    if (schemaStream != null) {
      val rawSchema = new JSONObject(new JSONTokener(schemaStream))
      val schema = SchemaLoader.load(rawSchema)
      schema.validate(new JSONObject(json))
    } else {
      throw new Exception("Json schema for specification is not found")
    }
    true
  }
}
