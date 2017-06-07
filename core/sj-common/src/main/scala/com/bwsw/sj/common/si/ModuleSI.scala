package com.bwsw.sj.common.si

import java.io.File

import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadataLiterals
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, CreateModuleMetadata}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.{EngineLiterals, MessageResourceUtils}
import org.apache.commons.io.FileUtils
import scaldi.Injectable.inject
import scaldi.Injector

class ModuleSI(implicit injector: Injector) extends JsonValidator {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils.createMessage

  private val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  private val fileStorage = connectionRepository.getFileStorage
  private val instanceRepository = connectionRepository.getInstanceRepository
  private val entityRepository: GenericMongoRepository[FileMetadataDomain] = connectionRepository.getFileMetadataRepository
  private val tmpDirectory = "/tmp/"
  private val fileBuffer = inject[FileBuffer]
  private val createModuleMetadata = inject[CreateModuleMetadata]

  def create(entity: ModuleMetadata): CreationResult = {
    val errors = entity.validate()

    if (errors.isEmpty) {
      val uploadingFile = new File(entity.filename)
      FileUtils.copyFile(entity.file.get, uploadingFile)
      fileStorage.put(uploadingFile, entity.filename, entity.specification.to, FileMetadataLiterals.moduleType)
      uploadingFile.delete()

      Created
    } else {
      NotCreated(errors)
    }
  }

  def get(moduleType: String, moduleName: String, moduleVersion: String): Either[String, ModuleMetadata] = {
    exists(moduleType, moduleName, moduleVersion).map { metadata =>
      fileBuffer.clear()
      val file = fileStorage.get(metadata.filename, tmpDirectory + metadata.filename)
      fileBuffer.append(file)

      createModuleMetadata.from(metadata, Option(file))
    }
  }

  def getAll: Seq[ModuleMetadata] = {
    entityRepository
      .getByParameters(Map("filetype" -> FileMetadataLiterals.moduleType))
      .map(createModuleMetadata.from(_))
  }

  def getMetadataWithoutFile(moduleType: String, moduleName: String, moduleVersion: String): Either[String, ModuleMetadata] =
    exists(moduleType, moduleName, moduleVersion).map(createModuleMetadata.from(_))

  def getByType(moduleType: String): Either[String, Seq[ModuleMetadata]] = {
    if (EngineLiterals.moduleTypes.contains(moduleType)) {
      val modules = entityRepository.getByParameters(
        Map("filetype" -> FileMetadataLiterals.moduleType, "specification.module-type" -> moduleType))
        .map(createModuleMetadata.from(_))

      Right(modules)
    } else
      Left(createMessage("rest.modules.type.unknown", moduleType))
  }

  def getRelatedInstances(metadata: ModuleMetadata): Seq[String] = {
    instanceRepository.getByParameters(Map(
      "module-name" -> metadata.specification.name,
      "module-type" -> metadata.specification.moduleType,
      "module-version" -> metadata.specification.version)
    ).map(_.name)
  }

  def delete(metadata: ModuleMetadata): DeletionResult = {
    if (getRelatedInstances(metadata).nonEmpty) {
      DeletionError(createMessage(
        "rest.modules.module.cannot.delete",
        metadata.signature))
    } else if (fileStorage.delete(metadata.filename))
      Deleted
    else
      DeletionError(createMessage("rest.cannot.delete.file", metadata.filename))
  }

  def exists(moduleType: String, moduleName: String, moduleVersion: String): Either[String, FileMetadataDomain] = {
    if (EngineLiterals.moduleTypes.contains(moduleType)) {
      val moduleSignature = ModuleMetadata.createModuleSignature(moduleType, moduleName, moduleVersion)
      val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)
      if (filesMetadata.isEmpty)
        Left(createMessage("rest.modules.module.notfound", moduleSignature))
      else {
        if (!fileStorage.exists(filesMetadata.head.filename))
          Left(createMessage("rest.modules.module.jar.notfound", moduleSignature))
        else
          Right(filesMetadata.head)
      }
    } else {
      Left(createMessage("rest.modules.type.unknown", moduleType))
    }
  }

  private def getFilesMetadata(moduleType: String, moduleName: String, moduleVersion: String) = {
    entityRepository.getByParameters(Map("filetype" -> FileMetadataLiterals.moduleType,
      "specification.name" -> moduleName,
      "specification.module-type" -> moduleType,
      "specification.version" -> moduleVersion)
    )
  }
}
