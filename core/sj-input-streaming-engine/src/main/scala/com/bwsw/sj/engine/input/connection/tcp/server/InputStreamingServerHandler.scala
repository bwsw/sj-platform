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
  * @param channelContextQueue  queue for keeping a channel context [[ChannelHandlerContext]] to process messages ([[ByteBuf]]) in their turn
  * @param bufferForEachContext map for keeping a buffer containing incoming bytes [[ByteBuf]] with the appropriate channel context [[ChannelHandlerContext]]
  */

@Sharable
class InputStreamingServerHandler(channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                                  bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf])
  extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {
    val result = Try {
      val message = msg.asInstanceOf[ByteBuf]

      if (bufferForEachContext.contains(ctx)) {
        bufferForEachContext(ctx).writeBytes(message)
      } else {
        bufferForEachContext += ctx -> ctx.alloc().buffer().writeBytes(message)
      }

      channelContextQueue.add(ctx)
    }
    ReferenceCountUtil.release(msg)
    result match {
      case Success(_) =>
      case Failure(e) => throw e
    }
  }

  /**
    * Exception handler that print stack trace and than close the connection when an exception is raised.
    *
    * @param ctx   channel handler context
    * @param cause what has caused an exception
    */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }
}