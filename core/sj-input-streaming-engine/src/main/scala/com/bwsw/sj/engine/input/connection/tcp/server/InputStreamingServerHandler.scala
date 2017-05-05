package com.bwsw.sj.engine.input.connection.tcp.server

import java.util.concurrent.ArrayBlockingQueue

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil

import scala.collection.concurrent

/**
 * Handles a server-side channel.
 * It receives a new portion of bytes from the server and puts it in an auxiliary buffer
 * because of a handler should not contain an execution logic of incoming data
 * @param channelContextQueue Queue for keeping a channel context to process messages (byte buffer) in their turn
 * @param bufferForEachContext Map for keeping a buffer containing incoming bytes with the appropriate channel context
 */

@Sharable
class InputStreamingServerHandler(channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                                  bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf])
  extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    try {
      val message = msg.asInstanceOf[ByteBuf]

      if (bufferForEachContext.contains(ctx)) {
        bufferForEachContext(ctx).writeBytes(message)
      } else {
        bufferForEachContext += ctx -> ctx.alloc().buffer().writeBytes(message)
      }

      channelContextQueue.add(ctx)
    } finally {
      ReferenceCountUtil.release(msg)
    }
  }

  /**
   * Exception handler that print stack trace and than close the connection when an exception is raised.
 *
   * @param ctx Channel handler context
   * @param cause What has caused an exception
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}