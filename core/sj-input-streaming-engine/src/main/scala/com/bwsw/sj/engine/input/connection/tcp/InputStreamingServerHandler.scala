package com.bwsw.sj.engine.input.connection.tcp

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

/**
 * Handles a server-side channel.
 */

class InputStreamingServerHandler extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    val in = msg.asInstanceOf[ByteBuf]
    val i = 3
    try {
      while (in.isReadable) {
        println(in.readBytes(i).toString(io.netty.util.CharsetUtil.US_ASCII))
        in.discardReadBytes()
        println(in.toString(io.netty.util.CharsetUtil.US_ASCII))
      }
    } finally {

    }
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

