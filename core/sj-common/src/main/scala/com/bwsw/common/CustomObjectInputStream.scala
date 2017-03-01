package com.bwsw.common

import java.io.{InputStream, ObjectInputStream, ObjectStreamClass}

class CustomObjectInputStream(private var classLoader: ClassLoader,
                              in: InputStream)
    extends ObjectInputStream(in) {

  protected override def resolveClass(desc: ObjectStreamClass): Class[_] =
    try {
      val name: String = desc.getName
      Class.forName(name, false, classLoader)
    } catch {
      case _: ClassNotFoundException => super.resolveClass(desc)
    }
}
