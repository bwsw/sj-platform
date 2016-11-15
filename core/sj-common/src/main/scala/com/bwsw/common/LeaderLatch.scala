package com.bwsw.common

import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.leader
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.KeeperException

import scala.annotation.tailrec

class LeaderLatch(zkServers: Set[String], masterNode: String, id: String = "") {
  private val servers = zkServers.mkString(",")
  private val curatorClient = createCuratorClient()
  createMasterNode()
  private val leaderLatch = new leader.LeaderLatch(curatorClient, masterNode, id)
  private var isStarted = false

  private def createCuratorClient() = {
    val curatorClient = CuratorFrameworkFactory.newClient(servers, new ExponentialBackoffRetry(1000, 3))
    curatorClient.start()
    curatorClient.getZookeeperClient.blockUntilConnectedOrTimedOut()
    curatorClient
  }

  private def createMasterNode() = {
    val doesPathExist = Option(curatorClient.checkExists().forPath(masterNode))
    if (doesPathExist.isEmpty) curatorClient.create.creatingParentsIfNeeded().forPath(masterNode)
  }

  def start() = {
    leaderLatch.start()
    isStarted = true
  }

  def takeLeadership(delay: Long) = {
    while (!hasLeadership) {
      Thread.sleep(delay)
    }
  }

  def getLeaderInfo() = {
    var leaderInfo = getLeaderId()
    while (leaderInfo == "") {
      leaderInfo = getLeaderId()
      Thread.sleep(50)
    }

    leaderInfo
  }

  @tailrec
  private def getLeaderId(): String = {
    try {
      leaderLatch.getLeader.getId
    } catch {
      case e: KeeperException =>
        Thread.sleep(50)
        getLeaderId()
    }
  }

  def hasLeadership() = {
    leaderLatch.hasLeadership
  }

  def close() = {
    if (isStarted) leaderLatch.close()
    curatorClient.close()
  }
}
