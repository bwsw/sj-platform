package com.bwsw.sj.engine.input.connection.tcp.server

import java.util.concurrent.{ArrayBlockingQueue, Callable}

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import org.slf4j.LoggerFactory

import scala.collection.concurrent


/**
 * Input streaming server that sets up a server listening the specific host and port.
 * Bind and start to accept incoming connections.
 * Than wait until the server socket is closed gracefully shut down the server.
 * @param host Host of server
 * @param port Port of server
 * @param channelContextQueue Queue for keeping a channel context to process messages (byte buffer) in their turn
 * @param bufferForEachContext Map for keeping a buffer containing incoming bytes with the appropriate channel context
 */
class InputStreamingServer(host: String,
                           port: Int,
                           channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                           bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf]) extends Callable[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def call() = {
    logger.info(s"Launch input streaming server on: '$host:$port'\n")
    val bossGroup: EventLoopGroup = new NioEventLoopGroup()
    val workerGroup = new NioEventLoopGroup()
    try {
      val bootstrapServer = new ServerBootstrap()
      bootstrapServer.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new InputStreamingChannelInitializer(channelContextQueue, bufferForEachContext))

      bootstrapServer.bind(host, port).sync().channel().closeFuture().sync()
    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}