package com.bwsw.sj.engine.input.connection.tcp.server

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelInitializer}
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.logging.{LogLevel, LoggingHandler}

/**
 * A special ChannelInboundHandler which offers an easy way to initialize a Channel once.
 * Also a logger is included into channel pipeline
 * @param executor Executor of an input streaming module that is defined by a user
 * @param tokenizedMsgQueue Queue for keeping a part of incoming bytes that will become an input envelope with the channel context
 */
class InputStreamingChannelInitializer(executor: InputStreamingExecutor,
                                       tokenizedMsgQueue: ArrayBlockingQueue[(ChannelHandlerContext, ByteBuf)])
  extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) = {
    val pipeline = channel.pipeline()

    pipeline.addLast("logger", new LoggingHandler(LogLevel.WARN))
    pipeline.addLast("encoder", new StringEncoder())
    pipeline.addLast("handler", new InputStreamingServerHandler(executor, tokenizedMsgQueue))
  }
}