package com.bwsw.sj.engine.input.connection.tcp.server

import java.util.concurrent.ArrayBlockingQueue

import io.netty.buffer.ByteBuf
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInitializer}
import io.netty.handler.codec.string.StringEncoder

import scala.collection.concurrent

/**
  * A special ChannelInboundHandler which offers an easy way to initialize [[SocketChannel]] once.
  *
  * @param channelContextQueue  queue for keeping a channel context [[ChannelHandlerContext]] to process messages ([[ByteBuf]]) in their turn
  * @param bufferForEachContext map for keeping a buffer containing incoming bytes [[ByteBuf]] with the appropriate channel context [[ChannelHandlerContext]]
  */
class InputStreamingChannelInitializer(channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                                       bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf])
  extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel): Unit = {
    channel.config().setTcpNoDelay(true)
    channel.config().setKeepAlive(true)
    channel.config().setTrafficClass(0x10)
    channel.config().setPerformancePreferences(0, 1, 0)

    val pipeline = channel.pipeline()

    pipeline.addLast("encoder", new StringEncoder())
    pipeline.addLast("handler", new InputStreamingServerHandler(channelContextQueue, bufferForEachContext))
  }
}