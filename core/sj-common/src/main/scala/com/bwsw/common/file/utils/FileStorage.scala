package com.bwsw.common.file.utils

import java.io.{File, InputStream}

import com.bwsw.sj.common.dal.model.module.SpecificationDomain
import org.slf4j.LoggerFactory

trait FileStorage {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  def put(file: File, fileName: String): Unit

  def put(file: File, fileName: String, specification: Map[String, Any], filetype: String): Unit

  def put(file: File, fileName: String, specification: SpecificationDomain, filetype: String): Unit

  def get(fileName: String, newFileName: String): File

  def getStream(fileName: String): InputStream

  def delete(fileName: String): Boolean

  def getContent(): Seq[String]

  def exists(fileName: String): Boolean

}
