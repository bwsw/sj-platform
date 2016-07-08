package com.bwsw.sj.engine.input.connection.tcp

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelOption, EventLoopGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel


/**
 * Input streaming server that sets up a server listening the specific host and port.
 * Bind and start to accept incoming connections.
 * Than wait until the server socket is closed gracefully shut down the server.
 */

class InputStreamingServer(host: String, port: Int) {

  def run() = {
    val bossGroup: EventLoopGroup = new NioEventLoopGroup()
    val workerGroup = new NioEventLoopGroup()
    try {
      val bootstrapServer = new ServerBootstrap()
      bootstrapServer.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .childHandler(new InputStreamingChannelInitializer())
        .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)

      bootstrapServer.bind(host, port).sync().channel().closeFuture().sync()
    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}
