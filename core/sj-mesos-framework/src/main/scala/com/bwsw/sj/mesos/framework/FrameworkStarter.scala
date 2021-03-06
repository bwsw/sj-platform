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
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.FrameworkLiterals
import com.bwsw.sj.mesos.framework.config.FrameworkConfigNames
import com.bwsw.sj.mesos.framework.rest.Rest
import com.bwsw.sj.mesos.framework.schedule.{FrameworkScheduler, FrameworkUtil}
import com.typesafe.config.ConfigFactory
import org.apache.mesos.MesosSchedulerDriver
import org.apache.mesos.Protos.{Credential, FrameworkInfo, Status}
import scaldi.Injectable.inject

import scala.util.Try


object FrameworkStarter {

  import com.bwsw.sj.common.SjModule._

  private val config = ConfigFactory.load()
  FrameworkUtil.config = Option(config)
  private val frameworkName = "JugglerFramework"
  private val frameworkUser = Try(config.getString(FrameworkConfigNames.user)).getOrElse("root")
  private val frameworkCheckpoint = false
  private val frameworkFailoverTimeout = 0.0d
  private val frameworkRole = "*"

  private val master_path = Try(config.getString(FrameworkLiterals.mesosMaster)).getOrElse("zk://127.0.0.1:2181/mesos")
  private val zookeeper_host = Try(config.getString(FrameworkLiterals.zookeeperHost)).getOrElse("127.0.0.1")
  private val zookeeper_port = Try(config.getString(FrameworkLiterals.zookeeperPort)).getOrElse("2181")
  private val frameworkTaskId = Try(config.getString(FrameworkLiterals.frameworkId)).getOrElse("broken")

  private val connectionRepository: ConnectionRepository = inject[ConnectionRepository]

  /**
    * Main function to start rest and framework.
    *
    * @param args exposed port for rest service
    */
  def main(args: Array[String]): Unit = {
    val port = if (args.nonEmpty) args(0).toInt else 8080
    Rest.start(port)
    FrameworkUtil.instancePort = Option(port)

    val frameworkPrincipal = connectionRepository.getConfigRepository.get(ConfigLiterals.frameworkPrincipalTag)
    val frameworkSecret = connectionRepository.getConfigRepository.get(ConfigLiterals.frameworkSecretTag)

    val frameworkInfo = FrameworkInfo.newBuilder.
      setName(frameworkName).
      setUser(frameworkUser).
      setCheckpoint(frameworkCheckpoint).
      setFailoverTimeout(frameworkFailoverTimeout).
      setRole(frameworkRole).
      setPrincipal(frameworkPrincipal.map(_.value).getOrElse("sherman")).
      build()

    val scheduler = new FrameworkScheduler

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

    val driverStatus: Status = driver.run()
    val status = if (driverStatus == Status.DRIVER_STOPPED) 0 else 1

    leader.close()
    System.exit(status)
  }
}