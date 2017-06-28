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
package com.bwsw.sj.crud.rest.controller

import java.io.File
import java.nio.file.Paths

import akka.stream.scaladsl.FileIO
import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.CustomFilesSI
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import com.bwsw.sj.crud.rest.utils.{FileMetadataUtils, JsonDeserializationErrorMessageCreator}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}

class CustomFilesControllerTests extends FlatSpec with Matchers with MockitoSugar {
  val entityDeletedMessageName = "rest.custom.files.file.deleted"
  val entityNotFoundMessageName = "rest.custom.files.file.notfound"
  val uploadedMessageName = "rest.custom.files.file.uploaded"
  val alreadyExistsMessageName = "rest.custom.files.file.exists"

  val fileSize = 1000l
  val uploadDate = "10.10.10"
  val description = "description"

  val messageResourceUtils = mock[MessageResourceUtils]
  val fileMetadataUtils = mock[FileMetadataUtils]

  val serviceInterface = mock[CustomFilesSI]
  when(serviceInterface.get(anyString())).thenReturn(None)
  when(serviceInterface.delete(anyString())).thenReturn(EntityNotFound)

  val filesCount = 3
  val allFiles = Range(0, filesCount).map(i => createCustomFile(s"file$i"))
  allFiles.foreach {
    case FileInfo(_, metadata, name, _) =>
      when(serviceInterface.create(metadata)).thenReturn(NotCreated())
      when(serviceInterface.get(name)).thenReturn(Some(metadata))
      when(serviceInterface.delete(name)).thenReturn(Deleted)
  }
  when(serviceInterface.getAll()).thenReturn(allFiles.map(_.metadata).toBuffer)

  val newFileName = "new-file"
  val newFile = createCustomFile(newFileName)

  when(serviceInterface.create(newFile.metadata)).thenReturn(Created)

  val uploadedMessage = uploadedMessageName + "," + newFileName
  when(messageResourceUtils.createMessage(uploadedMessageName, newFileName)).thenReturn(uploadedMessage)

  val entityNotFoundMessage = entityNotFoundMessageName + "," + newFileName
  val entityNotFoundResponse = NotFoundRestResponse(MessageResponseEntity(entityNotFoundMessage))
  when(messageResourceUtils.createMessage(entityNotFoundMessageName, newFileName))
    .thenReturn(entityNotFoundMessage)

  val missedFilenameApi = mock[FileMetadataApi]
  when(missedFilenameApi.filename).thenReturn(None)
  when(missedFilenameApi.file).thenReturn(None)
  val missedFilenameMessage = "rest.custom.files.file.missing"
  when(messageResourceUtils.getMessage(missedFilenameMessage)).thenReturn(missedFilenameMessage)

  val notDeletedFileName = "not-deleted-file"
  val notDeletedFile = createCustomFile(notDeletedFileName)
  val deletionError = "file-not-deleted"
  when(serviceInterface.delete(notDeletedFileName)).thenReturn(DeletionError(deletionError))

  val injector = new Module {
    bind[JsonSerializer] to mock[JsonSerializer]
    bind[MessageResourceUtils] to messageResourceUtils
    bind[CustomFilesSI] to serviceInterface
    bind[JsonDeserializationErrorMessageCreator] to mock[JsonDeserializationErrorMessageCreator]
    bind[FileMetadataUtils] to fileMetadataUtils
  }.injector

  val controller = new CustomFilesController()(injector)


  // create
  "CustomFilesController" should "upload file if it does not exists in storage" in {
    val expected = OkRestResponse(MessageResponseEntity(uploadedMessage))
    controller.create(newFile.api) shouldBe expected
  }

  it should "not upload file if it is already exists in storage" in {
    allFiles.foreach {
      case FileInfo(api, _, name, _) =>
        val message = alreadyExistsMessageName + "," + name
        when(messageResourceUtils.createMessage(alreadyExistsMessageName, name)).thenReturn(message)

        controller.create(api) shouldBe ConflictRestResponse(MessageResponseEntity(message))
    }
  }

  it should "not upload file if it does not have a name" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(missedFilenameMessage))
    controller.create(missedFilenameApi) shouldBe expected
  }

  // getAll
  it should "give all files" in {
    val customFileInfoes = allFiles.map {
      case FileInfo(_, metadata, name, _) =>
        val customFileInfo = CustomFileInfo(name, description, uploadDate, fileSize)
        when(fileMetadataUtils.toCustomFileInfo(metadata)).thenReturn(customFileInfo)
        customFileInfo
    }

    val expected = OkRestResponse(CustomFilesResponseEntity(customFileInfoes))
    controller.getAll() shouldBe expected
  }

  // get
  it should "give file if it is exists" in {
    allFiles.foreach {
      case FileInfo(_, _, name, file) =>
        val source = FileIO.fromPath(Paths.get(file.getAbsolutePath))
        when(fileMetadataUtils.fileToSource(file)).thenReturn(source)

        controller.get(name) shouldBe CustomFile(name, source)
    }
  }

  it should "not give file if it does not exists" in {
    controller.get(newFileName) shouldBe entityNotFoundResponse
  }

  // delete
  it should "delete file if it is exists" in {
    allFiles.foreach {
      case FileInfo(_, _, name, _) =>
        val message = entityDeletedMessageName + "," + name
        when(messageResourceUtils.createMessage(entityDeletedMessageName, name)).thenReturn(message)

        controller.delete(name) shouldBe OkRestResponse(MessageResponseEntity(message))
    }
  }

  it should "not delete file if it does not exists" in {
    controller.delete(newFileName) shouldBe entityNotFoundResponse
  }

  it should "report in a case some deletion error" in {
    val expected = UnprocessableEntityRestResponse(MessageResponseEntity(deletionError))
    controller.delete(notDeletedFileName) shouldBe expected
  }


  def createCustomFile(filename: String) = {
    val file = new File(filename)

    val metadata = mock[FileMetadata]
    when(metadata.filename).thenReturn(filename)
    when(metadata.file).thenReturn(Some(file))
    when(metadata.length).thenReturn(Some(fileSize))
    when(metadata.uploadDate).thenReturn(Some(uploadDate))
    when(metadata.description).thenReturn(Some(description))

    val api = mock[FileMetadataApi]
    when(api.filename).thenReturn(Some(filename))
    when(api.file).thenReturn(Some(file))
    when(api.to()).thenReturn(metadata)
    when(api.customFileParts).thenReturn(Map[String, Any](
      "file" -> file,
      "description" -> description))

    FileInfo(api, metadata, filename, file)
  }

  case class FileInfo(api: FileMetadataApi, metadata: FileMetadata, name: String, file: File)

}
