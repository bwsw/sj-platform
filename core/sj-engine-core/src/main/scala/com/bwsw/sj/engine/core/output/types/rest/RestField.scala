package com.bwsw.sj.engine.core.output.types.rest

import com.bwsw.sj.engine.core.output.NamedType

/**
  * @author Pavel Tomskikh
  */
class RestField(name: String, default: String = "") extends NamedType[Any](name, default) {
  override def transform(value: Any): Any = value
}
