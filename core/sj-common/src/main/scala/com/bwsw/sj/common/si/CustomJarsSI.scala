package com.bwsw.sj.common.si

import java.io._

import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
  * Provides methods to access custom jar files represented by [[FileMetadata]] in [[GenericMongoRepository]]
  */
class CustomJarsSI extends ServiceInterface[FileMetadata, FileMetadataDomain] {
  override protected val entityRepository: GenericMongoRepository[FileMetadataDomain] = ConnectionRepository.getFileMetadataRepository

  private val fileStorage = ConnectionRepository.getFileStorage
  private val configRepository = ConnectionRepository.getConfigRepository
  private val tmpDirectory = "/tmp/"
  private val previousFilesNames: ListBuffer[String] = ListBuffer[String]()

  private def deletePreviousFiles() = {
    previousFilesNames.foreach(filename => {
      val file = new File(filename)
      if (file.exists()) file.delete()
    })
  }

  override def create(entity: FileMetadata): Either[ArrayBuffer[String], Boolean] = {
    val errors = new ArrayBuffer[String]

    errors ++= entity.validate()

    if (errors.isEmpty) {
      val specification = FileMetadata.getSpecification(entity.file.get)
      val uploadingFile = new File(entity.filename)
      FileUtils.copyFile(entity.file.get, uploadingFile)
      val specificationMap = serializer.deserialize[Map[String, Any]](serializer.serialize(specification))
      fileStorage.put(uploadingFile, entity.filename, specificationMap, FileMetadata.customJarType)
      val name = specification.name + "-" + specification.version
      val customJarConfig = ConfigurationSettingDomain(
        ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, name),
        entity.filename,
        ConfigLiterals.systemDomain
      )
      configRepository.save(customJarConfig)

      Right(true)
    } else {
      Left(errors)
    }
  }

  override def getAll(): mutable.Buffer[FileMetadata] = {
    entityRepository.getByParameters(Map("filetype" -> FileMetadata.customJarType)).map(x => FileMetadata.from(x))
  }

  override def get(name: String): Option[FileMetadata] = {
    if (fileStorage.exists(name)) {
      deletePreviousFiles()
      val jarFile = fileStorage.get(name, tmpDirectory + name)
      previousFilesNames.append(jarFile.getAbsolutePath)

      Some(new FileMetadata(name, Some(jarFile)))
    } else {
      None
    }
  }

  override def delete(name: String): Either[String, Boolean] = {
    val fileMetadatas = entityRepository.getByParameters(Map("filetype" -> FileMetadata.customJarType, "filename" -> name))
    var response: Either[String, Boolean] = Right(false)

    if (fileMetadatas.nonEmpty) {
      val fileMetadata = fileMetadatas.head

      if (fileStorage.delete(name)) {
        configRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain,
          fileMetadata.specification.name + "-" + fileMetadata.specification.version))
        response = Right(true)
      } else {
        response = Left(s"Can't delete jar '$name' for some reason. It needs to be debugged.")
      }
    }

    response
  }

  /**
    * Returns custom jar file with this name and version from [[entityRepository]]
    *
    * @param name
    * @param version
    */
  def getBy(name: String, version: String): Option[FileMetadata] = {
    val fileMetadatas = entityRepository.getByParameters(Map("filetype" -> FileMetadata.customJarType,
      "specification.name" -> name,
      "specification.version" -> version)
    )

    if (fileMetadatas.nonEmpty) {
      val filename = fileMetadatas.head.filename
      deletePreviousFiles()
      val jarFile = fileStorage.get(filename, tmpDirectory + filename)
      previousFilesNames.append(jarFile.getAbsolutePath)

      Some(new FileMetadata(name, Some(jarFile)))
    } else {
      None
    }
  }

  /**
    * Deletes custom jar file with this name and version from [[entityRepository]]
    *
    * @param name
    * @param version
    * @return Right(true) if custom jar file deleted, Right(false) if custom jar file not found in [[entityRepository]],
    *         Left(error) if some error happened
    */
  def deleteBy(name: String, version: String): Either[String, Boolean] = {
    val fileMetadatas = entityRepository.getByParameters(Map("filetype" -> FileMetadata.customJarType,
      "specification.name" -> name,
      "specification.version" -> version)
    )
    var response: Either[String, Boolean] = Right(false)

    if (fileMetadatas.nonEmpty) {
      val filename = fileMetadatas.head.filename

      if (fileStorage.delete(filename)) {
        configRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, s"$name-$version"))
        response = Right(true)
      } else {
        response = Left(s"Can't delete jar '$filename' for some reason. It needs to be debugged.")
      }
    }

    response
  }
}
