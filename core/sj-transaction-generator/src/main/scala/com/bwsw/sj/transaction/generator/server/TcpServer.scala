package com.bwsw.sj.transaction.generator.server

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import com.bwsw.sj.common.utils.TransactionGeneratorLiterals
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer, EventLoopGroup}
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import org.apache.log4j.Logger

/**
 * Simple tcp server for creating transaction ID
 *
 *
 * @author Kseniya Tomskikh
 */
class TcpServer(host: String, port: Int) {
  private val logger = Logger.getLogger(getClass)

  def listen() = {
    logger.info(s"Launch a tcp server on: '$host:$port'\n")
    val bossGroup: EventLoopGroup = new NioEventLoopGroup()
    val workerGroup = new NioEventLoopGroup()
    try {
      val bootstrapServer = new ServerBootstrap()
      bootstrapServer.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new TcpServerChannelInitializer())

      val future = bootstrapServer.bind(host, port).sync().channel()
      future.closeFuture().sync()
    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}

class TcpServerChannelInitializer() extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) = {
    val pipeline = channel.pipeline()

    pipeline.addLast("encoder", new StringEncoder())
    pipeline.addLast("decoder", new StringDecoder())
    pipeline.addLast("handler", new TransactionGenerator())
  }
}

@Sharable
class TransactionGenerator() extends ChannelInboundHandlerAdapter {
  private val counter = new AtomicInteger(0)
  private val currentMillis = new AtomicLong(0)
  private val scale = TransactionGeneratorLiterals.scale

  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    val a = generateID()
    ctx.writeAndFlush(a.toString)
  }

  private def generateID() = this.synchronized {
    val now = System.currentTimeMillis()
    if (now - currentMillis.get > 0) {
      currentMillis.set(now)
      counter.set(0)
    }
    now * scale + counter.getAndIncrement()
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
