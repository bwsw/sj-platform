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

import java.io.{ByteArrayInputStream, File}
import java.net.URI
import java.util.Date

import com.bwsw.common.JsonSerializer
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.common.http.{HttpClient, HttpClientBuilder}
import com.bwsw.sj.common.config.{ConfigLiterals, SettingsUtils}
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, ModuleMetadataCreator, Specification}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.si.{InstanceSI, ModuleSI}
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.instance._
import com.bwsw.sj.crud.rest.instance.validator.InstanceValidator
import com.bwsw.sj.crud.rest.model.instance.response.{InstanceApiResponse, InstanceApiResponseCreator}
import com.bwsw.sj.crud.rest.model.instance.{BatchInstanceApi, InputInstanceApi, OutputInstanceApi, RegularInstanceApi}
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import org.apache.http._
import org.apache.http.client.methods.{CloseableHttpResponse, HttpGet}
import org.mockito.ArgumentMatchers.{any, anyInt, argThat, eq => argEq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import scaldi.{Injector, Module}

import scala.collection.mutable.ArrayBuffer

class InstanceControllerTests extends FlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  import InstanceControllerTests._

  val messageResourceUtils = mock[MessageResourceUtils]
  val marathonAddress = "marathon.address:1234"
  val settingsUtils = mock[SettingsUtils]
  when(settingsUtils.getMarathonConnect()).thenReturn(marathonAddress)

  val serializer = mock[JsonSerializer]
  val jsonDeserializationErrorMessageCreator = mock[JsonDeserializationErrorMessageCreator]

  val configRepository = mock[GenericMongoRepository[ConfigurationSettingDomain]]
  val connectionRepository = mock[ConnectionRepository]
  when(connectionRepository.getConfigRepository).thenReturn(configRepository)

  val moduleSI = mock[ModuleSI]
  val instanceSI = mock[InstanceSI]
  val createModuleMetadata = mock[ModuleMetadataCreator]
  val createInstanceApiResponse = mock[InstanceApiResponseCreator]
  val instanceStarterBuilder = mock[InstanceStarterBuilder]
  val instanceStopperBuilder = mock[InstanceStopperBuilder]
  val instanceDestroyerBuilder = mock[InstanceDestroyerBuilder]
  val httpClientBuilder = mock[HttpClientBuilder]

  val existingModuleType = batchStreamingType
  val moduleDomain = mock[FileMetadataDomain]
  val module = mock[ModuleMetadata]
  when(createModuleMetadata.from(argEq(moduleDomain), any[Option[File]])(any[Injector]())).thenReturn(module)

  val existingInstanceName = "existing-instance-name"
  val existingInstance = mock[Instance]
  moduleTypes.foreach { moduleType =>
    when(moduleSI.exists(moduleType, existingModuleName, existingModuleVersion)).thenReturn(Right(moduleDomain))
  }

  val wrongModuleType = "wrong-module-type"
  val wrongModuleName = "wrong-module-name"
  val wrongModuleVersion = "wrong-module-version"
  val moduleNotFoundError = "module not found"
  val moduleNotFoundResponse = NotFoundRestResponse(MessageResponseEntity(moduleNotFoundError))

  val wrongInstanceName = "wrong-instance"
  val instanceNotFoundMessageName = "rest.modules.module.instances.instance.notfound"
  val instanceNotFoundMessage = instanceNotFoundMessageName + "," + wrongInstanceName
  val instanceNotFoundResponse = NotFoundRestResponse(MessageResponseEntity(instanceNotFoundMessage))

  // defined in application.conf
  val zkHost: Option[String] = Some("zookeeper.host")
  val zkPort: Option[Int] = Some(12345)

  val threadSleepingTimeout = 200

  val injector = new Module {
    bind[MessageResourceUtils] to messageResourceUtils
    bind[SettingsUtils] to settingsUtils
    bind[JsonSerializer] to serializer
    bind[JsonDeserializationErrorMessageCreator] to jsonDeserializationErrorMessageCreator
    bind[InstanceSI] to instanceSI
    bind[ModuleSI] to moduleSI
    bind[ConnectionRepository] to connectionRepository
    bind[ModuleMetadataCreator] to createModuleMetadata
    bind[InstanceApiResponseCreator] to createInstanceApiResponse
    bind[InstanceStarterBuilder] to instanceStarterBuilder
    bind[InstanceStopperBuilder] to instanceStopperBuilder
    bind[InstanceDestroyerBuilder] to instanceDestroyerBuilder
    bind[HttpClientBuilder] to httpClientBuilder
  }

  val controller = new InstanceController()(injector)

  override def beforeEach(): Unit = {
    when(instanceSI.get(wrongInstanceName)).thenReturn(None)
    when(instanceSI.get(existingInstanceName)).thenReturn(Some(existingInstance))

    when(moduleSI.exists(existingModuleType, existingModuleName, existingModuleVersion))
      .thenReturn(Right(moduleDomain))
    when(moduleSI.exists(wrongModuleType, wrongModuleName, wrongModuleVersion)).thenReturn(Left(moduleNotFoundError))

    when(messageResourceUtils.createMessage(instanceNotFoundMessageName, wrongInstanceName))
      .thenReturn(instanceNotFoundMessage)
  }

  override def afterEach(): Unit = {
    reset(
      messageResourceUtils,
      serializer,
      jsonDeserializationErrorMessageCreator,
      instanceSI,
      createInstanceApiResponse,
      instanceStarterBuilder,
      instanceStopperBuilder,
      instanceDestroyerBuilder,
      httpClientBuilder,
      moduleDomain,
      module,
      existingInstance)
  }

  // get
  "InstanceController" should "give instance if it exists" in {
    val existingInstanceApi = mock[InstanceApiResponse]
    when(createInstanceApiResponse.from(existingInstance)).thenReturn(existingInstanceApi)

    val expectedResponse = OkRestResponse(InstanceResponseEntity(existingInstanceApi))
    val response = controller.get(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      existingInstanceName)
    response shouldBe expectedResponse
  }

  it should "tell that instance does not exists (get)" in {
    val response = controller.get(existingModuleType, existingModuleName, existingModuleVersion, wrongInstanceName)
    response shouldBe instanceNotFoundResponse
  }

  it should "tell that module does not exists (get)" in {
    val response = controller.get(wrongModuleType, wrongModuleName, wrongModuleVersion, existingInstanceName)
    response shouldBe moduleNotFoundResponse
  }

  // getAll
  it should "give all instances" in {
    val instancesCount = 5
    val instances = Range(0, instancesCount).map { i =>
      createInstance(existingModuleType, existingModuleName, existingModuleVersion, s"instance-$i")
    }.toBuffer
    when(instanceSI.getAll).thenReturn(instances.map(_.instance))

    val expectedResponse = OkRestResponse(ShortInstancesResponseEntity(instances.map(_.short)))

    controller.getAll shouldBe expectedResponse
  }

  // getByModule
  it should "give all instances of a specific module" in {
    Seq(
      (inputStreamingType, "module-1-name", "module-1-version", 2),
      (outputStreamingType, "module-2-name", "module-2-version", 0),
      (regularStreamingType, "module-3-name", "module-3-version", 3)).map {
      case (moduleType, moduleName, moduleVersion, instancesCount) =>
        val domain = mock[FileMetadataDomain]
        when(moduleSI.exists(moduleType, moduleName, moduleVersion)).thenReturn(Right(domain))
        val module = mock[ModuleMetadata]
        when(createModuleMetadata.from(argEq(domain), argEq(None))(any[Injector]())).thenReturn(module)

        val instances = Range(0, instancesCount).map { i =>
          createInstance(moduleType, moduleName, moduleVersion, s"instance-$i")
        }
        when(instanceSI.getByModule(moduleType, moduleName, moduleVersion)).thenReturn(instances.map(_.instance))

        (moduleType, moduleName, moduleVersion, instances.map(_.api))
    }.foreach {
      case (moduleType, moduleName, moduleVersion, apiResponse) =>

        val expectedResponse = OkRestResponse(InstancesResponseEntity(apiResponse))

        controller.getByModule(moduleType, moduleName, moduleVersion) shouldBe expectedResponse
    }
  }

  it should "tell that module does not exists (getByModule)" in {
    controller.getByModule(wrongModuleType, wrongModuleName, wrongModuleVersion) shouldBe moduleNotFoundResponse
  }

  // delete
  it should "tell that instance has been deleted" in {
    when(instanceSI.delete(existingInstanceName)).thenReturn(Deleted)
    val messageName = "rest.modules.instances.instance.deleted"
    val message = messageName + "," + existingInstanceName
    when(messageResourceUtils.createMessage(messageName, existingInstanceName)).thenReturn(message)

    val expectedResponse = OkRestResponse(MessageResponseEntity(message))
    val response = controller.delete(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      existingInstanceName)

    response shouldBe expectedResponse
  }

  it should "tell that instance will be deleted" in {
    val instanceDestroyer = mock[InstanceDestroyer]
    when(instanceDestroyerBuilder.apply(existingInstance, marathonAddress)).thenReturn(instanceDestroyer)

    when(instanceSI.delete(existingInstanceName)).thenReturn(WillBeDeleted(existingInstance))
    val messageName = "rest.modules.instances.instance.deleting"
    val message = messageName + "," + existingInstanceName
    when(messageResourceUtils.createMessage(messageName, existingInstanceName)).thenReturn(message)

    val expectedResponse = OkRestResponse(MessageResponseEntity(message))

    val response = controller.delete(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      existingInstanceName)

    response shouldBe expectedResponse
    Thread.sleep(threadSleepingTimeout)
    verify(instanceDestroyer).run()
  }

  it should "tell that instance could not been deleted" in {
    val error = "can't delete instance"
    when(instanceSI.delete(existingInstanceName)).thenReturn(DeletionError(error))

    val expectedResponse = UnprocessableEntityRestResponse(MessageResponseEntity(error))
    val response = controller.delete(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      existingInstanceName)

    response shouldBe expectedResponse
  }

  it should "tell that instance does not exists (delete)" in {
    when(instanceSI.delete(wrongInstanceName)).thenReturn(EntityNotFound)

    val response = controller.delete(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      wrongInstanceName)

    response shouldBe instanceNotFoundResponse
  }

  it should "tell that module does not exists (delete)" in {
    val response = controller.delete(wrongModuleType, wrongModuleName, wrongModuleVersion, existingInstanceName)
    response shouldBe moduleNotFoundResponse
  }

  // start
  it should "start module if it could be started" in {
    val instanceStarter = mock[InstanceStarter]
    when(instanceStarterBuilder.apply(existingInstance, marathonAddress, zkHost, zkPort)).thenReturn(instanceStarter)

    when(instanceSI.canStart(existingInstance)).thenReturn(true)
    val messageName = "rest.modules.instances.instance.starting"
    val message = messageName + "," + existingInstanceName
    when(messageResourceUtils.createMessage(messageName, existingInstanceName)).thenReturn(message)

    val expectedResponse = OkRestResponse(MessageResponseEntity(message))

    val response = controller.start(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      existingInstanceName)

    response shouldBe expectedResponse
    Thread.sleep(threadSleepingTimeout)
    verify(instanceStarter).run()
  }

  it should "not start module if it couldn't be started" in {
    when(instanceSI.canStart(existingInstance)).thenReturn(false)
    val messageName = "rest.modules.instances.instance.cannot.start"
    val message = messageName + "," + existingInstanceName
    when(messageResourceUtils.createMessage(messageName, existingInstanceName)).thenReturn(message)

    val expectedResponse = UnprocessableEntityRestResponse(MessageResponseEntity(message))

    val response = controller.start(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      existingInstanceName)

    response shouldBe expectedResponse
  }

  it should "tell that instance does not exists (start)" in {
    val response = controller.start(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      wrongInstanceName)

    response shouldBe instanceNotFoundResponse
  }

  it should "tell that module does not exists (start)" in {
    val response = controller.start(wrongModuleType, wrongModuleName, wrongModuleVersion, existingInstanceName)
    response shouldBe moduleNotFoundResponse
  }

  // stop
  it should "stop module if it could be stopped" in {
    val instanceStopper = mock[InstanceStopper]
    when(instanceStopperBuilder.apply(existingInstance, marathonAddress)).thenReturn(instanceStopper)

    when(instanceSI.canStop(existingInstance)).thenReturn(true)
    val messageName = "rest.modules.instances.instance.stopping"
    val message = messageName + "," + existingInstanceName
    when(messageResourceUtils.createMessage(messageName, existingInstanceName)).thenReturn(message)

    val expectedResponse = OkRestResponse(MessageResponseEntity(message))

    val response = controller.stop(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      existingInstanceName)

    response shouldBe expectedResponse
    Thread.sleep(threadSleepingTimeout)
    verify(instanceStopper).run()
  }

  it should "not stop module if it couldn't be stopped" in {
    when(instanceSI.canStop(existingInstance)).thenReturn(false)
    val messageName = "rest.modules.instances.instance.cannot.stop"
    val message = messageName + "," + existingInstanceName
    when(messageResourceUtils.createMessage(messageName, existingInstanceName)).thenReturn(message)

    val expectedResponse = UnprocessableEntityRestResponse(MessageResponseEntity(message))

    val response = controller.stop(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      existingInstanceName)

    response shouldBe expectedResponse
  }

  it should "tell that instance does not exists (stop)" in {
    val response = controller.stop(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      wrongInstanceName)

    response shouldBe instanceNotFoundResponse
  }

  it should "tell that module does not exists (stop)" in {
    val response = controller.stop(wrongModuleType, wrongModuleName, wrongModuleVersion, existingInstanceName)
    response shouldBe moduleNotFoundResponse
  }

  // tasks
  it should "give all tasks of the instance" in {
    val restAddress = "rest.address:4321"
    when(existingInstance.restAddress).thenReturn(Some(restAddress))

    val tasksCount = 4
    val tasks = Range(0, tasksCount).map { i =>
      val task = FrameworkTask(i.toString, null, null, null, null, null, scala.collection.mutable.ListBuffer.empty)
      val serialized = s"""{"id":"$i"}"""
      (task, serialized)
    }

    val frameworkRestEntity = FrameworkRestEntity(tasks.map(_._1))
    val serializedEntity = tasks.map(_._2).mkString("""{"tasks":[""", ",", "]}")
    when(serializer.deserialize[FrameworkRestEntity](serializedEntity)).thenReturn(frameworkRestEntity)

    val responseEntity = mock[HttpEntity]
    when(responseEntity.getContent).thenReturn(new ByteArrayInputStream(serializedEntity.getBytes))

    val httpResponse = mock[CloseableHttpResponse]
    when(httpResponse.getEntity).thenReturn(responseEntity)

    val httpClient = mock[HttpClient]
    when(httpClient.execute(argThat[HttpGet](
      (argument: HttpGet) => argument.getURI == new URI(restAddress)))).thenReturn(httpResponse)
    when(httpClientBuilder.apply(anyInt())).thenReturn(httpClient)

    val expectedResponse = OkRestResponse(frameworkRestEntity)
    val response = controller.tasks(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      existingInstanceName)

    response shouldBe expectedResponse
  }

  it should "tell that instance does not have framework info" in {
    when(existingInstance.restAddress).thenReturn(None)

    val message = "rest.modules.instances.instance.cannot.get.tasks"
    when(messageResourceUtils.getMessage(message)).thenReturn(message)
    val expectedResponse = UnprocessableEntityRestResponse(MessageResponseEntity(message))

    val response = controller.tasks(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      existingInstanceName)

    response shouldBe expectedResponse
  }

  it should "tell that instance does not exists (tasks)" in {
    val response = controller.tasks(
      existingModuleType,
      existingModuleName,
      existingModuleVersion,
      wrongInstanceName)

    response shouldBe instanceNotFoundResponse
  }

  it should "tell that module does not exists (tasks)" in {
    val response = controller.tasks(wrongModuleType, wrongModuleName, wrongModuleVersion, existingInstanceName)
    response shouldBe moduleNotFoundResponse
  }

  // create
  it should "create valid instance" in new InstanceCreation {
    val messageName = "rest.modules.instances.instance.created"
    when(module.specification).thenReturn(specificationForValidInstance)

    instancesInfo.foreach {
      case (moduleType, _, instance, _) =>
        when(instance.name).thenReturn(instanceName)
        val moduleSignature = s"$moduleType-$existingModuleName-$existingModuleVersion"
        when(module.signature).thenReturn(moduleSignature)
        when(instanceSI.create(instance, module)).thenReturn(Created)

        val message = messageName + "," + instanceName + "," + moduleSignature
        when(messageResourceUtils.createMessage(messageName, instanceName, moduleSignature)).thenReturn(message)
        val expectedResponse = CreatedRestResponse(MessageResponseEntity(message))
        val response = controller.create(serializedInstance, moduleType, existingModuleName, existingModuleVersion)

        response shouldBe expectedResponse
    }
  }

  it should "not create incorrect instance (error from InstanceSI.create())" in new InstanceCreation {
    val messageName = "rest.modules.instances.instance.cannot.create.incorrect.parameters"
    when(module.specification).thenReturn(specificationForValidInstance)
    val error = "instance is incorrect"
    val message = messageName + "," + error
    when(messageResourceUtils.createMessageWithErrors(messageName, ArrayBuffer(error))).thenReturn(message)
    val expectedResponse = BadRequestRestResponse(MessageResponseEntity(message))

    instancesInfo.foreach {
      case (moduleType, _, instance, _) =>
        when(instanceSI.create(instance, module)).thenReturn(NotCreated(ArrayBuffer(error)))
        val response = controller.create(serializedInstance, moduleType, existingModuleName, existingModuleVersion)

        response shouldBe expectedResponse
    }
  }

  it should "not create incorrect instance (error from InstanceValidator.validate())" in new InstanceCreation {
    val messageName = "rest.modules.instances.instance.cannot.create"
    when(module.specification).thenReturn(specificationForNotValidInstance)
    val message = messageName + "," + validationError
    when(messageResourceUtils.createMessageWithErrors(messageName, ArrayBuffer(validationError))).thenReturn(message)
    val expectedResponse = BadRequestRestResponse(MessageResponseEntity(message))

    instancesInfo.foreach {
      case (moduleType, _, _, _) =>
        val response = controller.create(serializedInstance, moduleType, existingModuleName, existingModuleVersion)

        response shouldBe expectedResponse
    }
  }


  it should "not create instance if json is incorrect" in {
    val serialized = "{not a json"
    val error = "json is incorrect"
    val exception = new JsonDeserializationException(error)
    when(jsonDeserializationErrorMessageCreator.apply(exception)).thenReturn(error)
    when(serializer.deserialize[InputInstanceApi](serialized)).thenAnswer(_ => throw exception)
    when(serializer.deserialize[BatchInstanceApi](serialized)).thenAnswer(_ => throw exception)
    when(serializer.deserialize[RegularInstanceApi](serialized)).thenAnswer(_ => throw exception)
    when(serializer.deserialize[OutputInstanceApi](serialized)).thenAnswer(_ => throw exception)

    val messageName = "rest.modules.instances.instance.cannot.create"
    val message = messageName + "," + error
    when(messageResourceUtils.createMessage(messageName, error)).thenReturn(message)
    val expectedResponse = BadRequestRestResponse(MessageResponseEntity(message))

    moduleTypes.foreach { moduleType =>
      val response = controller.create(
        serialized,
        moduleType,
        existingModuleName,
        existingModuleVersion)

      response shouldBe expectedResponse
    }
  }

  it should "tell that module does not exists (create)" in {
    val serialized = """{"name":"instance"}"""
    val response = controller.create(serialized, wrongModuleType, wrongModuleName, wrongModuleVersion)
    response shouldBe moduleNotFoundResponse
  }

  trait InstanceCreation {
    val instanceName = "new-instance"
    val serializedInstance = s"""{"name":"$instanceName"}"""
    when(serializer.deserialize[InputInstanceApi](serializedInstance)).thenReturn(inputInstanceApi)
    when(serializer.deserialize[RegularInstanceApi](serializedInstance)).thenReturn(regularInstanceApi)
    when(serializer.deserialize[BatchInstanceApi](serializedInstance)).thenReturn(batchInstanceApi)
    when(serializer.deserialize[OutputInstanceApi](serializedInstance)).thenReturn(outputInstanceApi)

    instancesInfo.foreach {
      case (moduleType, _, instance, validatorClass) =>
        val validatorClassConfigName = s"${ConfigLiterals.systemDomain}.$moduleType-validator-class"
        val validatorClassName = validatorClass.getName
        val validatorClassConfig = ConfigurationSettingDomain(validatorClassConfigName, validatorClassName,
          ConfigLiterals.systemDomain, new Date())

        when(configRepository.get(validatorClassConfigName)).thenReturn(Some(validatorClassConfig))
        when(instance.moduleType).thenReturn(moduleType)
    }
  }

  def createInstance(moduleType: String, moduleName: String, moduleVersion: String, name: String): InstanceInfo = {
    val description = "description"
    val status = ready
    val restAddress = "rest.address"

    val instance = mock[Instance]
    when(instance.name).thenReturn(name)
    when(instance.moduleType).thenReturn(moduleType)
    when(instance.moduleName).thenReturn(moduleName)
    when(instance.moduleVersion).thenReturn(moduleVersion)
    when(instance.description).thenReturn(description)
    when(instance.status).thenReturn(status)
    when(instance.restAddress).thenReturn(Some(restAddress))

    val short = ShortInstance(
      name,
      moduleType,
      moduleName,
      moduleVersion,
      description,
      status,
      restAddress)

    val apiResponse = mock[InstanceApiResponse]
    when(createInstanceApiResponse.from(instance)).thenReturn(apiResponse)

    InstanceInfo(instance, short, apiResponse)
  }

  case class InstanceInfo(instance: Instance, short: ShortInstance, api: InstanceApiResponse)

}

