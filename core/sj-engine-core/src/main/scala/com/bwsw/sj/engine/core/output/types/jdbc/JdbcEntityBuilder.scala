package com.bwsw.sj.engine.core.output.types.jdbc

import com.bwsw.sj.engine.core.output.{EntityBuilder, IncompatibleTypeException, NamedType}

/**
  * Created by diryavkin_dn on 06.03.17.
  */
class JdbcEntityBuilder[T](m: Map[String, NamedType[T]] = Map[String, NamedType[T]]()) extends EntityBuilder[T](m){
  def field[DV](c: JdbcField[DV])= {
    new JdbcEntityBuilder[T](m + (c.getName -> c.asInstanceOf[NamedType[T]]))
  }

  override def field(c: NamedType[T]): EntityBuilder[T] = throw new IncompatibleTypeException("Use more specific method field. Parent method is locked.")

}