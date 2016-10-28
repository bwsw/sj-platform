package com.bwsw.sj.engine.core.windowed

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.engine.core.entities.Window

import scala.collection.mutable

class WindowRepository(instance: WindowedInstance, inputs: mutable.Map[SjStream, Array[Int]]) {
  private val windowPerStream: mutable.Map[String, Window] = createStorageOfWindows()
  val window = instance.window
  val slidingInterval = instance.slidingInterval

  private def createStorageOfWindows() = {
    inputs.map(x => (x._1.name, new Window(x._1.name)))
  }

  def get(stream: String) = {
    windowPerStream(stream)
  }

  def put(stream: String, window: Window) = {
    windowPerStream(stream) = window
  }

  def getAll() = {
    Map(windowPerStream.toList: _*)
  }
}
