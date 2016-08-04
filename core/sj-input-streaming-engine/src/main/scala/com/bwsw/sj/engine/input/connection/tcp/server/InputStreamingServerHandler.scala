package com.bwsw.sj.engine.input.connection.tcp.server

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

import scala.collection.concurrent

/**
 * Handles a server-side channel.
 * It receives a new portion of bytes from the server and puts it in an auxiliary buffer
 * because of a handler should not contain an execution logic of incoming data
 * @param executor Executor of an input streaming module that is defined by a user
 * @param channelContextQueue Queue for keeping a channel context to process messages (byte buffer) in their turn
 * @param bufferForEachContext Map for keeping a buffer containing incoming bytes with the channel context
 */

@Sharable
class InputStreamingServerHandler(executor: InputStreamingExecutor,
                                  channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                                  bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf])
  extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    val message = msg.asInstanceOf[ByteBuf]

    if (bufferForEachContext.contains(ctx)) {
      bufferForEachContext(ctx).writeBytes(message)
    } else {
      bufferForEachContext += ctx -> message
    }

    channelContextQueue.add(ctx)
  }

  /**
   * Exception handler that print stack trace and than close the connection when an exception is raised.
   * @param ctx Channel handler context
   * @param cause What is caused of exception
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) = {
    cause.printStackTrace()
    ctx.close()
  }
}