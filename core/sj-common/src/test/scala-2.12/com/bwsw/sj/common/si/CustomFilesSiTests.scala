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

import java.io.{File, FileNotFoundException}
import java.util.Date

import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.dal.model.module.{FileMetadataDomain, SpecificationDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadataLiterals.customFileType
import com.bwsw.sj.common.si.model.{FileMetadata, FileMetadataCreator}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.{any, anyString, eq => mockitoEq}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}

class CustomFilesSiTests extends FlatSpec with Matchers with MockitoSugar {
  val tmpDirectory = "/tmp/"

  val fileInStorageDescription = "exists in storage"

  val filesInStorageNames = Seq("file-in-storage-1", "file-in-storage-2")

  val filesInStorage = filesInStorageNames.map(filename => (filename, new File(getClass.getResource(filename).toURI)))
  val filesInStorageMetadatas = filesInStorage.map {
    case (filename, file) => createFileMetadata(filename, file, fileInStorageDescription)
  }
  val filesInStorageMetadataDomains = filesInStorage.map {
    case (filename, file) => createFileMetadataDomain(filename, file, fileInStorageDescription)
  }

  val fileNotInStorageName = "file-not-in-storage"
  val fileNotInStorageDescription = "not exists in storage"
  val fileNotInStorage = new File(getClass.getResource(fileNotInStorageName).toURI)
  val fileNotInStorageMetadataDomain = createFileMetadataDomain(
    fileNotInStorageName,
    fileNotInStorage,
    fileNotInStorageDescription)
  val fileNotInStorageMetadata = createFileMetadata(
    fileNotInStorageName,
    fileNotInStorage,
    fileNotInStorageDescription)

  val fileMetadataCreator = mock[FileMetadataCreator]
  filesInStorageMetadatas.zip(filesInStorageMetadataDomains).foreach {
    case (metadata, domain) =>
      when(fileMetadataCreator.from(mockitoEq(domain))(any[Injector]())).thenReturn(metadata)
  }

  val fileMetadataRepository = mock[GenericMongoRepository[FileMetadataDomain]]
  when(fileMetadataRepository.getByParameters(any[Map[String, String]]())).thenReturn(Seq.empty)
  when(fileMetadataRepository.getByParameters(Map("filetype" -> customFileType)))
    .thenReturn(filesInStorageMetadataDomains)
  filesInStorageMetadataDomains.foreach { domain =>
    when(fileMetadataRepository.getByParameters(Map("filename" -> domain.filename))).thenReturn(Seq(domain))
  }


  "CustomFilesSI" should "create file if it not exists in storage" in new Mocks {
    customFilesSI.create(fileNotInStorageMetadata) shouldBe Created
    verify(fileStorage)
      .put(
        new File(fileNotInStorageName),
        fileNotInStorageName,
        Map("description" -> Some(fileNotInStorageDescription)),
        customFileType)
  }

  it should "not create already existed file" in new Mocks {
    filesInStorageMetadatas.foreach { metadata =>
      customFilesSI.create(metadata) shouldBe NotCreated()
      verify(fileStorage, never()).put(any[File](), anyString(), any[Map[String, Any]](), anyString())
    }
  }

  it should "give all files from storage" in new Mocks {
    customFilesSI.getAll().toSet shouldBe filesInStorageMetadatas.toSet
  }

  it should "give file if it exists in storage" in new Mocks {
    filesInStorage.foreach {
      case (filename, file) =>
        val metadata = customFilesSI.get(filename)
        metadata shouldBe defined
        metadata.get.filename shouldBe filename
        metadata.get.file shouldBe Some(file)
    }
  }

  it should "not give file if it does not exists in storage" in new Mocks {
    customFilesSI.get(fileNotInStorageName) shouldBe empty
  }

  it should "delete file if it exists in storage" in new Mocks {
    filesInStorageNames.foreach { filename =>
      customFilesSI.delete(filename) shouldBe Deleted
    }
  }

  it should "not delete file if it does not exists in storage" in new Mocks {
    customFilesSI.delete(fileNotInStorageName) shouldBe EntityNotFound
  }


  private def createFileMetadataDomain(name: String, file: File, description: String) = {
    FileMetadataDomain(
      new ObjectId(),
      name,
      file.getName,
      customFileType,
      new Date(file.lastModified()),
      file.length(),
      new SpecificationDomain(name, description, null, null, null, null, null, null, null, null, null, null))
  }

  private def createFileMetadata(name: String, file: File, description: String) = {
    val fileMetadata = mock[FileMetadata]
    when(fileMetadata.filename).thenReturn(name)
    when(fileMetadata.file).thenReturn(Some(file))
    when(fileMetadata.description).thenReturn(Some(description))
    fileMetadata
  }


  trait Mocks {
    val fileStorage = mock[MongoFileStorage]

    when(fileStorage.exists(anyString())).thenReturn(false)
    filesInStorageNames.foreach {
      filename =>
        when(fileStorage.exists(filename)).thenReturn(true)
        when(fileStorage.delete(filename)).thenReturn(true)
    }

    when(fileStorage.get(fileNotInStorageName, tmpDirectory + fileNotInStorageName))
      .thenThrow(classOf[FileNotFoundException])
    filesInStorage.foreach {
      case (filename, file) => when(fileStorage.get(filename, tmpDirectory + filename)).thenReturn(file)
    }

    val connectionRepository = mock[ConnectionRepository]
    when(connectionRepository.getFileStorage).thenReturn(fileStorage)
    when(connectionRepository.getFileMetadataRepository).thenReturn(fileMetadataRepository)

    val fileBuffer = mock[FileBuffer]

    val module = new Module {
      bind[ConnectionRepository] to connectionRepository
      bind[FileBuffer] to fileBuffer
      bind[FileMetadataCreator] to fileMetadataCreator
      bind[MessageResourceUtils] to mock[MessageResourceUtils]
    }
    implicit val injector = module.injector
    val customFilesSI = new CustomFilesSI()(injector)
  }

}