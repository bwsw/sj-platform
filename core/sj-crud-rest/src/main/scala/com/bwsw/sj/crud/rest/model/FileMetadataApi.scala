package com.bwsw.sj.crud.rest.model

import java.io.File

import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.crud.rest.CustomJarInfo

class FileMetadataApi(val filename: String, val file: Option[File]) {
  def to(): FileMetadata = {
    new FileMetadata(filename, file)
  }
}

object FileMetadataApi {
  def from(fileMetadata: FileMetadata): CustomJarInfo = {
    CustomJarInfo(
      fileMetadata.name.get,
      fileMetadata.version.get,
      fileMetadata.length.get
    )
  }
}