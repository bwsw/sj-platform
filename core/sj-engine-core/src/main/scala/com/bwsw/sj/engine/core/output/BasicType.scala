package com.bwsw.sj.engine.core.output

/**
  * Created by Ivan Kudryavtsev on 03.03.2017.
  */


trait TransformableType[T] {
  def transform(o: Any): T
}

abstract class NamedType[T](name: String, default: AnyRef) extends TransformableType[T] {
  def getName: String = name

  def getDefaultValue: AnyRef = default
}

abstract class BasicType[RT, DT](name: String, default: DT) extends NamedType[RT](name, default.asInstanceOf[AnyRef])

class IncompatibleTypeException(msg: String) extends Exception(msg)
