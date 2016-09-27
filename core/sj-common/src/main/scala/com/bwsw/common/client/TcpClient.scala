package com.bwsw.common.client

import java.net.{InetSocketAddress, URI}
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.utils.ConfigSettingsUtils
import com.twitter.common.quantity.{Amount, Time}
import com.twitter.common.zookeeper.ZooKeeperClient
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.string.StringEncoder
import org.apache.log4j.Logger

/**
 * Simple tcp client for retrieving transaction ID
 *
 * @author Kseniya Tomskikh
 */

class TcpClient(options: TcpClientOptions) {
  private val logger = Logger.getLogger(getClass)
  private val messageForServer = "get"
  private val out = new ArrayBlockingQueue[ByteBuf](1)
  private var channel: Channel = null
  private val workerGroup = new NioEventLoopGroup()
  private val bootstrap = new Bootstrap()
  private val zkSessionTimeout = ConfigSettingsUtils.getZkSessionTimeout()
  private val zkClient = createZooKeeperClient()
  private val master = getMasterServer()

  createChannel()

  private def createZooKeeperClient() = {
    val zooKeeperServers = createZooKeeperServers()

    new ZooKeeperClient(Amount.of(zkSessionTimeout, Time.MILLISECONDS), zooKeeperServers)
  }

  private def createZooKeeperServers() = {
    val zooKeeperServers = new java.util.ArrayList[InetSocketAddress]()
    options.zkServers.map(x => (x.split(":")(0), x.split(":")(1).toInt))
      .foreach(zkServer => zooKeeperServers.add(new InetSocketAddress(zkServer._1, zkServer._2)))

    zooKeeperServers
  }

  private def getMasterServer() = {
    val zkNode = new URI(s"/${options.prefix}/master").normalize()
    val master = new String(zkClient.get().getData(zkNode.toString, null, null), "UTF-8")
    logger.debug(s"Master server address: $master")
    master.split(":")
  }

  private def createChannel() = {
    bootstrap.group(workerGroup)
      .channel(classOf[NioSocketChannel])
      .handler(new TcpClientChannelInitializer(out))

    channel = bootstrap.connect(master(0), master(1).toInt).sync().channel()
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