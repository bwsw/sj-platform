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
package com.bwsw.sj.crud.rest.instance

import com.bwsw.common.http.HttpClient
import com.bwsw.common.http.HttpStatusChecker._
import com.bwsw.common.marathon.MarathonApi
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.EngineLiterals
import com.typesafe.scalalogging.Logger
import scaldi.Injector

import scala.util.{Failure, Success, Try}

/**
  * One-thread deleting object for instance
  * using synchronous apache http client
  *
  * protected methods and variables need for testing purposes
  *
  * @author Kseniya Tomskikh
  */
class InstanceDestroyer(instance: Instance,
                        marathonAddress: String,
                        marathonUsername: Option[String] = None,
                        marathonPassword: Option[String] = None,
                        delay: Long = 1000,
                        marathonTimeout: Int = 60000)
                       (implicit val injector: Injector) extends Runnable {

  private val logger = Logger(getClass.getName)
  protected val instanceManager = new InstanceDomainRenewer()
  protected val client = new HttpClient(marathonTimeout, marathonUsername, marathonPassword)
  protected val marathonManager = new MarathonApi(client, marathonAddress)
  private val frameworkName = InstanceAdditionalFieldCreator.getFrameworkName(instance)

  import EngineLiterals._

  def run(): Unit = {
    Try {
      logger.info(s"Instance: '${instance.name}'. Destroy an instance.")
      instanceManager.updateInstanceStatus(instance, deleting)
      deleteFramework()
      instanceManager.deleteInstance(instance.name)
      client.close()
    } match {
      case Success(_) =>
        logger.info(s"Instance: '${instance.name}' has been destroyed.")
      case Failure(e) =>
        logger.error(s"Instance: '${instance.name}'. Instance is failed during the destroying process.", e)
        instanceManager.updateInstanceStatus(instance, error)
        client.close()
    }
  }

  protected def deleteFramework(): Unit = {
    logger.debug(s"Instance: '${instance.name}'. Deleting a framework.")
    val response = marathonManager.destroyMarathonApplication(frameworkName)
    if (isStatusOK(response)) {
      instanceManager.updateFrameworkStage(instance, deleting)
      waitForFrameworkToDelete()
    } else {
      if (isStatusNotFound(response)) {
        instanceManager.updateFrameworkStage(instance, deleted)
      } else {
        instanceManager.updateFrameworkStage(instance, error)
        throw new Exception(s"Marathon returns status code: $response " +
          s"during the destroying process of framework. Framework '$frameworkName' is marked as error.")
      }
    }
  }

  protected def waitForFrameworkToDelete(): Unit = {
    var hasDeleted = false
    while (!hasDeleted) {
      logger.debug(s"Instance: '${instance.name}'. Waiting until a framework is deleted.")
      val frameworkApplicationInfo = marathonManager.getApplicationInfo(frameworkName)
      if (!isStatusNotFound(frameworkApplicationInfo)) {
        instanceManager.updateFrameworkStage(instance, deleting)
        Thread.sleep(delay)
      } else {
        instanceManager.updateFrameworkStage(instance, deleted)
        hasDeleted = true
      }
    } //todo will see about it, maybe get stuck implicitly if getApplicationInfo() returns some marathon error statuses
  }
}

class InstanceDestroyerBuilder(implicit val injector: Injector) {
  def apply(instance: Instance,
            marathonAddress: String,
            marathonUsername: Option[String] = None,
            marathonPassword: Option[String] = None,
            delay: Long = 1000,
            marathonTimeout: Int = 60000): InstanceDestroyer =
    new InstanceDestroyer(instance, marathonAddress, marathonUsername, marathonPassword, delay, marathonTimeout)
}
