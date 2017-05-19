package com.bwsw.sj.engine.core.batch

import com.bwsw.sj.common.dal.model.instance.BatchInstanceDomain
import com.bwsw.sj.engine.core.entities.Window
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

/**
  * Provides methods to keep windows that consist of batches
  * Use it in [[BatchStreamingExecutor.onWindow()]] to retrieve
  *
  * @param instance set of settings of a batch streaming module
  */
class WindowRepository(instance: BatchInstanceDomain) {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val windowPerStream: mutable.Map[String, Window] = createStorageOfWindows()
  val window: Int = instance.window
  val slidingInterval: Int = instance.slidingInterval

  private def createStorageOfWindows(): mutable.Map[String, Window] = {
    logger.debug("Create a storage to keep windows.")
    mutable.Map(instance.getInputsWithoutStreamMode().map(x => (x, new Window(x))): _*)
  }

  def get(stream: String): Window = {
    logger.debug(s"Get a window for stream: '$stream'.")
    windowPerStream(stream)
  }

  private[engine] def put(stream: String, window: Window): Unit = {
    logger.debug(s"Put a window for stream: '$stream'.")
    windowPerStream(stream) = window
  }

  def getAll(): Map[String, Window] = {
    logger.debug(s"Get all windows.")
    Map(windowPerStream.toList: _*)
  }
}
