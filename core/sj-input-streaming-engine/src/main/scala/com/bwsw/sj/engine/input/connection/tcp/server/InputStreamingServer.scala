package com.bwsw.sj.engine.input.connection.tcp.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelOption, EventLoopGroup}
import io.netty.handler.logging.{LogLevel, LoggingHandler}


/**
 * Input streaming server that sets up a server listening the specific host and port.
 * Bind and start to accept incoming connections.
 * Than wait until the server socket is closed gracefully shut down the server.
 * @param host Host of server
 * @param port Port of server
 * @param buffer An auxiliary buffer for keeping incoming bytes
 */
class InputStreamingServer(host: String, port: Int, buffer: ByteBuf) {

  def run() = {
    val bossGroup: EventLoopGroup = new NioEventLoopGroup()
    val workerGroup = new NioEventLoopGroup()
    try {
      val bootstrapServer = new ServerBootstrap()
      bootstrapServer.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new InputStreamingChannelInitializer(buffer))
        .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)

      bootstrapServer.bind(host, port).sync().channel().closeFuture().sync()
    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}
