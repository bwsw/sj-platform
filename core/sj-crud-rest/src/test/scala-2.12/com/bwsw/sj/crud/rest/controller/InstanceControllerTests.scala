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

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.model.instance._
import com.bwsw.sj.common.si.model.module.{CreateModuleMetadata, ModuleMetadata}
import com.bwsw.sj.common.si.result.{Deleted, DeletionError, EntityNotFound, WillBeDeleted}
import com.bwsw.sj.common.si.{InstanceSI, ModuleSI}
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.MessageResourceUtils
import com.bwsw.sj.crud.rest._
import com.bwsw.sj.crud.rest.instance._
import com.bwsw.sj.crud.rest.model.instance.response.{CreateInstanceApiResponse, InstanceApiResponse}
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import scaldi.{Injector, Module}

class InstanceControllerTests extends FlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

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
  val createModuleMetadata = mock[CreateModuleMetadata]
  val createInstanceApiResponse = mock[CreateInstanceApiResponse]
  val instanceStarterBuilder = mock[InstanceStarterBuilder]
  val instanceStopperBuilder = mock[InstanceStopperBuilder]
  val instanceDestroyerBuilder = mock[InstanceDestroyerBuilder]

  val existingModuleType = batchStreamingType
  val existingModuleName = "existing-module-name"
  val existingModuleVersion = "existing-module-version"
  val moduleDomain = mock[FileMetadataDomain]
  val module = mock[ModuleMetadata]
  when(createModuleMetadata.from(argEq(moduleDomain), argEq(None))(any[Injector]())).thenReturn(module)
  when(moduleSI.exists(existingModuleType, existingModuleName, existingModuleVersion))
    .thenReturn(Right(moduleDomain))

  val existingInstanceName = "existing-instance-name"
  val existingInstance = mock[Instance]

  val wrongModuleType = "wrong-module-type"
  val wrongModuleName = "wrong-module-name"
  val wrongModuleVersion = "wrong-module-version"
  val moduleNotFoundError = "module not found"
  when(moduleSI.exists(wrongModuleType, wrongModuleName, wrongModuleVersion)).thenReturn(Left(moduleNotFoundError))
  val moduleNotFoundResponse = NotFoundRestResponse(MessageResponseEntity(moduleNotFoundError))

  val wrongInstanceName = "wrong-instance"
  val instanceNotFoundMessageName = "rest.modules.module.instances.instance.notfound"
  val instanceNotFoundMessage = instanceNotFoundMessageName + "," + wrongInstanceName
  when(messageResourceUtils.createMessage(instanceNotFoundMessageName, wrongInstanceName))
    .thenReturn(instanceNotFoundMessage)
  val instanceNotFoundResponse = NotFoundRestResponse(MessageResponseEntity(instanceNotFoundMessage))

  // defined in application.conf
  val zkHost: Option[String] = Some("zookeeper.host")
  val zkPort: Option[Int] = Some(12345)

  val injector = new Module {
    bind[MessageResourceUtils] to messageResourceUtils
    bind[SettingsUtils] to settingsUtils
    bind[JsonSerializer] to serializer
    bind[JsonDeserializationErrorMessageCreator] to jsonDeserializationErrorMessageCreator
    bind[InstanceSI] to instanceSI
    bind[ModuleSI] to moduleSI
    bind[ConnectionRepository] to connectionRepository
    bind[CreateModuleMetadata] to createModuleMetadata
    bind[CreateInstanceApiResponse] to createInstanceApiResponse
    bind[InstanceStarterBuilder] to instanceStarterBuilder
    bind[InstanceStopperBuilder] to instanceStopperBuilder
    bind[InstanceDestroyerBuilder] to instanceDestroyerBuilder
  }

  val controller = new InstanceController()(injector)

  override def beforeEach(): Unit = {
    when(instanceSI.get(wrongInstanceName)).thenReturn(None)
    when(instanceSI.get(existingInstanceName)).thenReturn(Some(existingInstance))
  }

  override def afterEach(): Unit = {
    reset(instanceSI)
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
    //    Thread.sleep(100)
    //    verify(instanceDestroyer).run()
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
    //    Thread.sleep(100)
    //    verify(instanceStarter).run()
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
    //    Thread.sleep(100)
    //    verify(instanceStopper).run()
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
