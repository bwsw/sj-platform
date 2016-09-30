package com.bwsw.common.file.utils

import java.io.File

import org.slf4j.LoggerFactory

trait FileStorage {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  def put(file: File, fileName: String)

  def put(file: File, fileName: String, specification: Map[String, Any], filetype: String)

  def get(fileName: String, newFileName: String): File

  def delete(fileName: String): Boolean

  def getContent(): Seq[String]

  def exists(fileName: String): Boolean

}
