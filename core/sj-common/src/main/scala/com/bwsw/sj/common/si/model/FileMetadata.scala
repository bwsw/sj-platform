package com.bwsw.sj.common.si.model

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.jar.JarFile

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.dal.model.module.{FileMetadataDomain, SpecificationDomain}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.JsonValidator
import com.bwsw.sj.common.utils.MessageResourceUtils._

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class FileMetadata(val filename: String,
                   val file: Option[File] = None,
                   val name: Option[String] = None,
                   val version: Option[String] = None,
                   val length: Option[Long] = None,
                   val description: Option[String] = None,
                   val uploadDate: Option[String] = None)
  extends JsonValidator {
  protected val fileStorage: MongoFileStorage = ConnectionRepository.getFileStorage
  private val fileMetadataRepository = ConnectionRepository.getFileMetadataRepository

  /**
    * Validates file metadata
    *
    * @return empty array if file metadata is correct, validation errors otherwise
    */
  def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]

    if (!fileStorage.exists(filename)) {
      if (checkCustomFileSpecification(file.get)) {
        val specification = FileMetadata.getSpecification(file.get)
        if (doesCustomJarExist(specification)) errors += createMessage("rest.custom.jars.exists", specification.name, specification.version)
      } else errors += getMessage("rest.errors.invalid.specification")
    } else errors += createMessage("rest.custom.jars.file.exists", filename)

    errors
  }

  /**
    * Indicates that specification of uploading custom jar file is valid
    *
    * @param jarFile uploading jar file
    */
  private def checkCustomFileSpecification(jarFile: File): Boolean = {
    val json = FileMetadata.getSpecificationFromJar(jarFile)
    if (isEmptyOrNullString(json)) {
      return false
    }

    Try(validateWithSchema(json, "customschema.json")) match {
      case Success(isValid) => isValid
      case Failure(_) => false
    }
  }

  private def doesCustomJarExist(specification: SpecificationDomain) = {
    fileMetadataRepository.getByParameters(
      Map("filetype" -> FileMetadata.customJarType,
        "specification.name" -> specification.name,
        "specification.version" -> specification.version
      )).nonEmpty
  }

}

object FileMetadata {
  private val serializer = new JsonSerializer()
  private var maybeSpecification: Option[SpecificationDomain] = None

  val customJarType: String = "custom"
  val customFileType: String = "custom-file"
  val moduleType: String = "module"

  def from(fileMetadataDomain: FileMetadataDomain): FileMetadata = {
    new FileMetadata(
      fileMetadataDomain.filename,
      None,
      Some(fileMetadataDomain.specification.name),
      Some(fileMetadataDomain.specification.version),
      Some(fileMetadataDomain.length),
      Some(fileMetadataDomain.specification.description),
      Some(fileMetadataDomain.uploadDate.toString)
    )
  }

  /**
    * Retrieves [[SpecificationDomain]] from jar file
    *
    * @param jarFile
    * @return specification
    */
  def getSpecification(jarFile: File): SpecificationDomain = {
    maybeSpecification match {
      case Some(specification) =>

        specification
      case None =>
        val serializedSpecification = getSpecificationFromJar(jarFile)

        serializer.deserialize[SpecificationDomain](serializedSpecification)
    }
  }

  /**
    * Retrieves content of specification.json file from root of jar
    *
    * @param file jar file
    * @return content of specification.json
    */
  def getSpecificationFromJar(file: File): String = {
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
