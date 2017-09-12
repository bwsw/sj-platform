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
package com.bwsw.sj.engine.batch.task

import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.framework.recipes.barriers.DistributedDoubleBarrier
import org.apache.curator.retry.ExponentialBackoffRetry
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

/**
  * Uses two instances of [[org.apache.curator.framework.recipes.barriers.DistributedDoubleBarrier DistributedDoubleBarrier]]
  * to prevent infinite blocking
  *
  * @param zooKeeperHosts    set of ZooKeeper hosts
  * @param barrierPathPrefix prefix of barrier path
  * @param memberQty         the number of members in the barrier
  * @author Pavel Tomskikh
  */
class TwoBarriers(zooKeeperHosts: Set[String],
                  barrierPathPrefix: String,
                  memberQty: Int) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val barrierPaths = Array(0, 1).map(i => barrierPathPrefix + "-" + i)
  private val currentBarrierIndexPath = barrierPathPrefix + "-current"
  private val curatorClient = createCuratorClient()
  private val doubleBarriers = barrierPaths.map(path => new DistributedDoubleBarrier(curatorClient, path, memberQty))
  private var currentBarrierIndex = getCurrentBarrierIndex

  def enter(): Unit = {
    logger.debug(s"Enter to barrier $currentBarrierIndex")
    doubleBarriers(currentBarrierIndex).enter()
    logger.debug(s"Entered to barrier $currentBarrierIndex")
  }

  def leave(): Unit = {
    logger.debug(s"Leave from barrier $currentBarrierIndex")
    doubleBarriers(currentBarrierIndex).leave()
    logger.debug(s"Left from barrier $currentBarrierIndex")

    if (currentBarrierIndex == 0)
      currentBarrierIndex = 1
    else
      currentBarrierIndex = 0

    updateCurrentBarrierIndex()
  }


  private def getCurrentBarrierIndex: Int = {
    Try(curatorClient.getData.forPath(currentBarrierIndexPath)) match {
      case Success(bytes) =>
        Byte.byte2int(bytes.headOption.getOrElse(0))

      case Failure(_) =>
        Try(curatorClient.create().forPath(currentBarrierIndexPath, Array[Byte](0)))

        0
    }
  }

  private def updateCurrentBarrierIndex(): Unit =
    Try(curatorClient.setData().forPath(currentBarrierIndexPath, Array(currentBarrierIndex.toByte)))

  private def createCuratorClient(): CuratorFramework = {
    val curatorClient = CuratorFrameworkFactory.newClient(zooKeeperHosts.mkString(","), new ExponentialBackoffRetry(1000, 3))
    curatorClient.start()
    curatorClient.getZookeeperClient.blockUntilConnectedOrTimedOut()

    curatorClient
  }
}
