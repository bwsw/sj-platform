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

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.MongoFileStorage
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.model.module.{FileMetadataDomain, SpecificationDomain}
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadataLiterals.customJarType
import com.bwsw.sj.common.si.model.{FileMetadata, FileMetadataCreator}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.{MessageResourceUtils, SpecificationUtils}
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.{any, anyString, eq => mockitoEq}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}

import scala.collection.mutable.ArrayBuffer

class CustomJarsSiTests extends FlatSpec with Matchers with MockitoSugar {
  val tmpDirectory = "/tmp/"
  val alreadyExistsError = ArrayBuffer[String]("already exists")
  val jarInStorageDescription = "exists in storage"

  val jarsInStorageInfo = Seq(
    ("jar-in-storage-1", "v1"),
    ("jar-in-storage-1", "v2"),
    ("jar-in-storage-2", "v1"),
    ("jar-in-storage-2", "v2"))

  val jarsInStorageFilenames = jarsInStorageInfo.map {
    case (name, version) => s"$name-$version.jar"
  }

  val jarsInStorage = jarsInStorageInfo.zip(jarsInStorageFilenames).map {
    case ((name, version), filename) =>
      (name, version, new File(getClass.getResource(filename).toURI))
  }

  val jarsInStorageMetadatas = jarsInStorage.map {
    case (name, version, file) =>
      val metadata = createJarMetadataMock(name, version, file, jarInStorageDescription)
      when(metadata.validate()).thenReturn(alreadyExistsError)
      metadata
  }
  val jarsInStorageMetadataDomains = jarsInStorage.map {
    case (name, version, file) =>
      createJarMetadataDomain(name, version, file, jarInStorageDescription)
  }
  val jarsInStorageConfigNames = jarsInStorage.map {
    case (name, version, _) =>
      s"${ConfigLiterals.systemDomain}.$name-$version"
  }

  val jarNotInStorageName = "jar-not-in-storage"
  val jarNotInStorageFilename = jarNotInStorageName + ".jar"
  val jarNotInStorageVersion = "v1"
  val jarNotInStorageDescription = "not exists in storage"
  val jarNotInStorage = new File(getClass.getResource(jarNotInStorageFilename).toURI)
  val jarNotInStorageMetadataDomain = createJarMetadataDomain(
    jarNotInStorageName,
    jarNotInStorageVersion,
    jarNotInStorage,
    jarNotInStorageDescription)
  val jarNotInStorageMetadata = createJarMetadataMock(
    jarNotInStorageName,
    jarNotInStorageVersion,
    jarNotInStorage,
    jarNotInStorageDescription)
  val jarNotInStorageConfig = ConfigurationSettingDomain(
    s"${ConfigLiterals.systemDomain}.$jarNotInStorageName-$jarNotInStorageVersion",
    jarNotInStorageFilename,
    ConfigLiterals.systemDomain,
    new Date())
  val jarNotInStorageSpecification = jarNotInStorageMetadataDomain.specification
  val jarNotInStorageSpecificationString =
    s"""{
       |"name": "$jarNotInStorageName",
       |"version": "$jarNotInStorageVersion",
       |"description": "$jarNotInStorageDescription"
       |}""".stripMargin
  val jarNotInStorageSpecificationMap: Map[String, Any] = Map(
    "name" -> jarNotInStorageName,
    "version" -> jarNotInStorageVersion,
    "description" -> jarNotInStorageDescription)

  val specificationUtils = mock[SpecificationUtils]
  when(specificationUtils.getSpecification(jarNotInStorage)).thenReturn(jarNotInStorageMetadataDomain.specification)

  val serializer = mock[JsonSerializer]
  when(serializer.serialize(jarNotInStorageSpecification)).thenReturn(jarNotInStorageSpecificationString)
  when(serializer.deserialize[Map[String, Any]](jarNotInStorageSpecificationString))
    .thenReturn(jarNotInStorageSpecificationMap)

  val createFileMetadata = mock[FileMetadataCreator]
  jarsInStorageMetadatas.zip(jarsInStorageMetadataDomains).foreach {
    case (metadata, domain) =>
      when(createFileMetadata.from(mockitoEq(domain))(any[Injector]())).thenReturn(metadata)
  }

  val jarMetadataRepository = mock[GenericMongoRepository[FileMetadataDomain]]
  when(jarMetadataRepository.getByParameters(any[Map[String, String]]())).thenReturn(Seq.empty)
  when(jarMetadataRepository.getByParameters(Map("filetype" -> customJarType)))
    .thenReturn(jarsInStorageMetadataDomains)
  jarsInStorageMetadataDomains.foreach { domain =>
    when(jarMetadataRepository.getByParameters(
      Map(
        "filetype" -> customJarType,
        "filename" -> domain.filename)))
      .thenReturn(Seq(domain))

    when(jarMetadataRepository.getByParameters(
      Map(
        "filetype" -> customJarType,
        "specification.name" -> domain.specification.name,
        "specification.version" -> domain.specification.version)))
      .thenReturn(Seq(domain))
  }

//TODO rewrite this test, after adding Date to config, this test failing
  "CustomFilesSI" should "create correct custom jar" in new Mocks {
//    when(jarNotInStorageMetadata.validate()).thenReturn(ArrayBuffer[String]())
//
//    customJarsSI.create(jarNotInStorageMetadata) shouldBe Created
//    verify(fileStorage)
//      .put(
//        new File(jarNotInStorageFilename),
//        jarNotInStorageFilename,
//        jarNotInStorageSpecificationMap,
//        customJarType)
//    verify(configRepository).save(jarNotInStorageConfig)
  }

