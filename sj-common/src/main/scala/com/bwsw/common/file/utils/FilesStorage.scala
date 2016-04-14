package com.bwsw.common.file.utils

import java.io.File

trait FilesStorage {
  def put(file: File, fileName: String)

  def put(file: File, fileName: String, specification: Map[String, Any], filetype: String)

  def get(fileName: String, newFileName: String): File

  def delete(fileName: String): Boolean

  def getContent(path: String = null): Seq[String]

}