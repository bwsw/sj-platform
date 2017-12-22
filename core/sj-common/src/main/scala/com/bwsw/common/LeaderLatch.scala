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
package com.bwsw.common

import com.typesafe.scalalogging.Logger
import org.apache.curator.framework.recipes.leader
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.KeeperException

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/**
  * Wrapper for [[org.apache.curator.framework.recipes.leader.LeaderLatch]]
  * Used to:
  * 1) start only one module ([[com.bwsw.sj.common.dal.model.instance.InstanceDomain]]) at the time
  * 2) in [[com.bwsw.sj.common.utils.EngineLiterals.batchStreamingType]] engine to wait for all instance tasks
  * until they finish (as a barrier)
  *
  * @param zkServers  zk address
  * @param masterNode zk math that will be latched
  * @param id         unique id
  */
class LeaderLatch(zkServers: Set[String], masterNode: String, id: String = "") {
  private val logger = Logger(this.getClass)
  private val servers = zkServers.mkString(",")
  private val curatorClient = createCuratorClient()
  createMasterNode()
  private val leaderLatch = new leader.LeaderLatch(curatorClient, masterNode, id)
  private var isStarted = false

  private def createCuratorClient(): CuratorFramework = {
    logger.debug(s"Create a curator client (connection: $servers).")
    val curatorClient = CuratorFrameworkFactory.newClient(servers, new ExponentialBackoffRetry(1000, 3))
    curatorClient.start()
    curatorClient.getZookeeperClient.blockUntilConnectedOrTimedOut()
    curatorClient
  }

  private def createMasterNode(): Any = {
    logger.debug(s"Create a master node: $masterNode if it doesn't exist.")
    val doesPathExist = Option(curatorClient.checkExists().forPath(masterNode))
    if (doesPathExist.isEmpty) curatorClient.create.creatingParentsIfNeeded().forPath(masterNode)
  }

  def start(): Unit = {
    logger.info("Start a leader latch.")
    leaderLatch.start()
    isStarted = true
  }

  def acquireLeadership(delay: Long): Unit = {
    while (!hasLeadership) {
      logger.debug("Waiting until the leader latch acquires leadership.")
      Thread.sleep(delay)
    }
  }

  def getLeaderInfo(): String = {
    logger.debug("Get info of a leader.")
    var leaderInfo = getLeaderId()
    while (leaderInfo == "") {
      logger.debug("Waiting until the leader latch gets a leader info.")
      leaderInfo = getLeaderId()
      Thread.sleep(50)
    }

    leaderInfo
  }

  @tailrec
  private def getLeaderId(): String = {
    logger.debug("Try to get a leader id.")
    Try(leaderLatch.getLeader.getId) match {
      case Success(leaderId) => leaderId
      case Failure(_: KeeperException) =>
        logger.debug("Waiting until the leader latch gets a leader id.")
        Thread.sleep(50)
        getLeaderId()
      case Failure(e) => throw e
    }
  }

  def hasLeadership(): Boolean = {
    leaderLatch.hasLeadership
  }

  def close(): Unit = {
    logger.info("Close a leader latch if it's started.")
    if (isStarted) leaderLatch.close()
    curatorClient.close()
  }
}
