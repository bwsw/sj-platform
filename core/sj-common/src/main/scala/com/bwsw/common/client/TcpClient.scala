package com.bwsw.common.client

import java.util.concurrent.ArrayBlockingQueue

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.string.StringEncoder
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.recipes.leader.LeaderLatch
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.KeeperException

import scala.annotation.tailrec

/**
 * Simple tcp client for retrieving transaction ID
 *
 * @author Kseniya Tomskikh
 */

class TcpClient(options: TcpClientOptions) {
  private val messageForServer = "get"
  private val out = new ArrayBlockingQueue[ByteBuf](1)
  private var channel: Channel = null
  private val workerGroup = new NioEventLoopGroup()
  private val bootstrap = new Bootstrap()
  private val curatorClient = createCuratorClient()
  private val (host, port) = getMasterAddress()

  createChannel()

  private def getMasterAddress() = {
    val leader = new LeaderLatch(curatorClient, options.prefix + "/master", "client")
    var leaderInfo = getLeaderId(leader)
    while(leaderInfo == "") {
      leaderInfo = getLeaderId(leader)
      Thread.sleep(50)
    }
    val address = leaderInfo.split(":")
    (address(0), address(1).toInt)
  }

  @tailrec
  private def getLeaderId(leaderLatch: LeaderLatch): String = {
    try {
      leaderLatch.getLeader.getId
    } catch {
      case e: KeeperException =>
        Thread.sleep(50)
        getLeaderId(leaderLatch)
    }
  }

  private def createCuratorClient() = {
    val curatorClient = CuratorFrameworkFactory.newClient(options.zkServers, new ExponentialBackoffRetry(1000, 3))
    curatorClient.start()
    curatorClient.getZookeeperClient.blockUntilConnectedOrTimedOut()
    curatorClient
  }

  private def createChannel() = {
    bootstrap.group(workerGroup)
      .channel(classOf[NioSocketChannel])
      .handler(new TcpClientChannelInitializer(out))

    channel = bootstrap.connect(host, port).sync().channel()
  }

  def get() = {
    channel.writeAndFlush(messageForServer)
    val serializedId = out.take()
    serializedId.readLong()
  }

  def close() = {
    workerGroup.shutdownGracefully()
  }
}

class TcpClientChannelInitializer(out: ArrayBlockingQueue[ByteBuf]) extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) = {
    channel.config().setTcpNoDelay(true)
    channel.config().setKeepAlive(true)
    channel.config().setTrafficClass(0x10)
    channel.config().setPerformancePreferences(0, 1, 0)

    val pipeline = channel.pipeline()

    pipeline.addLast("encoder", new StringEncoder())
    pipeline.addLast("handler", new TcpClientChannel(out))
  }
}

@Sharable
class TcpClientChannel(out: ArrayBlockingQueue[ByteBuf]) extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    val serializedId = msg.asInstanceOf[ByteBuf]
    out.offer(serializedId)
  }

  /**
   * Exception handler that print stack trace and than close the connection when an exception is raised.
   * @param ctx Channel handler context
   * @param cause What has caused an exception
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) = {
    cause.printStackTrace()
    ctx.close()
  }
}