object InstanceControllerTests extends MockitoSugar {
  val existingModuleName = "existing-module-name"
  val existingModuleVersion = "existing-module-version"

  val inputInstance = mock[InputInstance]
  val inputInstanceApi = mock[InputInstanceApi]
  when(inputInstanceApi
    .to(argEq(inputStreamingType), argEq(existingModuleName), argEq(existingModuleVersion))(any[Injector]()))
    .thenReturn(inputInstance)

  val batchInstance = mock[BatchInstance]
  val batchInstanceApi = mock[BatchInstanceApi]
  when(batchInstanceApi
    .to(argEq(batchStreamingType), argEq(existingModuleName), argEq(existingModuleVersion))(any[Injector]()))
    .thenReturn(batchInstance)

  val regularInstance = mock[RegularInstance]
  val regularInstanceApi = mock[RegularInstanceApi]
  when(regularInstanceApi
    .to(argEq(regularStreamingType), argEq(existingModuleName), argEq(existingModuleVersion))(any[Injector]()))
    .thenReturn(regularInstance)

  val outputInstance = mock[OutputInstance]
  val outputInstanceApi = mock[OutputInstanceApi]
  when(outputInstanceApi
    .to(argEq(outputStreamingType), argEq(existingModuleName), argEq(existingModuleVersion))(any[Injector]()))
    .thenReturn(outputInstance)

