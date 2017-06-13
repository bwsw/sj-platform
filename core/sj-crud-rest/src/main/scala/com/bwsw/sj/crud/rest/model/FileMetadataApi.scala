package com.bwsw.sj.crud.rest.model

import java.io.File

import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.utils.RestLiterals
import scaldi.Injector

class FileMetadataApi(var filename: Option[String] = None,
                      var file: Option[File] = None,
                      var description: String = RestLiterals.defaultDescription,
                      var customFileParts: Map[String, Any] = Map()) {
  def to()(implicit injector: Injector): FileMetadata = {
    new FileMetadata(
      filename = filename.get,
      file = file,
      description = Some(description))
  }
}
