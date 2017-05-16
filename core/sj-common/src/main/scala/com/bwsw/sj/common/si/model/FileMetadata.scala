package com.bwsw.sj.common.si.model

import java.io.{BufferedReader, File, FileNotFoundException, InputStreamReader}
import java.net.URLClassLoader
import java.util.jar.JarFile

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.StreamingValidator
import com.bwsw.sj.common.utils.EngineLiterals.{inputStreamingType, outputStreamingType}
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.StreamLiterals._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.crud.rest.validator.JsonValidator
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class FileMetadata(val filename: String,
                   val file: Option[File] = None,
                   val name: Option[String] = None,
                   val version: Option[String] = None,
                   val length: Option[Long] = None)
  extends JsonValidator {
  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private val fileStorage = ConnectionRepository.getFileStorage
  private val fileMetadataRepository = ConnectionRepository.getFileMetadataRepository

  def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]

    if (!fileStorage.exists(filename)) {
      if (checkCustomFileSpecification(file.get)) {
        val specification = FileMetadata.getSpecification(file.get)
        if (doesCustomJarExist(specification)) errors += createMessage("rest.custom.jars.exists", filename)
      } else errors += getMessage("rest.errors.invalid.specification")
    } else errors += createMessage("rest.custom.jars.file.exists", filename)

    errors
  }

  /**
    * Check specification of uploading custom jar file
    *
    * @param jarFile - input jar file
    * @return - content of specification.json
    */
  private def checkCustomFileSpecification(jarFile: File): Boolean = {
    val json = FileMetadata.getSpecificationFromJar(jarFile)
    if (isEmptyOrNullString(json)) {
      return false
    }

    validateWithSchema(json, "customschema.json")
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
    FileMetadata.maybeSpecification = Some(FileMetadata.getSpecificationFromJar(jarFile))
    validateSerializedSpecification(FileMetadata.maybeSpecification.get)
    validateWithSchema(FileMetadata.maybeSpecification.get, "schema.json")
    val specification = FileMetadata.serializer.deserialize[Map[String, Any]](FileMetadata.maybeSpecification.get)
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

  private def doesSourceTypesConsistOf(sourceTypes: List[String], types: Set[String]) = {
    sourceTypes.forall(_type => types.contains(_type))
  }

  private def isNonZeroCardinality(cardinality: List[Int]): Boolean = {
    cardinality.head > 0 && (cardinality.last >= cardinality.head)
  }

  private def isSingleCardinality(cardinality: List[Int]): Boolean = {
    cardinality.head == 1 && cardinality.last == 1
  }

  private def getValidatorClassInterfaces(className: String, classLoader: URLClassLoader) = {
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

  private def validateSerializedSpecification(specificationJson: String) = {
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

  private def doesCustomJarExist(specification: Map[String, Any]) = {
    fileMetadataRepository.getByParameters(
      Map("filetype" -> "custom",
        "specification.name" -> specification("name").asInstanceOf[String],
        "specification.version" -> specification("version").asInstanceOf[String]
      )).nonEmpty
  }

}

object FileMetadata {
  private val serializer = new JsonSerializer()
  private var maybeSpecification: Option[String] = None

  def from(fileMetadataDomain: FileMetadataDomain): FileMetadata = {
    new FileMetadata(
      fileMetadataDomain.filename,
      None,
      Some(fileMetadataDomain.specification.engineName),
      Some(fileMetadataDomain.specification.engineVersion),
      Some(fileMetadataDomain.length)
    )
  }

  def getSpecification(jarFile: File): Map[String, Any] = {
    val serializedSpecification = maybeSpecification match {
      case Some(_serializedSpecification) => _serializedSpecification
      case None => getSpecificationFromJar(jarFile)
    }

    serializer.deserialize[Map[String, Any]](serializedSpecification)
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
        val result = Try {
          var line = reader.readLine
          while (Option(line).isDefined) {
            builder.append(line + "\n")
            line = reader.readLine
          }
        }
        reader.close()
        result match {
          case Success(_) =>
          case Failure(e) => throw e
        }
      }
    }

    builder.toString()
  }
}
