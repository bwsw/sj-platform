package com.bwsw.common.client

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.common.LeaderLatch
import com.bwsw.sj.common.utils.GeneratorLiterals
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.string.StringEncoder

/**
 * Simple tcp client for retrieving transaction ID
 *
 * @author Kseniya Tomskikh
 */

class TcpClient(options: TcpClientOptions) {
  private val out = new ArrayBlockingQueue[ByteBuf](1)
  private var channel: Channel = null
  private val workerGroup = new NioEventLoopGroup()
  private val bootstrap = new Bootstrap()
  private val (host, port) = getMasterAddress()

  createChannel()

  private def getMasterAddress() = {
    val leader = new LeaderLatch(Set(options.zkServers), options.prefix + GeneratorLiterals.masterDirectory)
    val leaderInfo = leader.getLeaderInfo()
    val address = leaderInfo.split(":")
    leader.close()

    (address(0), address(1).toInt)
  }

  private def createChannel() = {
    bootstrap.group(workerGroup)
      .channel(classOf[NioSocketChannel])
      .handler(new TcpClientChannelInitializer(out))

    channel = bootstrap.connect(host, port).sync().channel()
  }

  def get() = {
    channel.writeAndFlush(GeneratorLiterals.messageForServer)
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