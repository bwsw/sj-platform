package com.bwsw.sj.engine.core.output

/**
  * Created by Ivan Kudryavtsev on 03.03.2017.
  */


trait TransformableType[T] {
  def transform(o: Object): T
}

abstract class NamedType[T](name: String, default: Object) extends TransformableType[T] {
  def getName = name

  def getDefaultValue: Object = default
}

abstract class BasicType[RT, DT](name: String, default: DT) extends NamedType[RT](name, default.asInstanceOf[Object])

class IncompatibleTypeException(msg: String) extends Exception(msg)
