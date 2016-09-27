package com.bwsw.common.client

import java.util.concurrent.ArrayBlockingQueue

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer}
import io.netty.handler.codec.string.{StringDecoder, StringEncoder}

/**
 * Simple tcp client for retrieving transaction ID
 *
 * @author Kseniya Tomskikh
 */

class TcpClient(options: TcpClientOptions) {
  private val out = new ArrayBlockingQueue[Long](1000)
  private val in = new ArrayBlockingQueue[Byte](1)

  startClient()

  private def startClient() = {
    new Thread(new Client(in, out, options)).start()
  }

  def get() = {
    in.put(1)
    out.take()
  }
}

class Client(in: ArrayBlockingQueue[Byte], out: ArrayBlockingQueue[Long], options: TcpClientOptions) extends Runnable {
  override def run(): Unit = {
    val workerGroup = new NioEventLoopGroup()
    try {
      val bootstrap = new Bootstrap()
      bootstrap.group(workerGroup)
        .channel(classOf[NioSocketChannel])
        .handler(new TcpClientChannelInitializer(out))

      val channel = bootstrap.connect(options.host, options.port).sync().channel()

      while (true) {
        in.take()
        channel.writeAndFlush("get")
      }
    } finally {
      workerGroup.shutdownGracefully()
    }
  }
}

class TcpClientChannelInitializer(out: ArrayBlockingQueue[Long]) extends ChannelInitializer[SocketChannel] {

  def initChannel(channel: SocketChannel) = {
    val pipeline = channel.pipeline()

    pipeline.addLast("encoder", new StringEncoder())
    pipeline.addLast("decoder", new StringDecoder())
    pipeline.addLast("handler", new TcpClientChannel(out))
  }
}

@Sharable
class TcpClientChannel(out: ArrayBlockingQueue[Long]) extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: Any) = {
    val buffer = msg.asInstanceOf[String]
    val id = buffer.toLong
    out.put(id)
  }

  /**
   * Exception handler that print stack trace and than close the connection when an exception is raised.
   * @param ctx Channel handler context
   * @param cause What has caused an exception
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) = {
    cause.printStackTrace()
    ctx.close()
  }
}