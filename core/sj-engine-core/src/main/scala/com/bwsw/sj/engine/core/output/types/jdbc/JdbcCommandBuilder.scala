package com.bwsw.sj.engine.core.output.types.jdbc

import java.sql.PreparedStatement

import com.bwsw.sj.engine.core.output.Entity

/**
  * Created by diryavkin_dn on 07.03.17.
  */
class JdbcCommandBuilder(transactionFieldName: String, entity: Entity[(PreparedStatement, Int) => Unit]) {
  def buildInsert(transaction: Long, m: Map[String, AnyRef], preparedStatement: PreparedStatement): PreparedStatement = {
    var t = 0
    val mv = entity.getFields().map(f => if (m.contains(f)) {
      t+=1
      t -> entity.getField(f).transform(m(f))
    }
    else {
      t+=1
      t -> entity.getField(f).transform(entity.getField(f).getDefaultValue)
    }
    )
    t+=1
    mv.foreach( {case (key: Int, value: ((PreparedStatement, Int) => Unit) ) => value.apply(preparedStatement, key)})
    preparedStatement.setLong(t, transaction)
    preparedStatement
  }
  def buildDelete(transaction: Long, preparedStatement: PreparedStatement): PreparedStatement = {
    preparedStatement.setLong(1, transaction)
    preparedStatement
  }

  def getTransactionFieldName = this.transactionFieldName
}