package com.bwsw.sj.engine.input

import java.util.concurrent.{ExecutorService, Executors}

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.engine.input.connection.tcp.server.InputStreamingServer
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

  def main(args: Array[String]) {

    val logger = LoggerFactory.getLogger(this.getClass)
    val threadFactory = new ThreadFactoryBuilder()
      .setNameFormat("InputTaskRunner-%d")
      .setDaemon(true)
      .build()
    val executorService: ExecutorService = Executors.newFixedThreadPool(1, threadFactory)

    val buffer: ByteBuf = Unpooled.buffer()

    val manager: InputTaskManager = new InputTaskManager()
    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for input module\n")

    val inputInstanceMetadata = manager.getInstanceMetadata

    val inputTaskEngine = createInputTaskEngine(manager, inputInstanceMetadata)

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

    new InputStreamingServer("192.168.1.174", 8888, buffer).run() //    new InputStreamingServer(manager.entryHost, manager.entryPort).run()
  }

  def createInputTaskEngine(manager: InputTaskManager, instance: InputInstance) = {
    instance.checkpointMode match {
      case "time-interval" => new TimeCheckpointInputTaskEngine(manager, instance)
      case "every-nth" => new NumericalCheckpointInputTaskEngine(manager, instance)
    }
  }
}
