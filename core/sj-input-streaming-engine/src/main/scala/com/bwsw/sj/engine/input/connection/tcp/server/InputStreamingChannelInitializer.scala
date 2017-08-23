/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.engine.input.connection.tcp.server

import java.util.concurrent.ArrayBlockingQueue

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInitializer}
import io.netty.handler.codec.string.StringEncoder

import scala.collection.concurrent

/**
  * A special ChannelInboundHandler which offers an easy way to initialize [[io.netty.channel.socket.SocketChannel]] once.
  *
  * @param channelContextQueue  queue for keeping a channel context [[io.netty.channel.ChannelHandlerContext]] to process
  *                             messages ([[io.netty.buffer.ByteBuf]]) in their turn
  * @param bufferForEachContext map for keeping a buffer containing incoming bytes [[io.netty.buffer.ByteBuf]]
  *                             with the appropriate channel context [[io.netty.channel.ChannelHandlerContext]]
  */
class InputStreamingChannelInitializer(channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                                       bufferForEachContext: concurrent.Map[ChannelHandlerContext, ChannelContextState])
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