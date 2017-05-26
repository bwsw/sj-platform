package com.bwsw.common

import java.io.{InputStream, ObjectInputStream, ObjectStreamClass}

import scala.util.{Failure, Success, Try}

/**
  * Wrapper for [[ObjectInputStream]] with additional [[ClassLoader]] to load user defined classes from module
  *
  * @param classLoader extended class loader (with classes from module)
  * @param in          an input stream of bytes
  */
class CustomObjectInputStream(private var classLoader: ClassLoader,
                              in: InputStream)
  extends ObjectInputStream(in) {

  protected override def resolveClass(desc: ObjectStreamClass): Class[_] =
    Try {
      val name: String = desc.getName
      Class.forName(name, false, classLoader)
    } match {
      case Success(clazz) => clazz
      case Failure(_: ClassNotFoundException) => super.resolveClass(desc)
      case Failure(e) => throw e
    }
}
