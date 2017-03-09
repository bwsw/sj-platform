package com.bwsw.sj.engine.core.output.types.es

import com.bwsw.sj.engine.core.output.Entity

/**
  * Created by Ivan Kudryavtsev on 05.03.2017.
  */
class ElasticsearchCommandBuilder(transactionFieldName: String, entity: Entity[String]) {
  def buildIndex(transaction: Long, m: Map[String, AnyRef]): String = {
    val mv = entity.getFields().map(f => if (m.contains(f))
      (f -> entity.getField(f).transform(m(f)))
    else
      (f -> entity.getField(f).transform(entity.getField(f).getDefaultValue)))

    s"""{"$transactionFieldName": $transaction, """ + mv.map({ case (k: String, v: String) => s""""$k": $v""" }).mkString(", ") + "}"
  }

  def buildDelete(transaction: Long): String = s"""{"$transactionFieldName": $transaction }"""

}
