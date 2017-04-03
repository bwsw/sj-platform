package com.bwsw.sj.engine.core.output.types.es

import com.bwsw.sj.engine.core.output.{NamedType, EntityBuilder, IncompatibleTypeException}

/**
  * Created by Ivan Kudryavtsev on 05.03.2017.
  */
class ElasticsearchEntityBuilder(m: Map[String, NamedType[String]] = Map[String, NamedType[String]]()) extends EntityBuilder[String](m) {
  def field[DV](c: ElasticsearchField[DV]): ElasticsearchEntityBuilder = {
    new ElasticsearchEntityBuilder(m + (c.getName -> c.asInstanceOf[NamedType[String]]))
  }

  override def field(c: NamedType[String]): EntityBuilder[String] = throw new IncompatibleTypeException("Use a more specific method field. Parent method is locked.")
}
