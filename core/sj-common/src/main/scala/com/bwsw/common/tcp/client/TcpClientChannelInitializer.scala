package com.bwsw.common.tcp.client

import java.util.concurrent.ArrayBlockingQueue

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.string.StringEncoder

class TcpClientChannelInitializer(out: ArrayBlockingQueue[Long]) extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) = {
    channel.config().setTcpNoDelay(true)
    channel.config().setKeepAlive(true)
    channel.config().setTrafficClass(0x10)
    channel.config().setPerformancePreferences(0, 1, 0)

    val pipeline = channel.pipeline()

    pipeline.addLast("encoder", new StringEncoder())
    pipeline.addLast("handler", new TcpClientChannel(out))
  }
}
