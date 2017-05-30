package com.bwsw.sj.common.si

import java.io.File

import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.si.result._
import org.apache.commons.io.FileUtils
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Provides methods to access custom files represented by [[FileMetadata]] in [[GenericMongoRepository]]
  */
class CustomFilesSI(implicit injector: Injector) extends ServiceInterface[FileMetadata, FileMetadataDomain] {
  private val connectionRepository = inject[ConnectionRepository]
  override protected val entityRepository: GenericMongoRepository[FileMetadataDomain] = connectionRepository.getFileMetadataRepository

  private val fileStorage = connectionRepository.getFileStorage
  private val tmpDirectory = "/tmp/"
  private val previousFilesNames: ListBuffer[String] = ListBuffer[String]()

  private def deletePreviousFiles() = {
    previousFilesNames.foreach(filename => {
      val file = new File(filename)
      if (file.exists()) file.delete()
    })
  }

  override def create(entity: FileMetadata): CreationResult = {
    if (!fileStorage.exists(entity.filename)) {
      val uploadingFile = new File(entity.filename)
      FileUtils.copyFile(entity.file.get, uploadingFile)
      fileStorage.put(uploadingFile, entity.filename, Map("description" -> entity.description), FileMetadata.customFileType)
      uploadingFile.delete()

      Created
    } else {
      NotCreated()
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

  override def delete(name: String): DeletionResult = {
    val fileMetadatas = entityRepository.getByParameters(Map("filename" -> name))

    if (fileMetadatas.isEmpty)
      EntityNotFound
    else {
      if (fileStorage.delete(name))
        Deleted
      else
        DeletionError(s"Can't delete jar '$name' for some reason. It needs to be debugged.")
    }
  }
}

