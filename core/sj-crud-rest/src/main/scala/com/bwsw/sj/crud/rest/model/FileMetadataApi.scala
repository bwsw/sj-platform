package com.bwsw.sj.crud.rest.model

import java.io.File

import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.utils.RestLiterals
import com.bwsw.sj.crud.rest.{CustomFileInfo, CustomJarInfo}
import scaldi.Injector

class FileMetadataApi(var filename: Option[String] = None,
                      var file: Option[File] = None,
                      var description: String = RestLiterals.defaultDescription,
                      var customFileParts: Map[String, Any] = Map()) {
  def to()(implicit injector: Injector): FileMetadata = {
    new FileMetadata(filename.get, file)
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