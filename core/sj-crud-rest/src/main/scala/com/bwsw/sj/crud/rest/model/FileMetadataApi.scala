package com.bwsw.sj.crud.rest.model

import java.io.File

import akka.http.scaladsl.model.Multipart
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.utils.RestLiterals
import com.bwsw.sj.crud.rest.{CustomFileInfo, CustomJarInfo}

class FileMetadataApi(var filename: String = "dummy",
                      var file: Option[File] = None,
                      var formData: Option[Multipart.FormData] = None,
                      var description: String = RestLiterals.defaultDescription) {
  def to(): FileMetadata = {
    new FileMetadata(filename, file)
  }
}

object FileMetadataApi {
  def toCustomJarInfo(fileMetadata: FileMetadata): CustomJarInfo = {
    CustomJarInfo(
      fileMetadata.name.get,
      fileMetadata.version.get,
      fileMetadata.length.get
    )
  }

  def toCustomFileInfo(fileMetadata: FileMetadata): CustomFileInfo = {
    CustomFileInfo(
      fileMetadata.filename,
      fileMetadata.description.get,
      fileMetadata.uploadDate.get,
      fileMetadata.length.get
    )
  }
}