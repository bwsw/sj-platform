package com.bwsw.sj.common.utils

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import scaldi.Injectable.inject
import scaldi.Injector

import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader

/**
  * Provides method for loading class from file in [[com.bwsw.common.file.utils.FileStorage FileStorage]]
  */
class FileClassLoader {

  /**
    * Loads the class from file in [[com.bwsw.common.file.utils.FileStorage FileStorage]]
    *
    * @param className    name of class
    * @param filename     name of file
    * @param tmpDirectory directory for temporary file saving
    * @return loaded class
    */
  def loadClass(className: String, filename: String, tmpDirectory: String)
               (implicit injector: Injector): Class[_] = {
    val storage = inject[ConnectionRepository].getFileStorage
    val file = storage.get(filename, tmpDirectory + filename)
    val loader = new URLClassLoader(Seq(file.toURI.toURL), ClassLoader.getSystemClassLoader)
    file.delete()
    loader.loadClass(className)
  }
}
