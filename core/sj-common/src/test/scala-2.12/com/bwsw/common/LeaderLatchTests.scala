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

import java.util.UUID

import org.apache.curator.CuratorZookeeperClient
import org.apache.curator.retry.RetryOneTime
import org.apache.curator.test.TestingServer
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.data.Stat
import org.scalatest.concurrent.TimeLimitedTests
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Outcome}

import scala.collection.JavaConverters._
import scala.util.Try


/**
  * Tests for [[LeaderLatch]]
  *
  * @author Pavel Tomskikh
  */
class LeaderLatchTests extends FlatSpec with Matchers with BeforeAndAfterAll with TimeLimitedTests {

  val server = new TestingServer(true)
  val connectString = server.getConnectString
  val client = new CuratorZookeeperClient(connectString, 5000, 5000, null, new RetryOneTime(1000))
  client.start()
  client.blockUntilConnectedOrTimedOut()
  val zooKeeper = client.getZooKeeper


  "LeaderLatch" should "create master node on ZK server" in {
    val masterNode = newMasterNode
    val leaderLatchId = UUID.randomUUID().toString

    zooKeeper.exists(masterNode, false) shouldBe null

    new LeaderLatch(Set(connectString), masterNode, leaderLatchId)

    zooKeeper.exists(masterNode, false) should not be null
  }

  it should "not have leadership if it did not started" in {
    val masterNode = newMasterNode
    val leaderLatchId = UUID.randomUUID().toString
    val leaderLatch = new LeaderLatch(Set(connectString), masterNode, leaderLatchId)

    leaderLatch.hasLeadership() shouldBe false
  }

  it should "takes leadership if master node does not have other LeaderLatches" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchId = UUID.randomUUID().toString

    val leaderLatch = new LeaderLatch(Set(connectString), masterNode, leaderLatchId)
    NodeInfo(masterNode, zooKeeper).children shouldBe empty

    leaderLatch.start()
    leaderLatch.takeLeadership(delay)

    val nodeInfo = NodeInfo(masterNode, zooKeeper)
    nodeInfo.children.size shouldBe 1
    nodeInfo.children.head.data shouldBe leaderLatchId
    leaderLatch.hasLeadership() shouldBe true
  }

  it should "gives its ID if it has leadership" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchId = UUID.randomUUID().toString
    val leaderLatch = new LeaderLatch(Set(connectString), masterNode, leaderLatchId)

    leaderLatch.start()
    leaderLatch.takeLeadership(delay)

    leaderLatch.getLeaderInfo() shouldBe leaderLatchId
  }

  it should "delete node from server after closing" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchId = UUID.randomUUID().toString
    val leaderLatch = new LeaderLatch(Set(connectString), masterNode, leaderLatchId)

    leaderLatch.start()
    leaderLatch.takeLeadership(delay)
    leaderLatch.close()

    Thread.sleep(100)
    val nodeInfo = NodeInfo(masterNode, zooKeeper)

    nodeInfo.children.size shouldBe 0
  }


  "LeaderLatch instances" should "give same leader ID" in {
    val masterNode = newMasterNode
    val delay = 10
    val leaderLatchCount = 3
    val leaderLatchIds = Seq.fill(leaderLatchCount)(UUID.randomUUID().toString)

    val leaderLatches = leaderLatchIds.map(id => new LeaderLatch(Set(connectString), masterNode, id))
    leaderLatches.head.start()
    leaderLatches.head.takeLeadership(delay)

    leaderLatches.tail.foreach(_.start())

    val leaderInfo = leaderLatches.map(_.getLeaderInfo()).toSet
    leaderInfo.size shouldBe 1
    leaderInfo.foreach(leaderLatchIds.contains)
  }

  override def afterAll(): Unit = {
    client.close()
    server.close()
  }

  case class NodeInfo(node: String, data: String, stat: Stat, children: Seq[NodeInfo])

  object NodeInfo {
    def apply(node: String, zooKeeper: ZooKeeper): NodeInfo = {
      val stat = new Stat

      new NodeInfo(
        node = node,
        data = new String(zooKeeper.getData(node, null, stat)),
        stat = stat,
        children = zooKeeper.getChildren(node, null).asScala.map(c => apply(s"$node/$c", zooKeeper)))
    }
  }

  override def timeLimit: Span = Span(3, Seconds)

  def newMasterNode: String =
    "/leader-latch/test/" + UUID.randomUUID().toString
}
