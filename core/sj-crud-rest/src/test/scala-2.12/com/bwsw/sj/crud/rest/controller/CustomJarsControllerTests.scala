package com.bwsw.sj.crud.rest.controller

import java.io.File
import java.nio.file.Paths

import akka.stream.scaladsl.FileIO
import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.CustomJarsSI
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

import scala.collection.mutable.ArrayBuffer

class CustomJarsControllerTests extends FlatSpec with Matchers with MockitoSugar {
  val entityDeletedByFilenameMessageName = "rest.custom.jars.file.deleted.by.filename"
  val entityDeletedMessageName = "rest.custom.jars.file.deleted"
  val entityNotFoundMessageName = "rest.custom.jars.file.notfound"
  val uploadedMessageName = "rest.custom.jars.file.uploaded"
  val notUploadedMessageName = "rest.custom.jars.cannot.upload"

  val jarSize = 1000l
  val uploadDate = "10.10.10"
  val description = "description"

  val messageResourceUtils = mock[MessageResourceUtils]
  val fileMetadataUtils = mock[FileMetadataUtils]

  val serviceInterface = mock[CustomJarsSI]
  when(serviceInterface.get(anyString())).thenReturn(None)
  when(serviceInterface.getBy(anyString(), anyString())).thenReturn(None)
  when(serviceInterface.delete(anyString())).thenReturn(EntityNotFound)
  when(serviceInterface.deleteBy(anyString(), anyString())).thenReturn(EntityNotFound)

  val jarsCount = 3
  val allJars = Range(0, jarsCount).map(i => createCustomJar(s"name$i", "version"))
  allJars.foreach {
    case JarInfo(_, metadata, name, version, jar) =>
      when(serviceInterface.get(jar.getName)).thenReturn(Some(metadata))
      when(serviceInterface.getBy(name, version)).thenReturn(Some(metadata))
      when(serviceInterface.delete(jar.getName)).thenReturn(Deleted)
      when(serviceInterface.deleteBy(name, version)).thenReturn(Deleted)
  }
  when(serviceInterface.getAll()).thenReturn(allJars.map(_.metadata).toBuffer)

  val newJarName = "new-jar-name"
  val newJarVersion = "new-jar-version"
  val newJar = createCustomJar(newJarName, newJarVersion)
  when(serviceInterface.create(newJar.metadata)).thenReturn(Created)

  val uploadedMessage = uploadedMessageName + "," + newJar.jar.getName
  when(messageResourceUtils.createMessage(uploadedMessageName, newJar.jar.getName)).thenReturn(uploadedMessage)

  val notValidJarName = "new-jar-name"
  val notValidJarVersion = "new-jar-version"
  val notValidJar = createCustomJar(notValidJarName, notValidJarVersion)
  val creationError = "not valid"
  when(serviceInterface.create(notValidJar.metadata)).thenReturn(NotCreated(ArrayBuffer(creationError)))

  val notUploadedMessage = notUploadedMessageName + "," + creationError
  when(messageResourceUtils.createMessageWithErrors(notUploadedMessageName, ArrayBuffer(creationError))).thenReturn(notUploadedMessage)

  val entityNotFoundMessage = entityNotFoundMessageName + "," + newJarName + "," + newJarVersion
  val entityNotFoundResponse = NotFoundRestResponse(MessageResponseEntity(entityNotFoundMessage))
  when(messageResourceUtils.createMessage(entityNotFoundMessageName, s"$newJarName-$newJarVersion"))
    .thenReturn(entityNotFoundMessage)

  val entityNotFoundByFilenameMessage = entityNotFoundMessageName + "," + newJar.jar.getName
  val entityNotFoundByFilenameResponse = NotFoundRestResponse(MessageResponseEntity(entityNotFoundByFilenameMessage))
  when(messageResourceUtils.createMessage(entityNotFoundMessageName, newJar.jar.getName))
    .thenReturn(entityNotFoundByFilenameMessage)

  val notDeletedJarName = "not-deleted-name"
  val notDeletedJarVersion = "not-deleted-version"
  val notDeletedFilename = s"$notDeletedJarName-$notDeletedJarVersion.jar"
  val deletionError = "jar not deleted"
  when(serviceInterface.delete(notDeletedFilename)).thenReturn(DeletionError(deletionError))
  when(serviceInterface.deleteBy(notDeletedJarName, notDeletedJarVersion)).thenReturn(DeletionError(deletionError))

  val injector = new Module {
    bind[JsonSerializer] to mock[JsonSerializer]
    bind[MessageResourceUtils] to messageResourceUtils
    bind[CustomJarsSI] to serviceInterface
    bind[JsonDeserializationErrorMessageCreator] to mock[JsonDeserializationErrorMessageCreator]
    bind[FileMetadataUtils] to fileMetadataUtils
  }.injector

