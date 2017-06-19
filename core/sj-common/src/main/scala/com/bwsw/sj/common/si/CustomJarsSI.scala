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
package com.bwsw.sj.common.si

import java.io._

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.si.model.{FileMetadata, FileMetadataCreator, FileMetadataLiterals}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.SpecificationUtils
import org.apache.commons.io.FileUtils
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Provides methods to access custom jar files represented by [[FileMetadata]] in [[GenericMongoRepository]]
  */
class CustomJarsSI(implicit injector: Injector) extends ServiceInterface[FileMetadata, FileMetadataDomain] {
  private val serializer = inject[JsonSerializer]
  private val connectionRepository = inject[ConnectionRepository]
  override protected val entityRepository: GenericMongoRepository[FileMetadataDomain] = connectionRepository.getFileMetadataRepository

  private val fileStorage = connectionRepository.getFileStorage
  private val configRepository = connectionRepository.getConfigRepository
  private val tmpDirectory = "/tmp/"
  private val fileBuffer = inject[FileBuffer]

  override def create(entity: FileMetadata): CreationResult = {
    val errors = entity.validate()

    if (errors.isEmpty) {
      val specification = inject[SpecificationUtils].getSpecification(entity.file.get)
      val uploadingFile = new File(entity.filename)
      FileUtils.copyFile(entity.file.get, uploadingFile)
      val specificationMap = serializer.deserialize[Map[String, Any]](serializer.serialize(specification))
      fileStorage.put(uploadingFile, entity.filename, specificationMap, FileMetadataLiterals.customJarType)
      val name = specification.name + "-" + specification.version
      val customJarConfig = ConfigurationSettingDomain(
        ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, name),
        entity.filename,
        ConfigLiterals.systemDomain
      )
      configRepository.save(customJarConfig)
      uploadingFile.delete()

      Created
    } else NotCreated(errors)
  }

  override def getAll(): Seq[FileMetadata] = {
    entityRepository.getByParameters(Map("filetype" -> FileMetadataLiterals.customJarType))
      .map(x => inject[FileMetadataCreator].from(x))
  }

  override def get(name: String): Option[FileMetadata] = {
    if (fileStorage.exists(name)) {
      fileBuffer.clear()
      val jarFile = fileStorage.get(name, tmpDirectory + name)
      fileBuffer.append(jarFile)

      Some(new FileMetadata(name, Some(jarFile)))
    } else {
      None
    }
  }

  override def delete(name: String): DeletionResult = {
    val fileMetadatas = entityRepository.getByParameters(Map("filetype" -> FileMetadataLiterals.customJarType, "filename" -> name))

    if (fileMetadatas.isEmpty)
      EntityNotFound
    else {
      if (fileStorage.delete(name)) {
        val fileMetadata = fileMetadatas.head
        configRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain,
          fileMetadata.specification.name + "-" + fileMetadata.specification.version))

        Deleted
      } else DeletionError(s"Can't delete jar '$name' for some reason. It needs to be debugged.")
    }
  }

  /**
    * Returns custom jar file with this name and version from [[entityRepository]]
    *
    * @param name    name of custom jar file from [[com.bwsw.sj.common.si.model.module.Specification]]
    * @param version version of custom jar file from [[com.bwsw.sj.common.si.model.module.Specification]]
    */
  def getBy(name: String, version: String): Option[FileMetadata] = {
    val fileMetadatas = entityRepository.getByParameters(Map("filetype" -> FileMetadataLiterals.customJarType,
      "specification.name" -> name,
      "specification.version" -> version)
    )

    if (fileMetadatas.nonEmpty) {
      val filename = fileMetadatas.head.filename
      fileBuffer.clear()
      val jarFile = fileStorage.get(filename, tmpDirectory + filename)
      fileBuffer.append(jarFile)

      Some(new FileMetadata(filename, Some(jarFile)))
    } else {
      None
    }
  }

  /**
    * Deletes custom jar file with this name and version from [[entityRepository]]
    *
    * @param name    name of custom jar file from [[com.bwsw.sj.common.si.model.module.Specification]]
    * @param version version of custom jar file from [[com.bwsw.sj.common.si.model.module.Specification]]
    */
  def deleteBy(name: String, version: String): DeletionResult = {
    val fileMetadatas = entityRepository.getByParameters(Map("filetype" -> FileMetadataLiterals.customJarType,
      "specification.name" -> name,
      "specification.version" -> version)
    )

    if (fileMetadatas.nonEmpty) {
      val filename = fileMetadatas.head.filename

      if (fileStorage.delete(filename)) {
        configRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, s"$name-$version"))
        Deleted
      } else DeletionError(s"Can't delete jar '$filename' for some reason. It needs to be debugged.")
    } else EntityNotFound
  }
}
