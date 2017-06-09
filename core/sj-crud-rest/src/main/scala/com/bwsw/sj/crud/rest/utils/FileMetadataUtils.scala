package com.bwsw.sj.crud.rest.utils

import java.io.File
import java.nio.file.Paths

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.crud.rest.{CustomFileInfo, CustomJarInfo}

import scala.concurrent.Future

class FileMetadataUtils {
  def toCustomJarInfo(fileMetadata: FileMetadata): CustomJarInfo = {
    CustomJarInfo(
      fileMetadata.name.get,
      fileMetadata.version.get,
      fileMetadata.length.get)
  }

  def toCustomFileInfo(fileMetadata: FileMetadata): CustomFileInfo = {
    CustomFileInfo(
      fileMetadata.filename,
      fileMetadata.description.get,
      fileMetadata.uploadDate.get,
      fileMetadata.length.get)
  }

  def fileToSource(file: File): Source[ByteString, Future[IOResult]] =
    FileIO.fromPath(Paths.get(file.getAbsolutePath))
}
