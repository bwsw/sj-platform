package com.bwsw.sj.engine.core.output.types.jdbc

import java.sql.PreparedStatement

import com.bwsw.sj.engine.core.output.{EntityBuilder, IncompatibleTypeException, NamedType}

/**
  * Created by diryavkin_dn on 06.03.17.
  */
class JdbcEntityBuilder(m: Map[String, NamedType[(PreparedStatement, Int) => Unit]] = Map[String, NamedType[(PreparedStatement, Int) => Unit]]()) extends EntityBuilder[(PreparedStatement, Int) => Unit](m){
  def field[DV](c: JdbcField[DV])= {
    new JdbcEntityBuilder(m + (c.getName -> c.asInstanceOf[NamedType[(PreparedStatement, Int) => Unit]]))
  }

  override def field(c: NamedType[(PreparedStatement, Int) => Unit]): EntityBuilder[(PreparedStatement, Int) => Unit] = throw new IncompatibleTypeException("Use more specific method field. Parent method is locked.")

}