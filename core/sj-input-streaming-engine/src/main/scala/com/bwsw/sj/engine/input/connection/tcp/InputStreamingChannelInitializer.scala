package com.bwsw.sj.engine.input.connection.tcp

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel

class InputStreamingChannelInitializer extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) = {
    val pipeline = channel.pipeline()

    pipeline.addLast("handler", new InputStreamingServerHandler())
  }
}
