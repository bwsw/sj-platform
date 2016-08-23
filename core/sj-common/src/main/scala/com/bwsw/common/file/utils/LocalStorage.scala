package com.bwsw.common.file.utils

import java.io.{FileNotFoundException, File}
import java.nio.file.FileAlreadyExistsException

import org.apache.commons.io.FileUtils

import scala.reflect.io.{Directory, Path}

class LocalStorage(pathToLocalStorage: String) extends FileStorage {
  override def put(file: File, fileName: String): Unit = {
    logger.debug(s"Try to put a file: '$fileName' in a local storage (path to local storage: $pathToLocalStorage)")
    val storageFile = new File(pathToLocalStorage + fileName)
    logger.debug(s"Check whether a local storage already contains a file with name: '$fileName' or not")
    if (!storageFile.exists()) {
      logger.debug(s"Create file in a local storage. Absolute path of file: '${pathToLocalStorage + fileName}'")
      FileUtils.copyFile(file, storageFile)
    } else {
      logger.error(s"File with name: '$fileName' already exists in a local storage")
      throw new FileAlreadyExistsException(s"$fileName already exists")
    }
  }

  override def get(fileName: String, newFileName: String): File = {
    logger.debug(s"Try to get a file: '$fileName' from a local storage (path to local storage: $pathToLocalStorage)")
    val file: File = new File(newFileName)
    val storageFile = new File(pathToLocalStorage + fileName)
    logger.debug(s"Check whether a local storage contains a file with name: '$fileName' or not")
    if (storageFile.exists()) {
      logger.debug(s"Copy a file: '$fileName' from a local storage to file with name: '$newFileName'")
      FileUtils.copyFile(storageFile, file)

      file
    } else {
      logger.error(s"File with name: '$fileName' doesn't exist in a local storage")
      throw new FileNotFoundException(s"$fileName doesn't exist")
    }
  }

  override def delete(fileName: String): Boolean = {
    logger.debug(s"Try to delete a file: '$fileName' from a local storage (path to local storage: $pathToLocalStorage)")
    val file = new File(pathToLocalStorage + fileName)
    logger.debug(s"Check whether a local storage contains a file with name: '$fileName' or not")
    if (file.exists) {
      logger.debug(s"Delete a file from a local storage. Absolute path of file: '${pathToLocalStorage + fileName}'")
      file.delete()
    } else {
      logger.debug(s"Local storage doesn't contain a file with name: '$fileName'")
      false
    }
  }

  override def getContent(): Seq[String] = {
    logger.debug(s"Get a list of contents of a local storage directory")
    Directory.apply(Path(pathToLocalStorage).toAbsolute).files.map(_.name).toSeq
  }

  override def put(file: File, fileName: String, specification: Map[String, Any], filetype: String) = {
    throw new NotImplementedError("Local storage hasn't an opportunity to save file with specification yet")
  }

  override def exists(fileName: String): Boolean = {
    logger.debug(s"Check whether a local storage contains a file with name: '$fileName' or not")
    val file = new File(pathToLocalStorage + fileName)
    file.exists
  }
}
