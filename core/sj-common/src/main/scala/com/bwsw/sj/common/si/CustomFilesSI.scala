package com.bwsw.sj.common.si

import java.io.File

import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadata
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
  * Provides methods to access custom files represented by [[FileMetadata]] in [[GenericMongoRepository]]
  */
class CustomFilesSI extends ServiceInterface[FileMetadata, FileMetadataDomain] {
  override protected val entityRepository: GenericMongoRepository[FileMetadataDomain] = ConnectionRepository.getFileMetadataRepository

  private val fileStorage = ConnectionRepository.getFileStorage
  private val tmpDirectory = "/tmp/"
  private val previousFilesNames: ListBuffer[String] = ListBuffer[String]()

  private def deletePreviousFiles() = {
    previousFilesNames.foreach(filename => {
      val file = new File(filename)
      if (file.exists()) file.delete()
    })
  }

  override def create(entity: FileMetadata): Either[ArrayBuffer[String], Boolean] = {
    if (!fileStorage.exists(entity.filename)) {
      val uploadingFile = new File(entity.filename)
      FileUtils.copyFile(entity.file.get, uploadingFile)
      fileStorage.put(uploadingFile, entity.filename, Map("description" -> entity.description), FileMetadata.customFileType)
      uploadingFile.delete()

      Right(true)
    } else {
      Left(ArrayBuffer())
    }
  }

  override def getAll(): mutable.Buffer[FileMetadata] = {
    entityRepository.getByParameters(Map("filetype" -> FileMetadata.customFileType)).map(x => FileMetadata.from(x))
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
    val fileMetadatas = entityRepository.getByParameters(Map("filename" -> name))
    var response: Either[String, Boolean] = Right(false)

    if (fileMetadatas.nonEmpty) {
      if (fileStorage.delete(name)) {
        response = Right(true)
      } else {
        response = Left(s"Can't delete jar '$name' for some reason. It needs to be debugged.")
      }
    }

    response
  }
}

