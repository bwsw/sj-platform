package com.bwsw.sj.engine.input.connection.tcp.server

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}


/**
 * Input streaming server that sets up a server listening the specific host and port.
 * Bind and start to accept incoming connections.
 * Than wait until the server socket is closed gracefully shut down the server.
 * @param host Host of server
 * @param port Port of server
 * @param executor Executor of an input streaming module that is defined by a user
 * @param tokenizedMsgQueue Queue for keeping a part of incoming bytes that will become an input envelope with the channel context
 */
class InputStreamingServer(host: String,
                           port: Int,
                           executor: InputStreamingExecutor,
                           tokenizedMsgQueue: ArrayBlockingQueue[(ChannelHandlerContext, ByteBuf)]) {

  def run() = {
    val bossGroup: EventLoopGroup = new NioEventLoopGroup()
    val workerGroup = new NioEventLoopGroup()
    try {
      val bootstrapServer = new ServerBootstrap()
      bootstrapServer.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new InputStreamingChannelInitializer(executor, tokenizedMsgQueue))

      bootstrapServer.bind(host, port).sync().channel().closeFuture().sync()
    } finally {
      workerGroup.shutdownGracefully()
      bossGroup.shutdownGracefully()
    }
  }
}