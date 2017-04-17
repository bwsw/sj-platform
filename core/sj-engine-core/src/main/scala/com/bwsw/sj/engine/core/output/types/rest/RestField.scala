package com.bwsw.sj.engine.core.output.types.rest

import com.bwsw.sj.engine.core.output.NamedType

/**
  * @author Pavel Tomskikh
  */
class RestField(name: String) extends NamedType[Any](name, null) {
  override def transform(value: Any) = value
}
