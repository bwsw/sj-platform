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

import com.bwsw.common.file.utils.{ClosableClassLoader, MongoFileStorage}
import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.engine.{StreamingValidator, ValidationInfo}
import com.bwsw.sj.common.si.model.instance.{Instance, InstanceCreator}
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, Specification}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils._
import org.mockito.ArgumentMatchers.{any, anyString, eq => argEq}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import scaldi.{Injector, Module}

import scala.collection.mutable.ArrayBuffer

class InstanceSiTests extends FlatSpec with Matchers with BeforeAndAfterEach with MockitoSugar {

  import InstanceSiTests._

  val readyInstance = createInstanceInfo(module1, "ready-instance", ready)
  val startedInstance = createInstanceInfo(module1, "started-instance", started)
  val failedInstance = createInstanceInfo(module1, "failed-instance", failed)
  val module1Instances = Seq(readyInstance, startedInstance, failedInstance)

  val module2 = createModule(inputStreamingType, "module-2", "v2")
  val stoppedInstance = createInstanceInfo(module2, "stopped-instance", stopped)
  val deletedInstance = createInstanceInfo(module2, "deleted-instance", deleted)
  val errorInstance = createInstanceInfo(module2, "error-instance", error)
  val module2Instances = Seq(deletedInstance, stoppedInstance, errorInstance)

  val storedInstances = module1Instances ++ module2Instances
  val canStartInstances = Seq(readyInstance, stoppedInstance, failedInstance)

  val moduleToInstances = Map(module1 -> module1Instances, module2 -> module2Instances)

  val instanceRepository = mock[GenericMongoRepository[InstanceDomain]]
  val connectionRepository = mock[ConnectionRepository]
  when(connectionRepository.getInstanceRepository).thenReturn(instanceRepository)



  val injector = new Module {
    bind[ConnectionRepository] to connectionRepository
    bind[MessageResourceUtils] to MessageResourceUtilsMock.messageResourceUtils
    bind[InstanceCreator] to createInstance
  }.injector


  class InstanceSIMock(implicit injector: Injector) extends InstanceSI()(injector) {
    private val fileClassLoader = mock[FileClassLoader]
    Seq[Class[_]](
      classOf[AllValid],
      classOf[InstanceNotValid],
      classOf[OptionsNotValid],
      classOf[InstanceAndOptionsNotValid]).foreach { clazz =>
      when(fileClassLoader.loadClass(argEq(clazz.getName)))
        .thenAnswer(_ => clazz)
    }
    override protected def createClassLoader(filename: String) = fileClassLoader
  }

  val instanceSI = new InstanceSIMock()(injector)

  override def beforeEach(): Unit = {
    reset(instanceRepository)

    when(instanceRepository.getByParameters(any[Map[String, Any]]())).thenReturn(Seq.empty)
    when(instanceRepository.get(anyString())).thenReturn(None)
    when(instanceRepository.getAll).thenReturn(storedInstances.map(_.instanceDomain).toBuffer)
    moduleToInstances.foreach {
      case (module, instances) =>
        when(instanceRepository.getByParameters(
          Map(
            "module-name" -> module.specification.name,
            "module-type" -> module.specification.moduleType,
            "module-version" -> module.specification.version)))
          .thenReturn(instances.map(_.instanceDomain))
    }
    storedInstances.foreach {
      case InstanceInfo(_, instanceDomain, _) =>
        when(instanceRepository.get(instanceDomain.name)).thenReturn(Some(instanceDomain))
    }
  }


  // create
  "InstanceSI" should "create valid instance" in {
    notStoredInstance match {
      case InstanceInfo(instance, _, module) =>
        when(module.specification.validatorClass).thenReturn(classOf[AllValid].getName)

        instanceSI.create(instance, module) shouldBe Created
    }
  }

  it should "not create instance if options not valid" in {
    notStoredInstance match {
      case InstanceInfo(instance, _, module) =>
        when(module.specification.validatorClass).thenReturn(classOf[OptionsNotValid].getName)

        instanceSI.create(instance, module) shouldBe NotCreated(ArrayBuffer(optionsNotValidError))
    }
  }

  it should "not create instance if it not valid" in {
    notStoredInstance match {
      case InstanceInfo(instance, _, module) =>
        when(module.specification.validatorClass).thenReturn(classOf[InstanceNotValid].getName)

        instanceSI.create(instance, module) shouldBe NotCreated(ArrayBuffer(instanceNotValidError))
    }
  }

  it should "not create valid instance if it and options not valid" in {
    notStoredInstance match {
      case InstanceInfo(instance, _, module) =>
        when(module.specification.validatorClass).thenReturn(classOf[InstanceAndOptionsNotValid].getName)

        val response = instanceSI.create(instance, module)
        response shouldBe a[NotCreated]
        val errors = response.asInstanceOf[NotCreated].errors
        errors.toSet shouldBe Set(optionsNotValidError, instanceNotValidError)
    }
  }

  // getAll
  it should "give all instances from storage" in {
    instanceSI.getAll.toSet shouldBe storedInstances.map(_.instance).toSet
  }

  // getByModule
  it should "give all instances of specific module" in {
    moduleToInstances.foreach {
      case (module, instances) =>
        val moduleType = module.specification.moduleType
        val moduleName = module.specification.name
        val moduleVersion = module.specification.version

        instanceSI.getByModule(moduleType, moduleName, moduleVersion).toSet shouldBe instances.map(_.instance).toSet
    }
  }

  // get
  it should "give module if it exists" in {
    storedInstances.foreach {
      case InstanceInfo(instance, _, _) =>
        instanceSI.get(instance.name) shouldBe Some(instance)
    }
  }