  val controller = new CustomJarsController()(injector)


  // create
  "CustomJarsController" should "upload valid custom jar" in {
    val expected = OkRestResponse(MessageResponseEntity(uploadedMessage))
    controller.create(newJar.api) shouldBe expected
  }

  it should "not upload not valid custom jar" in {
    val expected = BadRequestRestResponse(MessageResponseEntity(notUploadedMessage))
    controller.create(notValidJar.api) shouldBe expected
  }

  // getAll
  it should "give all custom jars" in {
    val customFileInfoes = allJars.map {
      case JarInfo(_, metadata, name, version, _) =>
        val customJarInfo = CustomJarInfo(name, version, jarSize)
        when(fileMetadataUtils.toCustomJarInfo(metadata)).thenReturn(customJarInfo)
        customJarInfo
    }

    val expected = OkRestResponse(CustomJarsResponseEntity(customFileInfoes))
    controller.getAll() shouldBe expected
  }

  // get
  it should "give custom jar if it is exists" in {
    allJars.foreach {
      case JarInfo(_, _, _, _, file) =>
        val source = FileIO.fromPath(Paths.get(file.getAbsolutePath))
        when(fileMetadataUtils.fileToSource(file)).thenReturn(source)

        controller.get(file.getName) shouldBe CustomJar(file.getName, source)
    }
  }

  it should "not custom jar if it does not exists" in {
    controller.get(newJar.jar.getName) shouldBe entityNotFoundByFilenameResponse
  }

  // getBy
  it should "give custom jar by name and version if it is exists" in {
    allJars.foreach {
      case JarInfo(_, _, name, version, file) =>
        val source = FileIO.fromPath(Paths.get(file.getAbsolutePath))
        when(fileMetadataUtils.fileToSource(file)).thenReturn(source)

        controller.getBy(name, version) shouldBe CustomJar(file.getName, source)
    }
  }

  it should "not custom jar by name and version if it does not exists" in {
    controller.getBy(newJarName, newJarVersion) shouldBe entityNotFoundResponse
  }

  // delete
  it should "delete custom jar if it is exists" in {
    allJars.foreach {
      case JarInfo(_, _, _, _, file) =>
        val message = entityDeletedByFilenameMessageName + "," + file.getName
        when(messageResourceUtils.createMessage(entityDeletedByFilenameMessageName, file.getName)).thenReturn(message)

        controller.delete(file.getName) shouldBe OkRestResponse(MessageResponseEntity(message))
    }
  }

  it should "not delete custom jar if it does not exists" in {
    controller.delete(newJar.jar.getName) shouldBe entityNotFoundByFilenameResponse
  }

  it should "report in a case some deletion error (delete)" in {
    val expected = UnprocessableEntityRestResponse(MessageResponseEntity(deletionError))
    controller.delete(notDeletedFilename) shouldBe expected
  }

  // deleteBy
  it should "delete custom jar by name and version if it is exists" in {
    allJars.foreach {
      case JarInfo(_, _, name, version, _) =>
        val message = entityDeletedMessageName + "," + name + "," + version
        when(messageResourceUtils.createMessage(entityDeletedMessageName, name, version)).thenReturn(message)

        controller.deleteBy(name, version) shouldBe OkRestResponse(MessageResponseEntity(message))
    }
  }

  it should "not delete custom jar by name and version if it does not exists" in {
    controller.deleteBy(newJarName, newJarVersion) shouldBe entityNotFoundResponse
  }

  it should "report in a case some deletion error (deleteBy)" in {
    val expected = UnprocessableEntityRestResponse(MessageResponseEntity(deletionError))
    controller.deleteBy(notDeletedJarName, notDeletedJarVersion) shouldBe expected
  }


  def createCustomJar(name: String, version: String) = {
    val filename = s"$name-$version.jar"
    val jar = new File(filename)

    val metadata = mock[FileMetadata]
    when(metadata.name).thenReturn(Some(name))
    when(metadata.version).thenReturn(Some(version))
    when(metadata.filename).thenReturn(filename)
    when(metadata.file).thenReturn(Some(jar))
    when(metadata.length).thenReturn(Some(jarSize))
    when(metadata.uploadDate).thenReturn(Some(uploadDate))
    when(metadata.description).thenReturn(Some(description))

    val api = mock[FileMetadataApi]
    when(api.filename).thenReturn(Some(filename))
    when(api.file).thenReturn(Some(jar))
    when(api.to()(any[Injector]())).thenReturn(metadata)

    JarInfo(api, metadata, name, version, jar)
  }

  case class JarInfo(api: FileMetadataApi, metadata: FileMetadata, name: String, version: String, jar: File)

}
