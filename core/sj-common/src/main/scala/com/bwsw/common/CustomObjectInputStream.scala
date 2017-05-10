package com.bwsw.common

import java.io.{InputStream, ObjectInputStream, ObjectStreamClass}

import scala.util.{Failure, Success, Try}

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
