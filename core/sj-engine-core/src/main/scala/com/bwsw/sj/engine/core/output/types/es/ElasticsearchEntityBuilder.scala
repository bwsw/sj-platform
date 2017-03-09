package com.bwsw.sj.engine.core.output.types.es

import com.bwsw.sj.engine.core.output.{NamedType, EntityBuilder, IncompatibleTypeException}

/**
  * Created by Ivan Kudryavtsev on 05.03.2017.
  */
class ElasticsearchEntityBuilder[T](m: Map[String, NamedType[T]] = Map[String, NamedType[T]]()) extends EntityBuilder[T](m) {
  def field[DV](c: ElasticsearchField[DV])= {
    new ElasticsearchEntityBuilder[T](m + (c.getName -> c.asInstanceOf[NamedType[T]]))
  }

  override def field(c: NamedType[T]): EntityBuilder[T] = throw new IncompatibleTypeException("Use more specific method field. Parent method is locked.")
}
