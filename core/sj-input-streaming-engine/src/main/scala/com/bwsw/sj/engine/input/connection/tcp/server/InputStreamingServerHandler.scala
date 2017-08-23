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

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil

import scala.collection.concurrent
import scala.util.{Failure, Success, Try}

/**
  * Handles a server-side channel.
  * It receives a new portion of bytes from the server and puts it in an auxiliary buffer
  * because of a handler should not contain an execution logic of incoming data to avoid locks
  *
  * @param channelContextQueue  queue for keeping a channel context [[io.netty.channel.ChannelHandlerContext]]
  *                             to process messages ([[io.netty.buffer.ByteBuf]]) in their turn
  * @param bufferForEachContext map for keeping a buffer containing incoming bytes [[io.netty.buffer.ByteBuf]]
  *                             with the appropriate channel context [[io.netty.channel.ChannelHandlerContext]]
  */

@Sharable
class InputStreamingServerHandler(channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                                  bufferForEachContext: concurrent.Map[ChannelHandlerContext, ChannelContextState])
  extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    val result = Try {
      val message = msg.asInstanceOf[ByteBuf]
      var maybeState = bufferForEachContext.get(ctx)

      maybeState match {
        case Some(state) =>
          state.buffer.writeBytes(message)

        case None =>
          maybeState = Some(ChannelContextState(ctx.alloc().buffer().writeBytes(message)))
          bufferForEachContext += ctx -> maybeState.get
      }

      if (!maybeState.get.isQueued) {
        channelContextQueue.add(ctx)
        maybeState.get.isQueued = true
      }
    }
    ReferenceCountUtil.release(msg)
    result match {
      case Success(_) =>
      case Failure(e) => throw e
    }
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    bufferForEachContext.get(ctx) match {
      case Some(state) =>
        state.isActive = false
        if (!state.buffer.isReadable)
          bufferForEachContext -= ctx

      case None =>
    }

    super.channelInactive(ctx)
  }

  /**
    * Exception handler that print stack trace and than close the connection when an exception is raised.
    *
    * @param ctx   channel handler context
    * @param cause what has caused an exception
    */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    bufferForEachContext -= ctx
    cause.printStackTrace()
    ctx.close()
  }
}