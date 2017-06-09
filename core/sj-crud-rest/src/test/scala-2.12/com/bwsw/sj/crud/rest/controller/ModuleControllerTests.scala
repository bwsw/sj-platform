package com.bwsw.sj.crud.rest.controller

import java.io.File
import java.nio.file.Paths

import akka.stream.scaladsl.FileIO
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.ModuleSI
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, Specification}
import com.bwsw.sj.common.si.result.{Created, NotCreated}
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest.model.module.{CreateSpecificationApi, ModuleMetadataApi, SpecificationApi}
import com.bwsw.sj.crud.rest.utils.{FileMetadataUtils, JsonDeserializationErrorMessageCreator}
import com.bwsw.sj.crud.rest.{ModuleInfo, ModuleJar, ModulesResponseEntity, SpecificationResponseEntity}
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

  val existenceModuleType = inputStreamingType
  val existenceModuleName = "existence-module-name"
  val existenceModuleVersion = "existence-module-version"
  val existenceModuleFilename = s"$existenceModuleName-$existenceModuleVersion.jar"
  val existenceModuleFile = new File(existenceModuleFilename)
  val existenceModule = mock[ModuleMetadata]
  when(serviceInterface.get(existenceModuleType, existenceModuleName, existenceModuleVersion))
    .thenReturn(Right(existenceModule))
  when(serviceInterface.getMetadataWithoutFile(existenceModuleType, existenceModuleName, existenceModuleVersion))
    .thenReturn(Right(existenceModule))

  val wrongType = "wrong-type"
  val wrongName = "wrong-name"
  val wrongVersion = "wrong-version"
  val moduleNotFoundError = "module not found"
  when(serviceInterface.get(wrongType, wrongName, wrongVersion)).thenReturn(Left(moduleNotFoundError))
  when(serviceInterface.getMetadataWithoutFile(wrongType, wrongName, wrongVersion))
    .thenReturn(Left(moduleNotFoundError))

  val moduleSize = 1000l
  val inputModulesCount = 3
  val inputModules = Range(0, inputModulesCount).map(i => createModule(s"name$i", inputStreamingType))
  val regularModulesCount = 3
  val regularModules = Range(0, regularModulesCount).map(i => createModule(s"name$i", regularStreamingType))

  val typeToModules = Map(
    inputStreamingType -> inputModules,
    regularStreamingType -> regularModules,
    outputStreamingType -> Seq.empty,
    batchStreamingType -> Seq.empty)

  val unknownTypeError = "unknown type"
  when(serviceInterface.getByType(wrongType)).thenReturn(Left(unknownTypeError))
  typeToModules.foreach {
    case (moduleType, modules) =>
      when(serviceInterface.getByType(moduleType)).thenReturn(Right(modules.map(_._2)))
  }

  val allModules = inputModules ++ regularModules
  when(serviceInterface.getAll).thenReturn(allModules.map(_._2))

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

    when(existenceModule.filename).thenReturn(existenceModuleFilename)
    when(existenceModule.file).thenReturn(Some(existenceModuleFile))
  }

  override def afterEach(): Unit = {
    reset(newModuleApi, newModule, existenceModule)
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
    val source = FileIO.fromPath(Paths.get(existenceModuleFile.getAbsolutePath))
    val expectedResponse = ModuleJar(existenceModuleFilename, source)
    when(fileMetadataUtils.fileToSource(existenceModuleFile)).thenReturn(source)

    controller.get(existenceModuleType, existenceModuleName, existenceModuleVersion) shouldBe expectedResponse
  }

  it should "not give module if it does not exist" in {
    val expectedResponse = NotFoundRestResponse(MessageResponseEntity(moduleNotFoundError))

    controller.get(wrongType, wrongName, wrongVersion) shouldBe expectedResponse
  }

  // getAll
  it should "give all modules" in {
    val expectedResponse = OkRestResponse(ModulesResponseEntity(allModules.map(_._1)))

    controller.getAll shouldBe expectedResponse
  }

  // getByType
  it should "give all modules with specific type" in {
    typeToModules.foreach {
      case (moduleType, modules) =>
        val expectedResponse = OkRestResponse(ModulesResponseEntity(modules.map(_._1)))

        controller.getByType(moduleType) shouldBe expectedResponse
    }
  }

  it should "not give modules if module type is incorrect" in {
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
    when(existenceModule.specification).thenReturn(specification)
    when(createSpecificationApi.from(specification)).thenReturn(specificationApi)

    val expectedResponse = OkRestResponse(SpecificationResponseEntity(specificationApi))

    controller.getSpecification(existenceModuleType, existenceModuleName, existenceModuleVersion) shouldBe expectedResponse
  }

  it should "not give module specification if module does not exist" in {
    val expectedResponse = NotFoundRestResponse(MessageResponseEntity(moduleNotFoundError))

    controller.get(wrongType, wrongName, wrongVersion) shouldBe expectedResponse
  }


  private def createModule(name: String, moduleType: String): (ModuleInfo, ModuleMetadata) = {
    val module = mock[ModuleMetadata]
    val version = "version"
    val moduleInfo = ModuleInfo(moduleType, name, version, moduleSize)

    when(fileMetadataUtils.toModuleInfo(module)).thenReturn(moduleInfo)

    (moduleInfo, module)
  }

}
