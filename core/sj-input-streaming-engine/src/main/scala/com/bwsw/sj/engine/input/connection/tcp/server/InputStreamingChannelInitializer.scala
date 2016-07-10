package com.bwsw.sj.engine.input.connection.tcp.server

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}

class InputStreamingChannelInitializer(buffer: ByteBuf) extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) = {
    val pipeline = channel.pipeline()

    pipeline.addLast("logger", new LoggingHandler(LogLevel.WARN))
    pipeline.addLast("handler", new InputStreamingServerHandler(buffer))

  }
}
