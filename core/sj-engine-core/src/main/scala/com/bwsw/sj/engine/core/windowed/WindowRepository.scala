package com.bwsw.sj.engine.core.windowed

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.engine.core.entities.Window
import org.slf4j.LoggerFactory

import scala.collection.mutable

class WindowRepository(instance: WindowedInstance) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val windowPerStream: mutable.Map[String, Window] = createStorageOfWindows()
  val window = instance.window
  val slidingInterval = instance.slidingInterval

  private def createStorageOfWindows() = {
    logger.debug("Create a storage to keep windows.")
    mutable.Map(instance.getInputsWithoutStreamMode().map(x => (x, new Window(x))): _*)
  }

  def get(stream: String) = {
    logger.debug(s"Get a window for stream: '$stream'.")
    windowPerStream(stream)
  }

  def put(stream: String, window: Window) = {
    logger.debug(s"Put a window for stream: '$stream'.")
    windowPerStream(stream) = window
  }

  def getAll() = {
    logger.debug(s"Get all windows.")
    Map(windowPerStream.toList: _*)
  }
}
