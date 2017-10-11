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
package com.bwsw.sj.mesos.framework

import com.bwsw.common.LeaderLatch
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.FrameworkLiterals
import com.bwsw.sj.mesos.framework.config.FrameworkConfigNames
import com.bwsw.sj.mesos.framework.rest.Rest
import com.bwsw.sj.mesos.framework.schedule.{FrameworkScheduler, FrameworkUtil}
import com.typesafe.config.ConfigFactory
import org.apache.mesos.MesosSchedulerDriver
import org.apache.mesos.Protos.{Credential, FrameworkInfo}
import scaldi.Injectable.inject

import scala.util.Try


object StartFramework {

  import com.bwsw.sj.common.SjModule._

  private val config = ConfigFactory.load()
  val frameworkName = "JugglerFramework"
  val frameworkUser = Try(config.getString(FrameworkConfigNames.user)).getOrElse("root")
  val frameworkCheckpoint = false
  val frameworkFailoverTimeout = 0.0d
  val frameworkRole = "*"

  val master_path = Try(config.getString(FrameworkLiterals.mesosMaster)).getOrElse("zk://127.0.0.1:2181/mesos")
  val zookeeper_host = Try(config.getString(FrameworkLiterals.zookeeperHost)).getOrElse("127.0.0.1")
  val zookeeper_port = Try(config.getString(FrameworkLiterals.zookeeperPort)).getOrElse("2181")
  val frameworkTaskId = Try(config.getString(FrameworkLiterals.frameworkId)).getOrElse("broken")

  val connectionRepository: ConnectionRepository = inject[ConnectionRepository]

  /**
    * Main function to start rest and framework.
    *
    * @param args exposed port for rest service
    */
  def main(args: Array[String]): Unit = {
    val port = if (args.nonEmpty) args(0).toInt else 8080
    Rest.start(port)
    FrameworkUtil.instancePort = Option(port)

    val frameworkInfo = FrameworkInfo.newBuilder.
      setName(frameworkName).
      setUser(frameworkUser).
      setCheckpoint(frameworkCheckpoint).
      setFailoverTimeout(frameworkFailoverTimeout).
      setRole(frameworkRole).
      setPrincipal("sherman").
      build()

    val scheduler = new FrameworkScheduler

    val frameworkPrincipal: Option[ConfigurationSettingDomain] = connectionRepository.getConfigRepository.get(ConfigLiterals.frameworkPrincipalTag)
    val frameworkSecret: Option[ConfigurationSettingDomain] = connectionRepository.getConfigRepository.get(ConfigLiterals.frameworkSecretTag)
    var credential: Option[Credential] = None

    if (frameworkPrincipal.isDefined && frameworkSecret.isDefined) {
      credential = Some(Credential.newBuilder.
        setPrincipal(frameworkPrincipal.get.value).
        setSecret(frameworkSecret.get.value).
        build())
    }

    val driver: MesosSchedulerDriver = {
      if (credential.isDefined) new MesosSchedulerDriver(scheduler, frameworkInfo, master_path, credential.get)
      else new MesosSchedulerDriver(scheduler, frameworkInfo, master_path)
    }

    val zkServers: String = zookeeper_host + ":" + zookeeper_port
    val leader: LeaderLatch = new LeaderLatch(Set(zkServers), s"/framework/$frameworkTaskId/lock")
    leader.start()
    leader.acquireLeadership(5)

    driver.start()
    driver.join()

    leader.close()
    System.exit(0)
  }
}