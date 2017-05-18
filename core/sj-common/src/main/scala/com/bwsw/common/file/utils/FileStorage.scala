package com.bwsw.common.file.utils

import java.io.{InputStream, File}

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.si.model.module.Specification
import org.slf4j.LoggerFactory

trait FileStorage {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  def put(file: File, fileName: String): Unit

  def put(file: File, fileName: String, specification: Map[String, Any], filetype: String): Unit

  def put(file: File, fileName: String, specification: Specification, filetype: String): Unit = {
    val serializer = new JsonSerializer()
    val json = serializer.serialize(specification)
    val map = serializer.deserialize[Map[String, Any]](json)

    put(file, fileName, map, filetype)
  }

  def get(fileName: String, newFileName: String): File

  def getStream(fileName: String): InputStream

  def delete(fileName: String): Boolean

  def getContent(): Seq[String]

  def exists(fileName: String): Boolean

}
