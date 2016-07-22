package com.bwsw.sj.engine.input.connection.tcp.server

import java.util.concurrent.{ArrayBlockingQueue, ConcurrentHashMap}

import com.bwsw.sj.engine.core.input.InputStreamingExecutor
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

/**
 * Handles a server-side channel.
 * It receives a new portion of bytes from the server and puts it in an auxiliary buffer
 * because of a handler should not contain an execution logic of incoming data
 * @param executor Executor of an input streaming module that is defined by a user
 * @param tokenizedMsgQueue Queue for keeping a part of incoming bytes that will become an input envelope with the channel context
 */

@Sharable
class InputStreamingServerHandler(executor: InputStreamingExecutor,
                                  tokenizedMsgQueue: ArrayBlockingQueue[(ChannelHandlerContext, ByteBuf)])
  extends ChannelInboundHandlerAdapter {

  private val bufferForEachContext = new ConcurrentHashMap[ChannelHandlerContext, ByteBuf]()

  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    val message = msg.asInstanceOf[ByteBuf]

    if (bufferForEachContext.contains(ctx)) {
      bufferForEachContext.get(ctx).writeBytes(message)
    } else {
      bufferForEachContext.put(ctx, message)
    }

    val currentBuffer = bufferForEachContext.get(ctx)
    val maybeInterval = executor.tokenize(currentBuffer)

    if (maybeInterval.isDefined) {
      val interval = maybeInterval.get
      tokenizedMsgQueue.add((ctx, currentBuffer.slice(interval.initialValue, interval.finalValue)))
      clearBufferAfterTokenize(currentBuffer, interval.finalValue)
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

  /**
   * Removes the read bytes of the byte buffer
   * @param buffer A buffer for keeping incoming bytes
   * @param endReadingIndex Index that was last at reading
   */
  private def clearBufferAfterTokenize(buffer: ByteBuf, endReadingIndex: Int) = {
    buffer.readerIndex(endReadingIndex + 1)
    buffer.discardReadBytes()
  }
}