package com.bwsw.sj.engine.core.output.types.rest

import com.bwsw.sj.engine.core.output.{EntityBuilder, NamedType}

/**
  * @author Pavel Tomskikh
  */
class RestEntityBuilder(m: Map[String, NamedType[String]] = Map[String, NamedType[String]]())
  extends EntityBuilder[String](m)
