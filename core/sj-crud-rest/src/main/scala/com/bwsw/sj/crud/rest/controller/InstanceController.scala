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

import java.net.URI

import com.bwsw.common.JsonSerializer
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.common.http.HttpClientBuilder
import com.bwsw.sj.common.config.{ConfigLiterals, SettingsUtils}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si._
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, ModuleMetadataCreator, Specification}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.{CommonAppConfigNames, EngineLiterals, MessageResourceUtils, RestLiterals}
import com.bwsw.sj.crud.rest.exceptions.ConfigSettingNotFound
import com.bwsw.sj.crud.rest.instance.validator.InstanceValidator
import com.bwsw.sj.crud.rest.instance._
import com.bwsw.sj.crud.rest.model.instance._
import com.bwsw.sj.crud.rest.model.instance.response.InstanceApiResponseCreator
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import com.bwsw.sj.crud.rest.{InstanceResponseEntity, InstancesResponseEntity, ShortInstance, ShortInstancesResponseEntity}
import com.typesafe.config.ConfigFactory
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class InstanceController(implicit injector: Injector) {
  private val messageResourceUtils = inject[MessageResourceUtils]
  private val settingsUtils = inject[SettingsUtils]

  import messageResourceUtils._

  private val logger = LoggerFactory.getLogger(getClass)
  private val (zkHost, zkPort) = getZkProperties()
  private val serializer = inject[JsonSerializer]
  serializer.enableNullForPrimitives(false)
  private val jsonDeserializationErrorMessageCreator = inject[JsonDeserializationErrorMessageCreator]
  private val serviceInterface = inject[InstanceSI]
  private val moduleSI = inject[ModuleSI]
  private val configService = inject[ConnectionRepository].getConfigRepository
  private val createModuleMetadata = inject[ModuleMetadataCreator]
  private val createInstanceApiResponse = inject[InstanceApiResponseCreator]

  def create(serializedEntity: String, moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    ifModuleExists(moduleType, moduleName, moduleVersion) { module =>
      Try(deserializeInstanceApi(serializedEntity, moduleType))
        .map(_.to(moduleType, moduleName, moduleVersion)) match {
        case Success(instance) =>
          val errors = new ArrayBuffer[String]
          errors ++= validateInstance(instance, module.specification)
          if (errors.isEmpty) {
            serviceInterface.create(instance, module) match {
              case Created =>
                CreatedRestResponse(
                  MessageResponseEntity(
                    createMessage("rest.modules.instances.instance.created", instance.name, module.signature)))

              case NotCreated(validationErrors) =>
                BadRequestRestResponse(
                  MessageResponseEntity(
                    createMessageWithErrors(
                      "rest.modules.instances.instance.cannot.create.incorrect.parameters",
                      validationErrors)))
            }
          } else {
            BadRequestRestResponse(
              MessageResponseEntity(
                createMessageWithErrors("rest.modules.instances.instance.cannot.create", errors)))
          }

        case Failure(exception: JsonDeserializationException) =>
          val error = jsonDeserializationErrorMessageCreator(exception)
          BadRequestRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.instances.instance.cannot.create", error)))

        case Failure(exception) => throw exception
      }
    }
  }

  def get(moduleType: String, moduleName: String, moduleVersion: String, name: String): RestResponse = {
    processInstance(moduleType, moduleName, moduleVersion, name) { instance =>
      OkRestResponse(InstanceResponseEntity(createInstanceApiResponse.from(instance)))
    }
  }

  def getAll: RestResponse = {
    val instances = serviceInterface.getAll.map { instance =>
      ShortInstance(
        instance.name,
        instance.moduleType,
        instance.moduleName,
        instance.moduleVersion,
        instance.description,
        instance.status,
        instance.restAddress.getOrElse(RestLiterals.defaultRestAddress))
    }

    OkRestResponse(ShortInstancesResponseEntity(instances))
  }

  def getByModule(moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    ifModuleExists(moduleType, moduleName, moduleVersion) { _ =>
      val instances = serviceInterface.getByModule(moduleType, moduleName, moduleVersion)
        .map(createInstanceApiResponse.from)
      OkRestResponse(InstancesResponseEntity(instances))
    }
  }

  def delete(moduleType: String, moduleName: String, moduleVersion: String, name: String): RestResponse = {
    ifModuleExists(moduleType, moduleName, moduleVersion) { _ =>
      serviceInterface.delete(name) match {
        case Deleted =>
          OkRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.instances.instance.deleted", name)))

        case WillBeDeleted(instance) =>
          destroyInstance(instance)
          OkRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.instances.instance.deleting", name)))

        case DeletionError(error) =>
          UnprocessableEntityRestResponse(MessageResponseEntity(error))

        case EntityNotFound =>
          NotFoundRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.module.instances.instance.notfound", name)))
      }
    }
  }

  def start(moduleType: String, moduleName: String, moduleVersion: String, name: String): RestResponse = {
    processInstance(moduleType, moduleName, moduleVersion, name) { instance =>
      if (serviceInterface.canStart(instance)) {
        startInstance(instance)
        OkRestResponse(
          MessageResponseEntity(
            createMessage("rest.modules.instances.instance.starting", name)))
      } else {
        UnprocessableEntityRestResponse(
          MessageResponseEntity(
            createMessage("rest.modules.instances.instance.cannot.start", name)))
      }
    }
  }

  def stop(moduleType: String, moduleName: String, moduleVersion: String, name: String): RestResponse = {
    processInstance(moduleType, moduleName, moduleVersion, name) { instance =>
      if (serviceInterface.canStop(instance)) {
        stopInstance(instance)
        OkRestResponse(
          MessageResponseEntity(
            createMessage("rest.modules.instances.instance.stopping", name)))
      } else {
        UnprocessableEntityRestResponse(
          MessageResponseEntity(
            createMessage("rest.modules.instances.instance.cannot.stop", name)))
      }
    }
  }

  def tasks(moduleType: String, moduleName: String, moduleVersion: String, name: String): RestResponse = {
    processInstance(moduleType, moduleName, moduleVersion, name) { instance =>
      var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
        getMessage("rest.modules.instances.instance.cannot.get.tasks")))

      if (instance.restAddress.isDefined && instance.restAddress.get != RestLiterals.defaultRestAddress) {
        val client = inject[HttpClientBuilder].apply(3000)
        val url = new URI(instance.restAddress.get)
        val httpGet = new HttpGet(url.toString)
        val httpResponse = client.execute(httpGet)
        response = OkRestResponse(
          serializer.deserialize[FrameworkRestEntity](EntityUtils.toString(httpResponse.getEntity, "UTF-8")))
        client.close()
      }

      response
    }
  }

  private def ifModuleExists(moduleType: String, moduleName: String, moduleVersion: String)
                            (f: ModuleMetadata => RestResponse): RestResponse = {
    moduleSI.exists(moduleType, moduleName, moduleVersion) match {
      case Right(moduleMetadata) =>
        f(createModuleMetadata.from(moduleMetadata))
      case Left(error) =>
        NotFoundRestResponse(MessageResponseEntity(error))
    }
  }

  private def processInstance(moduleType: String, moduleName: String, moduleVersion: String, name: String)
                             (f: Instance => RestResponse): RestResponse = {
    ifModuleExists(moduleType, moduleName, moduleVersion) { _ =>
      serviceInterface.get(name) match {
        case Some(instance) => f(instance)
        case None =>
          NotFoundRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.module.instances.instance.notfound", name)))
      }
    }
  }

  private def startInstance(instance: Instance) = {
    logger.debug(s"Starting application of instance ${instance.name}.")

    val instanceStarter = inject[InstanceStarterBuilder]
      .apply(instance, settingsUtils.getMarathonConnect(), zkHost, zkPort)
    new Thread(instanceStarter).start()
  }

  private def stopInstance(instance: Instance) = {
    logger.debug(s"Stopping application of instance ${instance.name}.")

    val instanceStopper = inject[InstanceStopperBuilder].apply(instance, settingsUtils.getMarathonConnect())
    new Thread(instanceStopper).start()
  }

  private def destroyInstance(instance: Instance) = {
    logger.debug(s"Destroying application of instance ${instance.name}.")

    val instanceDestroyer = inject[InstanceDestroyerBuilder].apply(instance, settingsUtils.getMarathonConnect())
    new Thread(instanceDestroyer).start()
  }

  private def deserializeInstanceApi(serialized: String, moduleType: String): InstanceApi = moduleType match {
    case EngineLiterals.inputStreamingType =>
      serializer.deserialize[InputInstanceApi](serialized)
    case EngineLiterals.regularStreamingType =>
      serializer.deserialize[RegularInstanceApi](serialized)
    case EngineLiterals.batchStreamingType =>
      serializer.deserialize[BatchInstanceApi](serialized)
    case EngineLiterals.outputStreamingType =>
      serializer.deserialize[OutputInstanceApi](serialized)
    case _ =>
      serializer.deserialize[InstanceApi](serialized)
  }

  private def validateInstance(instance: Instance, specification: Specification) = {
    val validatorClassConfig = s"${ConfigLiterals.systemDomain}.${instance.moduleType}-validator-class"
    val validatorClassName = configService.get(validatorClassConfig) match {
      case Some(configurationSetting) => configurationSetting.value
      case None => throw ConfigSettingNotFound(
        createMessage("rest.config.setting.notfound", validatorClassConfig))
    }
    val validatorClass = Class.forName(validatorClassName)
    val validator = validatorClass.getConstructor(classOf[Injector]).newInstance(injector).asInstanceOf[InstanceValidator]
    validator.validate(instance.asInstanceOf[validator.T], specification)
  }

  private def getZkProperties(): (Option[String], Option[Int]) = {
    val config = ConfigFactory.load()
    val (zkHost, zkPort) = Try {
      (config.getString(CommonAppConfigNames.zooKeeperHost),
        config.getInt(CommonAppConfigNames.zooKeeperPort))
    } match {
      case Success((_zkHost, _zkPort)) =>
        (Some(_zkHost), Some(_zkPort.toInt))

      case Failure(_) =>
        (None, None)
    }

    (zkHost, zkPort)
  }
}
