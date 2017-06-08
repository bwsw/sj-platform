package com.bwsw.sj.crud.rest.utils

import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.crud.rest.{CustomFileInfo, CustomJarInfo}

object FileMetadataUtils {
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
}
