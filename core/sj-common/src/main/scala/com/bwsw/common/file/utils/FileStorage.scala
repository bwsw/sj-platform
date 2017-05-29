package com.bwsw.common.file.utils

import java.io.{File, InputStream}

import com.bwsw.sj.common.dal.model.module.SpecificationDomain
import org.slf4j.{Logger, LoggerFactory}

/**
  * Provides methods to CRUD files using a specific storage
  */
trait FileStorage {

  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def put(file: File, fileName: String): Unit

  def put(file: File, fileName: String, specification: Map[String, Any], filetype: String): Unit

  def put(file: File, fileName: String, specification: SpecificationDomain, filetype: String): Unit

  def get(fileName: String, newFileName: String): File

  /**
    * Retrieve file as [[InputStream]]
    */
  def getStream(fileName: String): InputStream

  def delete(fileName: String): Boolean

  def exists(fileName: String): Boolean

}
