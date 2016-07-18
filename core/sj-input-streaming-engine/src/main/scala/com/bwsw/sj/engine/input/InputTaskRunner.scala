package com.bwsw.sj.engine.input

import java.util.concurrent.{ExecutorService, Executors}

import com.bwsw.sj.engine.input.connection.tcp.server.InputStreamingServer
import com.bwsw.sj.engine.input.task.InputTaskManager
import com.bwsw.sj.engine.input.task.engine.InputTaskEngineFactory
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.netty.buffer.{ByteBuf, Unpooled}
import org.slf4j.LoggerFactory

/**
 * Object is responsible for running a task of job that launches input module
 * Created: 07/07/2016
 *
 * @author Kseniya Mikhaleva
 */

object InputTaskRunner {

  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {

    val threadFactory = new ThreadFactoryBuilder()
      .setNameFormat("InputTaskRunner-%d")
      .setDaemon(true)
      .build()
    val executorService: ExecutorService = Executors.newFixedThreadPool(2, threadFactory)

    val buffer: ByteBuf = Unpooled.buffer()

    val manager: InputTaskManager = new InputTaskManager()
    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for input module\n")

    val inputTaskEngineFactory = new InputTaskEngineFactory(manager)

    val inputTaskEngine = inputTaskEngineFactory.createInputTaskEngine()

    logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")
    try {
      inputTaskEngine.runModule(executorService, buffer)
    } catch {
      case exception: Exception => {
        exception.printStackTrace()
        executorService.shutdownNow()
        System.exit(-1)
      }
    }

    new InputStreamingServer(manager.entryHost, manager.entryPort, buffer).run()
  }
}