  it should "not give instance if it does not exists" in {
    instanceSI.get("not-existed-instsce") shouldBe empty
  }

  // delete
  it should "delete instance if it has never been run and have status that does allow deletion" in {
    val name = readyInstance.instance.name

    instanceSI.delete(name) shouldBe Deleted
    verify(instanceRepository).delete(name)
  }

  it should "mark instance to deletion if it has already been run and have status that does allow deletion" in {
    Seq(stoppedInstance, failedInstance, errorInstance).foreach {
      case InstanceInfo(instance, _, _) =>
        instanceSI.delete(instance.name) shouldBe WillBeDeleted(instance)
        verify(instanceRepository, never()).delete(anyString())
    }
  }

  it should "not delete instance if it have status that does not allow deletion" in {
    Seq(startedInstance, deletedInstance).foreach {
      case InstanceInfo(instance, _, _) =>
        val error = "rest.modules.instances.instance.cannot.delete:" + instance.name

        instanceSI.delete(instance.name) shouldBe DeletionError(error)
        verify(instanceRepository, never()).delete(anyString())
    }
  }

  it should "not delete instance if it does not exists" in {
    instanceSI.delete("not-existed-instance") shouldBe EntityNotFound
    verify(instanceRepository, never()).delete(anyString())
  }

  // canStart
  it should "correctly indicate that instance can be started" in {
    canStartInstances.foreach {
      case InstanceInfo(instance, _, _) =>
        instanceSI.canStart(instance) shouldBe true
    }
  }

  it should "correctly indicate that instance cannot be started" in {
    storedInstances.diff(canStartInstances).foreach {
      case InstanceInfo(instance, _, _) =>
        instanceSI.canStart(instance) shouldBe false
    }
  }

  // canStop
  it should "correctly indicate that instance can be stopped" in {
    instanceSI.canStop(startedInstance.instance) shouldBe true
  }

  it should "correctly indicate that instance cannot be stopped" in {
    storedInstances.filter(_ != startedInstance).foreach {
      case InstanceInfo(instance, _, _) =>
        instanceSI.canStop(instance) shouldBe false
    }
  }
}

object InstanceSiTests extends MockitoSugar {

  val createInstance = mock[InstanceCreator]

  val moduleFilename = "module.jar"
  val module1 = createModule(regularStreamingType, "module-1", "v1")

  val notStoredInstance = createInstanceInfo(module1, "not-stored-instance", ready)
  val instanceNotValidError = "instance not valid"
  val optionsNotValidError = "options not valid"

  def createModule(moduleType: String, name: String, version: String) = {
    val specification = mock[Specification]
    when(specification.name).thenReturn(name)
    when(specification.version).thenReturn(version)
    when(specification.moduleType).thenReturn(moduleType)

    val module = mock[ModuleMetadata]
    when(module.specification).thenReturn(specification)
    when(module.name).thenReturn(Some(name))
    when(module.version).thenReturn(Some(version))
    when(module.filename).thenReturn(moduleFilename)

    module
  }

  def createInstanceInfo(module: ModuleMetadata, name: String, status: String) = {
    val instanceDomain = mock[InstanceDomain]

    val moduleType = module.specification.moduleType
    val moduleName = module.specification.name
    val moduleVersion = module.specification.version

    when(instanceDomain.moduleType).thenReturn(moduleType)
    when(instanceDomain.moduleName).thenReturn(moduleName)
    when(instanceDomain.moduleVersion).thenReturn(moduleVersion)
    when(instanceDomain.name).thenReturn(name)
    when(instanceDomain.status).thenReturn(status)

    val instance = mock[Instance]
    when(instance.moduleType).thenReturn(moduleType)
    when(instance.moduleName).thenReturn(moduleName)
    when(instance.moduleVersion).thenReturn(moduleVersion)
    when(instance.name).thenReturn(name)
    when(instance.status).thenReturn(status)
    when(instance.options).thenReturn(s"""{"name": $name}""")
    when(instance.to).thenReturn(instanceDomain)

    when(createInstance.from(argEq(instanceDomain))(any[Injector]())).thenReturn(instance)

    InstanceInfo(instance, instanceDomain, module)
  }

  case class InstanceInfo(instance: Instance,
                          instanceDomain: InstanceDomain,
                          module: ModuleMetadata)

  def notValid(instance: Instance) = {
    if (instance == notStoredInstance.instance)
      ValidationInfo(result = false, ArrayBuffer(instanceNotValidError))
    else ValidationInfo()
  }

  def valid(instance: Instance) = {
    if (instance != notStoredInstance.instance)
      ValidationInfo(result = false, ArrayBuffer(instanceNotValidError))
    else ValidationInfo()
  }

  def notValid(options: String) = {
    if (options == notStoredInstance.instance.options)
      ValidationInfo(result = false, ArrayBuffer(optionsNotValidError))
    else ValidationInfo()
  }

  def valid(options: String) = {
    if (options != notStoredInstance.instance.options)
      ValidationInfo(result = false, ArrayBuffer(optionsNotValidError))
    else ValidationInfo()
  }

  class AllValid extends StreamingValidator {
    override def validate(instance: Instance) = valid(instance)

    override def validate(options: String) = valid(options)
  }

  class InstanceNotValid extends StreamingValidator {
    override def validate(instance: Instance) = notValid(instance)

    override def validate(options: String) = valid(options)
  }

  class OptionsNotValid extends StreamingValidator {
    override def validate(instance: Instance) = valid(instance)

    override def validate(options: String) = notValid(options)
  }

  class InstanceAndOptionsNotValid extends StreamingValidator {
    override def validate(instance: Instance) = notValid(instance)

    override def validate(options: String) = notValid(options)
  }

}
