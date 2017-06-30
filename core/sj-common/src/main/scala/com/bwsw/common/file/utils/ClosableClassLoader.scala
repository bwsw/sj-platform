package com.bwsw.common.file.utils

/**
  * Unlike standard ClassLoader, this class support destructor named 'close'.
  */
trait ClosableClassLoader extends ClassLoader {

  /**
    * This method used as destructor of ClassLoader.
    * Call this method after using classloader.
    */
  def close(): Unit
}

