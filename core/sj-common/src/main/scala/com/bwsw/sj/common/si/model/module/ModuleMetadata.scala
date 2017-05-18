package com.bwsw.sj.common.si.model.module

import java.io.File

import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage

import scala.collection.mutable.ArrayBuffer

class ModuleMetadata(filename: String,
                     val specification: Specification,
                     file: Option[File] = None,
                     name: Option[String] = None,
                     version: Option[String] = None,
                     length: Option[Long] = None,
                     description: Option[String] = None,
                     uploadDate: Option[String] = None)
  extends FileMetadata(
    filename,
    file,
    name,
    version,
    length,
    description,
    uploadDate) {

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]

    if (!filename.endsWith(".jar"))
      errors += createMessage("rest.modules.modules.extension.unknown", filename)

    if (fileStorage.exists(filename))
      errors += createMessage("rest.modules.module.file.exists", filename)

    errors ++= specification.validate

    errors
  }

  /**
    * Apply method f(moduleType, moduleName, moduleVersion) to this.
    */
  def map[T](f: (String, String, String) => T): T =
    f(specification.moduleType, specification.name, specification.version)

  lazy val signature: String =
    ModuleMetadata.getModuleSignature(specification.moduleType, specification.name, specification.version)
}

object ModuleMetadata {
  def from(fileMetadata: FileMetadataDomain, file: Option[File] = None): ModuleMetadata = {
    val specification = Specification.from(fileMetadata.specification)

    new ModuleMetadata(
      fileMetadata.filename,
      specification,
      file = file,
      name = Option(specification.name),
      version = Option(specification.version),
      length = Option(fileMetadata.length),
      description = Option(specification.description),
      uploadDate = Option(fileMetadata.uploadDate.toString))
  }

  def getModuleSignature(moduleType: String, moduleName: String, moduleVersion: String): String =
    moduleType + "-" + moduleName + "-" + moduleVersion
}
