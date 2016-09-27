package com.bwsw.sj.engine.input.connection.tcp.server

import java.util.concurrent.ArrayBlockingQueue

import io.netty.buffer.ByteBuf
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInitializer}
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.logging.{LogLevel, LoggingHandler}

import scala.collection.concurrent

/**
 * A special ChannelInboundHandler which offers an easy way to initialize a Channel once.
 * Also a logger is included into channel pipeline
 * @param channelContextQueue Queue for keeping a channel context to process messages (byte buffer) in their turn
 * @param bufferForEachContext Map for keeping a buffer containing incoming bytes with the appropriate channel context
 */
class InputStreamingChannelInitializer(channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                                       bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf])
  extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) = {
    val pipeline = channel.pipeline()

    pipeline.addLast("logger", new LoggingHandler(LogLevel.WARN))
    pipeline.addLast("encoder", new StringEncoder())
    pipeline.addLast("handler", new InputStreamingServerHandler(channelContextQueue, bufferForEachContext))
  }
}