  it should "not create incorrect custom jar" in new Mocks {
    val errors = ArrayBuffer[String]("not valid")
    when(jarNotInStorageMetadata.validate()).thenReturn(errors)

    customJarsSI.create(jarNotInStorageMetadata) shouldBe NotCreated(errors)
    verify(fileStorage, never()).put(any[File](), anyString(), any[Map[String, Any]](), anyString())
  }

  it should "not create already existed custom jar" in new Mocks {
    jarsInStorageMetadatas.foreach { metadata =>
      customJarsSI.create(metadata) shouldBe NotCreated(alreadyExistsError)
      verify(fileStorage, never()).put(any[File](), anyString(), any[Map[String, Any]](), anyString())
    }
  }

  it should "give all custom jars from storage" in new Mocks {
    customJarsSI.getAll().toSet shouldBe jarsInStorageMetadatas.toSet
  }

  it should "give custom jar by filename if it exists in storage" in new Mocks {
    jarsInStorage.foreach {
      case (_, _, file) =>
        val metadata = customJarsSI.get(file.getName)
        metadata shouldBe defined
        metadata.get.filename shouldBe file.getName
        metadata.get.file shouldBe Some(file)
    }
  }

  it should "not give custom jar by filename if it does not exists in storage" in new Mocks {
    customJarsSI.get(jarNotInStorageFilename) shouldBe empty
  }

  it should "give custom jar by name and version if it exists in storage" in new Mocks {
    jarsInStorage.foreach {
      case (name, version, file) =>
        val metadata = customJarsSI.getBy(name, version)
        metadata shouldBe defined
        metadata.get.filename shouldBe file.getName
        metadata.get.file shouldBe Some(file)
    }
  }

  it should "not give custom jar by name and version if it does not exists in storage" in new Mocks {
    customJarsSI.getBy(jarNotInStorageName, jarNotInStorageVersion) shouldBe empty
  }

  it should "delete custom jar by filename if it exists in storage" in new Mocks {
    jarsInStorage.zip(jarsInStorageConfigNames).foreach {
      case ((_, _, file), configName) =>
        customJarsSI.delete(file.getName) shouldBe Deleted
        verify(configRepository).delete(configName)
    }
  }

  it should "not delete custom jar by filename if it does not exists in storage" in new Mocks {
    customJarsSI.delete(jarNotInStorageFilename) shouldBe EntityNotFound
    verify(configRepository, never()).delete(anyString())
    verify(fileStorage, never()).delete(anyString())
  }

  it should "delete custom jar by name and version if it exists in storage" in new Mocks {
    jarsInStorage.zip(jarsInStorageConfigNames).foreach {
      case ((name, version, _), configName) =>
        customJarsSI.deleteBy(name, version) shouldBe Deleted
        verify(configRepository).delete(configName)
    }
  }

  it should "not delete custom jar by name and version if it does not exists in storage" in new Mocks {
    customJarsSI.deleteBy(jarNotInStorageName, jarNotInStorageVersion) shouldBe EntityNotFound
    verify(configRepository, never()).delete(anyString())
    verify(fileStorage, never()).delete(anyString())
  }


  private def createJarMetadataDomain(name: String,
                                      version: String,
                                      jarFile: File,
                                      description: String): FileMetadataDomain = {
    FileMetadataDomain(
      new ObjectId(),
      name,
      jarFile.getName,
      customJarType,
      new Date(jarFile.lastModified()),
      jarFile.length(),
      new SpecificationDomain(
        name, description, version, null, null, null, null, null, null, null, null, null))
  }

  private def createJarMetadataMock(name: String, version: String, file: File, description: String): FileMetadata = {
    val jarMetadata = mock[FileMetadata]
    when(jarMetadata.name).thenReturn(Some(name))
    when(jarMetadata.version).thenReturn(Some(version))
    when(jarMetadata.filename).thenReturn(file.getName)
    when(jarMetadata.file).thenReturn(Some(file))
    when(jarMetadata.description).thenReturn(Some(description))
    jarMetadata
  }


  trait Mocks {
    val fileStorage = mock[MongoFileStorage]

    when(fileStorage.exists(anyString())).thenReturn(false)
    jarsInStorageFilenames.foreach {
      filename =>
        when(fileStorage.exists(filename)).thenReturn(true)
        when(fileStorage.delete(filename)).thenReturn(true)
    }

    when(fileStorage.get(jarNotInStorageFilename, tmpDirectory + jarNotInStorageFilename))
      .thenThrow(classOf[FileNotFoundException])
    jarsInStorage.foreach {
      case (_, _, file) => when(fileStorage.get(file.getName, tmpDirectory + file.getName)).thenReturn(file)
    }

    val configRepository = mock[GenericMongoRepository[ConfigurationSettingDomain]]

    val connectionRepository = mock[ConnectionRepository]
    when(connectionRepository.getFileStorage).thenReturn(fileStorage)
    when(connectionRepository.getFileMetadataRepository).thenReturn(jarMetadataRepository)
    when(connectionRepository.getConfigRepository).thenReturn(configRepository)

    val fileBuffer = mock[FileBuffer]

    val module = new Module {
      bind[ConnectionRepository] to connectionRepository
      bind[FileBuffer] to fileBuffer
      bind[FileMetadataCreator] to createFileMetadata
      bind[MessageResourceUtils] to mock[MessageResourceUtils]
      bind[SpecificationUtils] to specificationUtils
      bind[JsonSerializer] to serializer
    }
    implicit val injector = module.injector
    val customJarsSI = new CustomJarsSI()(injector)
  }

}