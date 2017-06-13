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

import java.io.File

import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.{FileMetadata, CreateFileMetadata, FileMetadataLiterals}
import com.bwsw.sj.common.si.result._
import org.apache.commons.io.FileUtils
import scaldi.Injectable.inject
import scaldi.Injector

/**
  * Provides methods to access custom files represented by [[FileMetadata]] in [[GenericMongoRepository]]
  */
class CustomFilesSI(implicit injector: Injector) extends ServiceInterface[FileMetadata, FileMetadataDomain] {
  private val connectionRepository = inject[ConnectionRepository]
  override protected val entityRepository: GenericMongoRepository[FileMetadataDomain] = connectionRepository.getFileMetadataRepository

  private val fileStorage = connectionRepository.getFileStorage
  private val tmpDirectory = "/tmp/"
  private val fileBuffer = inject[FileBuffer]

  override def create(entity: FileMetadata): CreationResult = {
    if (!fileStorage.exists(entity.filename)) {
      val uploadingFile = new File(entity.filename)
      FileUtils.copyFile(entity.file.get, uploadingFile)
      fileStorage.put(uploadingFile, entity.filename, Map("description" -> entity.description), FileMetadataLiterals.customFileType)
      uploadingFile.delete()

      Created
    } else {
      NotCreated()
    }
  }

  override def getAll(): Seq[FileMetadata] = {
    entityRepository.getByParameters(Map("filetype" -> FileMetadataLiterals.customFileType))
      .map(x => inject[CreateFileMetadata].from(x))
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
    val fileMetadatas = entityRepository.getByParameters(Map("filename" -> name))

    if (fileMetadatas.isEmpty)
      EntityNotFound
    else {
      if (fileStorage.delete(name))
        Deleted
      else
        DeletionError(s"Can't delete jar '$name' for some reason. It needs to be debugged.")
    }
  }
}
