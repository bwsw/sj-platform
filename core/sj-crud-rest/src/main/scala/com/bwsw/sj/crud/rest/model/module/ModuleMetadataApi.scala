package com.bwsw.sj.crud.rest.model.module

import java.io.File

import com.bwsw.sj.common.si.model.module.ModuleMetadata
import com.bwsw.sj.common.utils.RestLiterals
import com.bwsw.sj.crud.rest.ModuleInfo
import com.bwsw.sj.crud.rest.model.FileMetadataApi

class ModuleMetadataApi(filename: String,
                        file: File,
                        description: String = RestLiterals.defaultDescription,
                        customFileParts: Map[String, Any] = Map())
  extends FileMetadataApi(
    Option(filename),
    Option(file),
    description,
    customFileParts) {

  override def to(): ModuleMetadata =
    new ModuleMetadata(filename, SpecificationApi.from(file).to, Option(file))
}

object ModuleMetadataApi {
  def toModuleInfo(moduleMetadata: ModuleMetadata): ModuleInfo = {
    ModuleInfo(
      moduleMetadata.specification.moduleType,
      moduleMetadata.specification.name,
      moduleMetadata.specification.version,
      moduleMetadata.length.get)
  }
}
