package com.bwsw.sj.engine.input.connection.tcp.server

import java.nio.charset.Charset
import java.util.Date

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

/**
 * Handles a server-side channel.
 */

class InputStreamingServerHandler(buffer: ByteBuf) extends SimpleChannelInboundHandler[ByteBuf] {


  override def channelActive(ctx: ChannelHandlerContext) = {
    ctx.write("Welcome to input streaming module!\r\n")
    ctx.write("It is " + new Date() + " now.\r\n")
    ctx.flush()
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) = {
    println(msg.toString(Charset.forName("UTF-8")) + "_")
    buffer.writeBytes(msg)
    //after that the msg is empty
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

