package com.bwsw.common

import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.leader
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.KeeperException
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

class LeaderLatch(zkServers: Set[String], masterNode: String, id: String = "") {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val servers = zkServers.mkString(",")
  private val curatorClient = createCuratorClient()
  createMasterNode()
  private val leaderLatch = new leader.LeaderLatch(curatorClient, masterNode, id)
  private var isStarted = false

  private def createCuratorClient() = {
    logger.debug(s"Create a curator client (connection: $servers).")
    val curatorClient = CuratorFrameworkFactory.newClient(servers, new ExponentialBackoffRetry(1000, 3))
    curatorClient.start()
    curatorClient.getZookeeperClient.blockUntilConnectedOrTimedOut()
    curatorClient
  }

  private def createMasterNode() = {
    logger.debug(s"Create a master node: $masterNode if it doesn't exist.")
    val doesPathExist = Option(curatorClient.checkExists().forPath(masterNode))
    if (doesPathExist.isEmpty) curatorClient.create.creatingParentsIfNeeded().forPath(masterNode)
  }

  def start() = {
    logger.info("Start a leader latch.")
    leaderLatch.start()
    isStarted = true
  }

  def takeLeadership(delay: Long) = {
    logger.debug("Try to start a leader latch.")
    while (!hasLeadership) {
      logger.debug("Waiting until the leader latch takes a leadership.")
      Thread.sleep(delay)
    }
  }

  def getLeaderInfo() = {
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
    try {
      leaderLatch.getLeader.getId
    } catch {
      case e: KeeperException =>
        logger.debug("Waiting until the leader latch gets a leader id.")
        Thread.sleep(50)
        getLeaderId()
    }
  }

  def hasLeadership() = {
    leaderLatch.hasLeadership
  }

  def close() = {
    logger.info("Close a leader latch if it's started.")
    if (isStarted) leaderLatch.close()
    curatorClient.close()
  }
}
