package com.bwsw.common.file.utils

import java.io.File

import com.bwsw.common.exceptions.BadRecordWithKey
import org.apache.commons.io.FileUtils

import scala.reflect.io.{Directory, Path}

class LocalStorage(pathToLocalStorage: String) extends FilesStorage {
  override def put(file: File, fileName: String): Unit = {
    val storageFile = new File(pathToLocalStorage + fileName)
    if (!storageFile.exists) {
      FileUtils.copyFile(file, storageFile)
    } else throw BadRecordWithKey(s"$fileName already exists", fileName)
  }

  override def get(fileName: String, newFileName: String): File = {
    val file: File = new File(newFileName)
    val storageFile = new File(pathToLocalStorage + fileName)
    if (storageFile.exists()) {
      FileUtils.copyFile(storageFile, file)

      file
    } else throw new BadRecordWithKey(s"$fileName doesn't exist", fileName)
  }

  override def delete(fileName: String): Boolean = {
    val file = new File(pathToLocalStorage + fileName)
    if (file.exists) {
      file.delete()
    } else false
  }

  override def getContent(path: String): Seq[String] = {
    Directory.apply(Path(pathToLocalStorage).toAbsolute).files.map(_.name).toSeq
  }

  override def put(file: File, fileName: String, specification: Map[String, Any], filetype: String) = {
    put(file, fileName)
  }

  override def exists(fileName: String): Boolean = {
    val file = new File(pathToLocalStorage + fileName)
    file.exists
  }
}
