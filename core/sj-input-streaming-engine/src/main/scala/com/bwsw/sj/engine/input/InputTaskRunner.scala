package com.bwsw.sj.engine.input

import java.nio.charset.Charset
import java.util.concurrent.Executors

import com.bwsw.sj.engine.input.connection.tcp.server.InputStreamingServer
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.util.ReferenceCountUtil
import org.slf4j.LoggerFactory

/**
 * Object responsible for running a task of job that launches input module
 * Created: 07/07/2016
 *
 * @author Kseniya Mikhaleva
 */

object InputTaskRunner {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val threadFactory = new ThreadFactoryBuilder()
    .setNameFormat("InputTaskRunner-%d")
    .setDaemon(true)
    .build()
  private val executorService = Executors.newFixedThreadPool(1, threadFactory)

  private val checkpointGroup = new CheckpointGroup()

  private val buffer: ByteBuf = Unpooled.buffer()

  def main(args: Array[String]) {

    //    val manager = new InputTaskManager()
    //
    //    logger.debug(s"Task: ${manager.taskName}. Start creating t-stream producers for each output stream\n")
    //    val producers = manager.createOutputProducers
    //    logger.debug(s"Task: ${manager.taskName}. T-stream producers for each output stream are created\n")
    //
    //    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group\n")
    //    producers.foreach(x => checkpointGroup.add(x._2.name, x._2))
    //    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group\n")
    //
    //    new InputStreamingServer(manager.entryHost, manager.entryPort).run()


    executorService.execute(new Runnable {
      override def run(): Unit = try {
        while (true) {
          if (buffer.isReadable(5)) {
            println(buffer.toString(Charset.forName("UTF-8")) + "_")
            buffer.slice(1, 5)
            buffer.readerIndex(5)
            buffer.discardReadBytes()
            println(buffer.toString(Charset.forName("UTF-8")) + "_")
          } else Thread.sleep(2000)
        }
      } finally {
        ReferenceCountUtil.release(buffer)
      }
    })

    new InputStreamingServer("192.168.1.174", 8888, buffer).run()
  }
}