  val instancesInfo = Seq(
    (inputStreamingType, inputInstanceApi, inputInstance, classOf[InputInstanceValidator]),
    (batchStreamingType, batchInstanceApi, batchInstance, classOf[BatchInstanceValidator]),
    (regularStreamingType, regularInstanceApi, regularInstance, classOf[RegularInstanceValidator]),
    (outputStreamingType, outputInstanceApi, outputInstance, classOf[OutputInstanceValidator]))

  val specificationForValidInstance = mock[Specification]
  val specificationForNotValidInstance = mock[Specification]
  val validationError = "instance not valid"

  def validateInstance(instance: Instance, specification: Specification): Seq[String] = {
    if (Seq(inputInstance, batchInstance, regularInstance, outputInstance).contains(instance)) {
      if (specification == specificationForValidInstance)
        Seq.empty
      else if (specification == specificationForNotValidInstance)
        Seq(validationError)
      else
        Seq("unknown specification")
    } else {
      Seq("unknown instance")
    }
  }

  class InputInstanceValidator(implicit injector: Injector) extends InstanceValidator {
    override type T = InputInstance

    override def validate(instance: InputInstance, specification: Specification): Seq[String] =
      validateInstance(instance, specification)
  }

  class BatchInstanceValidator(implicit injector: Injector) extends InstanceValidator {
    override type T = BatchInstance

    override def validate(instance: BatchInstance, specification: Specification): Seq[String] =
      validateInstance(instance, specification)
  }

  class RegularInstanceValidator(implicit injector: Injector) extends InstanceValidator {
    override type T = RegularInstance

    override def validate(instance: RegularInstance, specification: Specification): Seq[String] =
      validateInstance(instance, specification)
  }

  class OutputInstanceValidator(implicit injector: Injector) extends InstanceValidator {
    override type T = OutputInstance

    override def validate(instance: OutputInstance, specification: Specification): Seq[String] =
      validateInstance(instance, specification)
  }

}
