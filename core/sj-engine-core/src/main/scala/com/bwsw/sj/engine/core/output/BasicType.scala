package com.bwsw.sj.engine.core.output

trait TransformableType[T] {
  def transform(o: Any): T
}

abstract class NamedType[T](name: String, default: AnyRef) extends TransformableType[T] {
  def getName: String = name

  def getDefaultValue: AnyRef = default
}

/**
  * Basic type of [[Entity]] element
  *
  * @param name    field name
  * @param default default value of this field
  * @tparam RT type of field that will be after transformation
  * @tparam DT type of field (and type of default value too)
  * @author Ivan Kudryavtsev
  */
abstract class BasicType[RT, DT](name: String, default: DT) extends NamedType[RT](name, default.asInstanceOf[AnyRef])


