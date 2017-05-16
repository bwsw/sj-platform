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
      fileStorage.put(uploadingFile, entity.filename, specification, "custom")
      val name = specification("name").toString + "-" + specification("version").toString
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
    entityRepository.getByParameters(Map("filetype" -> "custom")).map(x => FileMetadata.from(x))
  }

  override def get(name: String): Option[File] = {
    if (fileStorage.exists(name)) {
      deletePreviousFiles()
      val jarFile = fileStorage.get(name, tmpDirectory + name)
      previousFilesNames.append(jarFile.getAbsolutePath)

      Some(jarFile)
    } else {
      None
    }
  }

//  override def delete(name: String): Either[String, Boolean] = {
//    var response: Either[String, Boolean] = Left(createMessage("rest.services.service.cannot.delete.due.to.streams", name))
//    val streams = getRelatedStreams(name)
//
//    if (streams.isEmpty) {
//      response = Left(createMessage("rest.services.service.cannot.delete.due.to.instances", name))
//      val instances = getRelatedInstances(name)
//
//      if (instances.isEmpty) {
//        val provider = entityRepository.get(name)
//
//        provider match {
//          case Some(_) =>
//            entityRepository.delete(name)
//            response = Right(true)
//          case None =>
//            response = Right(false)
//        }
//      }
//    }
//
//    response
//  }
  override def delete(name: String): Either[String, Boolean] = ???
}
