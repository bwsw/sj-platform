/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.common.si.model

import java.io.File

import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.dal.model.module.{FileMetadataDomain, SpecificationDomain}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.JsonValidator
import com.bwsw.sj.common.utils.{MessageResourceUtils, SpecificationUtils}
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class FileMetadata(val filename: String,
                   val file: Option[File] = None,
                   val name: Option[String] = None,
                   val version: Option[String] = None,
                   val length: Option[Long] = None,
                   val description: Option[String] = None,
                   val uploadDate: Option[String] = None)
                  (implicit injector: Injector)
  extends JsonValidator {

  protected val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils._

  protected val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  protected val fileStorage: MongoFileStorage = connectionRepository.getFileStorage
  protected val fileMetadataRepository = connectionRepository.getFileMetadataRepository

  /**
    * Validates file metadata
    *
    * @return empty array if file metadata is correct, validation errors otherwise
    */
  def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]

    if (!fileStorage.exists(filename)) {
      if (checkCustomFileSpecification(file.get)) {
        val specification = inject[SpecificationUtils].getSpecification(file.get)
        if (doesCustomJarExist(specification)) errors += createMessage("rest.custom.jars.exists", specification.name, specification.version)
      } else errors += getMessage("rest.errors.invalid.specification")
    } else errors += createMessage("rest.custom.jars.file.exists", filename)

    errors
  }

  /**
    * Indicates that specification of uploading custom jar file is valid
    *
    * @param jarFile uploading jar file
    */
  private def checkCustomFileSpecification(jarFile: File): Boolean = {
    val json = inject[SpecificationUtils].getSpecificationFromJar(jarFile)
    if (isEmptyOrNullString(json)) {
      return false
    }

    Try(validateWithSchema(json, "customschema.json")) match {
      case Success(isValid) => isValid
      case Failure(_) => false
    }
  }

  private def doesCustomJarExist(specification: SpecificationDomain) = {
    fileMetadataRepository.getByParameters(
      Map("filetype" -> FileMetadataLiterals.customJarType,
        "specification.name" -> specification.name,
        "specification.version" -> specification.version
      )).nonEmpty
  }

}

object FileMetadataLiterals {
  val customJarType: String = "custom"
  val customFileType: String = "custom-file"
  val moduleType: String = "module"
}

class FileMetadataCreator {
  def from(fileMetadataDomain: FileMetadataDomain)(implicit injector: Injector): FileMetadata = {
    new FileMetadata(
      fileMetadataDomain.filename,
      None,
      Some(fileMetadataDomain.specification.name),
      Some(fileMetadataDomain.specification.version),
      Some(fileMetadataDomain.length),
      Some(fileMetadataDomain.specification.description),
      Some(fileMetadataDomain.uploadDate.toString)
    )
  }
}
