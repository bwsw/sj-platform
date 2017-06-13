package com.bwsw.sj.crud.rest.controller

import java.io.File
import java.nio.file.Paths

import akka.stream.scaladsl.FileIO
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ModuleSI
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, Specification}
import com.bwsw.sj.common.si.result.{Created, Deleted, DeletionError, NotCreated}
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest.model.module.{CreateSpecificationApi, ModuleMetadataApi, SpecificationApi}
import com.bwsw.sj.crud.rest.utils.{FileMetadataUtils, JsonDeserializationErrorMessageCreator}
import com.bwsw.sj.crud.rest._
import org.mockito.Mockito.{reset, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import scaldi.Module

import scala.collection.mutable.ArrayBuffer

class ModuleControllerTests extends FlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach {
  val messageResourceUtils = mock[MessageResourceUtils]
  val jsonDeserializationErrorMessageCreator = mock[JsonDeserializationErrorMessageCreator]
  val createSpecificationApi = mock[CreateSpecificationApi]
  val fileMetadataUtils = mock[FileMetadataUtils]
  val serviceInterface = mock[ModuleSI]

  val newModuleFilename = "new-module.jar"
  val newModuleApi = mock[ModuleMetadataApi]
  val newModule = mock[ModuleMetadata]

  val notUploadedMessageName = "rest.modules.module.cannot.upload"
  val uploadingError = "not valid"
  val notUploadedMessage = notUploadedMessageName + "," + newModuleFilename + "," + uploadingError
  when(messageResourceUtils.createMessage(notUploadedMessageName, newModuleFilename, uploadingError))
    .thenReturn(notUploadedMessage)

  val existingModuleType = inputStreamingType
  val existingModuleName = "existing-module-name"
  val existingModuleVersion = "existing-module-version"
  val existingModuleFilename = s"$existingModuleName-$existingModuleVersion.jar"
  val existingModuleFile = new File(existingModuleFilename)
  val existingModule = mock[ModuleMetadata]
  when(serviceInterface.get(existingModuleType, existingModuleName, existingModuleVersion))
    .thenReturn(Right(existingModule))
  when(serviceInterface.getMetadataWithoutFile(existingModuleType, existingModuleName, existingModuleVersion))
    .thenReturn(Right(existingModule))

  val wrongType = "wrong-type"
  val wrongName = "wrong-name"
  val wrongVersion = "wrong-version"
  val moduleNotFoundError = "module not found"
  when(serviceInterface.get(wrongType, wrongName, wrongVersion)).thenReturn(Left(moduleNotFoundError))
  when(serviceInterface.getMetadataWithoutFile(wrongType, wrongName, wrongVersion))
    .thenReturn(Left(moduleNotFoundError))

  val injector = new Module {
    bind[MessageResourceUtils] to messageResourceUtils
    bind[JsonDeserializationErrorMessageCreator] to jsonDeserializationErrorMessageCreator
    bind[ModuleSI] to serviceInterface
    bind[CreateSpecificationApi] to createSpecificationApi
    bind[FileMetadataUtils] to fileMetadataUtils
  }

  val controller = new ModuleController()(injector)

  override def beforeEach(): Unit = {
    when(newModuleApi.filename).thenReturn(Some(newModuleFilename))
    when(newModule.filename).thenReturn(newModuleFilename)

    when(existingModule.filename).thenReturn(existingModuleFilename)
    when(existingModule.file).thenReturn(Some(existingModuleFile))
  }

  override def afterEach(): Unit = {
    reset(newModuleApi, newModule, existingModule)
  }

  // create
  "ModuleController" should "upload correct module" in {
    when(newModuleApi.validate).thenReturn(ArrayBuffer[String]())
    when(newModuleApi.to()).thenReturn(newModule)
    when(serviceInterface.create(newModule)).thenReturn(Created)
    val uploadedMessageName = "rest.modules.module.uploaded"
    val uploadedMessage = uploadedMessageName + "," + newModuleFilename
    when(messageResourceUtils.createMessage(uploadedMessageName, newModuleFilename)).thenReturn(uploadedMessage)

    val expectedResponse = OkRestResponse(MessageResponseEntity(uploadedMessage))

    controller.create(newModuleApi) shouldBe expectedResponse
  }

  it should "not upload module with incorrect metadata" in {
    when(newModuleApi.validate).thenReturn(ArrayBuffer[String]())
    when(newModuleApi.to()).thenReturn(newModule)
    when(serviceInterface.create(newModule)).thenReturn(NotCreated(ArrayBuffer[String](uploadingError)))

    val expectedResponse = BadRequestRestResponse(MessageResponseEntity(notUploadedMessage))

    controller.create(newModuleApi) shouldBe expectedResponse
  }

  it should "not upload module with incorrect specification.json" in {
    when(newModuleApi.validate).thenReturn(ArrayBuffer[String]())
    val jsonDeserializationMessage = "incorrect json"
    val incorrectSpecificationException = new JsonDeserializationException(jsonDeserializationMessage)
    when(newModuleApi.to()).thenAnswer(_ => throw incorrectSpecificationException)
    when(jsonDeserializationErrorMessageCreator.apply(incorrectSpecificationException))
      .thenReturn(uploadingError)

    val expectedResponse = BadRequestRestResponse(MessageResponseEntity(notUploadedMessage))

    controller.create(newModuleApi) shouldBe expectedResponse
  }

  it should "not upload module with incorrect ModuleMetadataApi" in {
    when(newModuleApi.validate).thenReturn(ArrayBuffer[String](uploadingError))

    val expectedResponse = BadRequestRestResponse(MessageResponseEntity(notUploadedMessage))

    controller.create(newModuleApi) shouldBe expectedResponse
  }

  // get
  it should "give module if it exist" in {
    val source = FileIO.fromPath(Paths.get(existingModuleFile.getAbsolutePath))
    val expectedResponse = ModuleJar(existingModuleFilename, source)
    when(fileMetadataUtils.fileToSource(existingModuleFile)).thenReturn(source)

    controller.get(existingModuleType, existingModuleName, existingModuleVersion) shouldBe expectedResponse
  }

  it should "not give module if it does not exist" in {
    val expectedResponse = NotFoundRestResponse(MessageResponseEntity(moduleNotFoundError))

    controller.get(wrongType, wrongName, wrongVersion) shouldBe expectedResponse
  }

  // getAll
  it should "give all modules" in {
    val modulesCount = 5
    val allModules = Range(0, modulesCount).map(i => createModule(s"name$i", batchStreamingType))
    when(serviceInterface.getAll).thenReturn(allModules.map(_._2))

    val expectedResponse = OkRestResponse(ModulesResponseEntity(allModules.map(_._1)))

    controller.getAll shouldBe expectedResponse
  }

  // getByType
  it should "give all modules with specific type" in {
    val inputModulesCount = 3
    val inputModules = Range(0, inputModulesCount).map(i => createModule(s"name$i", inputStreamingType))
    val regularModulesCount = 3
    val regularModules = Range(0, regularModulesCount).map(i => createModule(s"name$i", regularStreamingType))

    Map(
      inputStreamingType -> inputModules,
      regularStreamingType -> regularModules,
      outputStreamingType -> Seq.empty,
      batchStreamingType -> Seq.empty).foreach {
      case (moduleType, modules) =>
        when(serviceInterface.getByType(moduleType)).thenReturn(Right(modules.map(_._2)))

        val expectedResponse = OkRestResponse(ModulesResponseEntity(modules.map(_._1)))

        controller.getByType(moduleType) shouldBe expectedResponse
    }
  }

  it should "not give modules if module type is incorrect" in {
    val unknownTypeError = "unknown type"
    when(serviceInterface.getByType(wrongType)).thenReturn(Left(unknownTypeError))

    val expectedResponse = BadRequestRestResponse(MessageResponseEntity(unknownTypeError))

    controller.getByType(wrongType) shouldBe expectedResponse
  }

  // getAllTypes
  it should "give all module types" in {
    val expectedResponse = OkRestResponse(TypesResponseEntity(moduleTypes))

    controller.getAllTypes shouldBe expectedResponse
  }

  // getSpecification
  it should "give module specification" in {
    val specification = mock[Specification]
    val specificationApi = mock[SpecificationApi]
    when(existingModule.specification).thenReturn(specification)
    when(createSpecificationApi.from(specification)).thenReturn(specificationApi)

    val expectedResponse = OkRestResponse(SpecificationResponseEntity(specificationApi))

    controller.getSpecification(existingModuleType, existingModuleName, existingModuleVersion) shouldBe expectedResponse
  }

  it should "not give module specification if module does not exist" in {
    val expectedResponse = NotFoundRestResponse(MessageResponseEntity(moduleNotFoundError))

    controller.get(wrongType, wrongName, wrongVersion) shouldBe expectedResponse
  }

  // delete
  it should "delete module if it exists" in {
    val moduleSignature = s"$existingModuleType-$existingModuleName-$existingModuleVersion"
    when(existingModule.signature).thenReturn(moduleSignature)
    when(serviceInterface.delete(existingModule)).thenReturn(Deleted)
    val deletedMessageName = "rest.modules.module.deleted"
    val deletedMessage = deletedMessageName + "," + moduleSignature
    when(messageResourceUtils.createMessage(deletedMessageName, moduleSignature)).thenReturn(deletedMessage)

    val expectedResponse = OkRestResponse(MessageResponseEntity(deletedMessage))

    controller.delete(existingModuleType, existingModuleName, existingModuleVersion) shouldBe expectedResponse
  }

  it should "not delete module if it does not exists" in {
    val expectedResponse = NotFoundRestResponse(MessageResponseEntity(moduleNotFoundError))

    controller.delete(wrongType, wrongName, wrongVersion) shouldBe expectedResponse
  }

  it should "report in a case some deletion error" in {
    val moduleType = batchStreamingType
    val moduleName = "not-deleted-module-name"
    val moduleVersion = "not-deleted-module-version"
    val module = mock[ModuleMetadata]
    when(serviceInterface.get(moduleType, moduleName, moduleVersion))
      .thenReturn(Right(module))
    val deletionError = "module not deleted"
    when(serviceInterface.delete(module)).thenReturn(DeletionError(deletionError))

    val expectedResponse = UnprocessableEntityRestResponse(MessageResponseEntity(deletionError))

    controller.delete(moduleType, moduleName, moduleVersion) shouldBe expectedResponse
  }

  // getRelated
  it should "give instances that related with module" in {
    val relatedInstances = Seq("instance1", "instance2", "instance3")
    when(serviceInterface.getRelatedInstances(existingModule)).thenReturn(relatedInstances)

    val expectedResponse = OkRestResponse(RelatedToModuleResponseEntity(relatedInstances))

    controller.getRelated(existingModuleType, existingModuleName, existingModuleVersion) shouldBe expectedResponse
  }

  it should "not give related instances if module does not exists" in {
    val expectedResponse = NotFoundRestResponse(MessageResponseEntity(moduleNotFoundError))

    controller.delete(wrongType, wrongName, wrongVersion) shouldBe expectedResponse
  }

  private def createModule(name: String, moduleType: String): (ModuleInfo, ModuleMetadata) = {
    val module = mock[ModuleMetadata]
    val version = "version"
    val moduleSize = 1000l
    val moduleInfo = ModuleInfo(moduleType, name, version, moduleSize)

    when(fileMetadataUtils.toModuleInfo(module)).thenReturn(moduleInfo)

    (moduleInfo, module)
  }

}
