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

import java.io.Closeable
import java.util.concurrent.{ArrayBlockingQueue, Callable}

import com.typesafe.scalalogging.Logger
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelOption, EventLoopGroup}
import io.netty.handler.logging.{LogLevel, LoggingHandler}

import scala.collection.concurrent
import scala.util.{Failure, Success, Try}


/**
  * Input streaming server that sets up a server listening the specific host and port.
  * Bind and start to accept incoming connections.
  * Than wait until the server socket is closed gracefully shut down the server.
  *
  * @param host                host of server
  * @param port                port of server
  * @param channelContextQueue queue for keeping a channel context [[io.netty.channel.ChannelHandlerContext]]
  *                            to process messages ([[io.netty.buffer.ByteBuf]]) in their turn
  * @param stateByContext      map for keeping a state of a channel context
  *                            [[com.bwsw.sj.engine.input.connection.tcp.server.ChannelHandlerContextState]]
  *                            with the appropriate channel context [[io.netty.channel.ChannelHandlerContext]]
  */
class InputStreamingServer(host: String,
                           port: Int,
                           channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                           stateByContext: concurrent.Map[ChannelHandlerContext, ChannelHandlerContextState])
  extends Callable[Unit] with Closeable {

  private val logger = Logger(this.getClass)
  private val bossGroup: EventLoopGroup = new NioEventLoopGroup(1)
  private val workerGroup = new NioEventLoopGroup()

  override def call(): Unit = {
    logger.info(s"Launch an input-streaming server on: '$host:$port'.")

    val result = Try {
      val bootstrapServer = new ServerBootstrap()
      bootstrapServer.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      bootstrapServer.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new InputStreamingChannelInitializer(channelContextQueue, stateByContext))

      bootstrapServer.bind(host, port).sync().channel().closeFuture().sync()
    }

    result match {
      case Success(_) =>
      case Failure(e) =>
        close()
        throw e
    }
  }

  override def close(): Unit = {
    logger.info(s"Shutdown an input-streaming server (address: '$host:$port').")
    workerGroup.shutdownGracefully()
    bossGroup.shutdownGracefully()
  }
}