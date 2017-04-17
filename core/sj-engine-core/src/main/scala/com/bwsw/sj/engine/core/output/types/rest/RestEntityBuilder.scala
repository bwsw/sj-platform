package com.bwsw.sj.engine.core.output.types.rest

import com.bwsw.sj.engine.core.output.{EntityBuilder, NamedType}

/**
  * @author Pavel Tomskikh
  */
class RestEntityBuilder(m: Map[String, NamedType[Any]] = Map[String, NamedType[Any]]())
  extends EntityBuilder[Any](m)
