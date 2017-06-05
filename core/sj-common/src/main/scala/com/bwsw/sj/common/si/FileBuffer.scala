package com.bwsw.sj.common.si

import java.io.File

import scala.collection.mutable.ListBuffer

/**
  * Stores temporary files
  */
class FileBuffer {

  private val files: ListBuffer[File] = ListBuffer[File]()

  def clear(): Unit = {
    files.foreach(_.delete())
    files.clear()
  }

  def append(file: File): Unit = files.append(file)
}
