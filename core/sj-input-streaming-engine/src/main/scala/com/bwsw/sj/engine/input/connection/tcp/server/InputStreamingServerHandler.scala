package com.bwsw.sj.engine.input.connection.tcp.server

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

/**
 * Handles a server-side channel.
 * It receives a new portion of bytes from the server and puts it in an auxiliary buffer
 * because of a handler should not contain an execution logic of incoming data
 * @param buffer An auxiliary buffer for keeping incoming bytes
 */

@Sharable
class InputStreamingServerHandler(buffer: ByteBuf) extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    val message = msg.asInstanceOf[ByteBuf]
    val copy = message.copy()
    buffer.writeBytes(copy)
     //after that the msg is empty
    ctx.write(message)
  }

  override def channelReadComplete(ctx: ChannelHandlerContext) {
    ctx.flush()
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