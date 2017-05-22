package com.bwsw.sj.common.si

import java.io.File

import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.si.model.module.ModuleMetadata
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class ModuleSI extends JsonValidator {

  private val fileStorage = ConnectionRepository.getFileStorage
  private val fileMetadataRepository = ConnectionRepository.getFileMetadataRepository
  private val instanceRepository = ConnectionRepository.getInstanceRepository
  private val entityRepository: GenericMongoRepository[FileMetadataDomain] = ConnectionRepository.getFileMetadataRepository
  private val tmpDirectory = "/tmp/"
  private val previousFilesNames: ListBuffer[String] = ListBuffer[String]()

  def create(entity: ModuleMetadata): Either[ArrayBuffer[String], Boolean] = {
    val modules = entity.map(getFilesMetadata)

    if (modules.nonEmpty) {
      Left(
        ArrayBuffer[String](
          createMessage("rest.modules.module.exists", entity.signature)))
    } else {
      val errors = new ArrayBuffer[String]
      errors ++= entity.validate()

      if (errors.isEmpty) {
        val uploadingFile = new File(entity.filename)
        FileUtils.copyFile(entity.file.get, uploadingFile)
        fileStorage.put(uploadingFile, entity.filename, entity.specification.to, FileMetadata.moduleType)

        Right(true)
      } else {
        Left(errors)
      }
    }
  }

  def get(moduleType: String, moduleName: String, moduleVersion: String): Either[String, ModuleMetadata] = {
    exists(moduleType, moduleName, moduleVersion).map { metadata =>
      deletePreviousFiles()
      val file = fileStorage.get(metadata.filename, tmpDirectory + metadata.filename)
      previousFilesNames.append(file.getAbsolutePath)

      ModuleMetadata.from(metadata, Option(file))
    }
  }

  def getAll: mutable.Buffer[ModuleMetadata] = {
    entityRepository
      .getByParameters(Map("filetype" -> FileMetadata.moduleType))
      .map(ModuleMetadata.from(_))
  }

  def getMetadataWithoutFile(moduleType: String, moduleName: String, moduleVersion: String): Either[String, ModuleMetadata] =
    exists(moduleType, moduleName, moduleVersion).map(ModuleMetadata.from(_))

  def getFileName(moduleType: String, moduleName: String, moduleVersion: String) = {
    val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)

    filesMetadata.head.filename
  }

  def getByType(moduleType: String): Either[String, mutable.Buffer[ModuleMetadata]] = {
    if (EngineLiterals.moduleTypes.contains(moduleType)) {
      val modules = fileMetadataRepository.getByParameters(
        Map("filetype" -> "module", "specification.module-type" -> moduleType))
        .map(ModuleMetadata.from(_))

      Right(modules)
    } else
      Left(createMessage("rest.modules.type.unknown", moduleType))
  }

  def getRelatedInstances(moduleType: String, moduleName: String, moduleVersion: String): mutable.Buffer[String] = {
    instanceRepository.getByParameters(Map(
      "module-name" -> moduleName,
      "module-type" -> moduleType,
      "module-version" -> moduleVersion)
    ).map(_.name)
  }

  def delete(metadata: ModuleMetadata): Either[String, Boolean] = {
    if (metadata.map(getRelatedInstances).isEmpty) {
      Left(createMessage(
        "rest.modules.module.cannot.delete",
        metadata.signature))
    } else if (fileStorage.delete(metadata.filename))
      Right(true)
    else
      Left(createMessage("rest.cannot.delete.file", metadata.filename))
  }

  def exists(moduleType: String, moduleName: String, moduleVersion: String): Either[String, FileMetadataDomain] = {
    val moduleSignature = ModuleMetadata.getModuleSignature(moduleType, moduleName, moduleVersion)
    val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)
    if (filesMetadata.isEmpty)
      Left(createMessage("rest.modules.module.notfound", moduleSignature))
    else if (!fileStorage.exists(filesMetadata.head.filename))
      Left(createMessage("rest.modules.module.jar.notfound", moduleSignature))
    else
      Right(filesMetadata.head)
  }

  private def getFilesMetadata(moduleType: String, moduleName: String, moduleVersion: String) = {
    fileMetadataRepository.getByParameters(Map("filetype" -> "module",
      "specification.name" -> moduleName,
      "specification.module-type" -> moduleType,
      "specification.version" -> moduleVersion)
    )
  }

  private def deletePreviousFiles() = {
    previousFilesNames.foreach(filename => {
      val file = new File(filename)
      if (file.exists()) file.delete()
    })
  }
}
