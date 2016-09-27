package com.bwsw.common.client

import java.net.{URI, InetSocketAddress}
import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.common.utils.ConfigSettingsUtils
import com.twitter.common.quantity.{Time, Amount}
import com.twitter.common.zookeeper.ZooKeeperClient
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer}
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import org.apache.log4j.Logger

/**
 * Simple tcp client for retrieving transaction ID
 *
 * @author Kseniya Tomskikh
 */

class TcpClient(options: TcpClientOptions) {
  private val out = new ArrayBlockingQueue[Long](1000)
  private val in = new ArrayBlockingQueue[Byte](1)

  startClient()

  private def startClient() = {
    new Thread(new Client(in, out, options)).start()
  }

  def get() = {
    in.put(1)
    out.take()
  }
}

class Client(in: ArrayBlockingQueue[Byte], out: ArrayBlockingQueue[Long], options: TcpClientOptions) extends Runnable {
  private val logger = Logger.getLogger(getClass)
  private val messageForServer = "get"
  private val zkSessionTimeout = ConfigSettingsUtils.getZkSessionTimeout()
  private val zkClient = createZooKeeperClient()

  private def createZooKeeperClient() = {
    val zkServers = createZooKeeperServers()
    new ZooKeeperClient(Amount.of(zkSessionTimeout, Time.MILLISECONDS), zkServers)
  }

  private def createZooKeeperServers() = {
    val zooKeeperServers = new java.util.ArrayList[InetSocketAddress]()
    options.zkServers.map(x => (x.split(":")(0), x.split(":")(1).toInt))
      .foreach(zkServer => zooKeeperServers.add(new InetSocketAddress(zkServer._1, zkServer._2)))

    zooKeeperServers
  }

  override def run(): Unit = {
    val master = getMasterServer()
    val workerGroup = new NioEventLoopGroup()
    try {
      val bootstrap = new Bootstrap()
      bootstrap.group(workerGroup)
        .channel(classOf[NioSocketChannel])
        .handler(new TcpClientChannelInitializer(out))

      val channel = bootstrap.connect(master(0), master(1).toInt).sync().channel()

      while (true) {
        in.take()
        channel.writeAndFlush(messageForServer)
      }
    } finally {
      workerGroup.shutdownGracefully()
    }
  }

  private def getMasterServer() = {
    val zkNode = new URI(s"/${options.prefix}/master").normalize()
    val master = new String(zkClient.get().getData(zkNode.toString, null, null), "UTF-8")
    logger.debug(s"Master server address: $master")
    master.split(":")
  }
}

class TcpClientChannelInitializer(out: ArrayBlockingQueue[Long]) extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) = {
    val pipeline = channel.pipeline()

    pipeline.addLast("encoder", new StringEncoder())
    pipeline.addLast("decoder", new StringDecoder())
    pipeline.addLast("handler", new TcpClientChannel(out))
  }
}

@Sharable
class TcpClientChannel(out: ArrayBlockingQueue[Long]) extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    val buffer = msg.asInstanceOf[String]
    val id = buffer.toLong
    out.put(id)
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