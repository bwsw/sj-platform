package com.bwsw.sj.engine.core.output.types.es

import com.bwsw.sj.engine.core.output.Entity
import org.elasticsearch.index.query.{MatchQueryBuilder, QueryBuilders}

/**
  * Provides methods for building es query to CRUD data
  *
  * @param transactionFieldName name of transaction field to check data on duplicate
  * @param entity               data
  * @author Ivan Kudryavtsev
  */

class ElasticsearchCommandBuilder(transactionFieldName: String, entity: Entity[String]) {

  def buildInsert(transaction: Long, values: Map[String, Any]): String = {
    val mv = entity.getFields.map(f => if (values.contains(f))
      f -> entity.getField(f).transform(values(f))
    else
      f -> entity.getField(f).transform(entity.getField(f).getDefaultValue))

    s"""{"$transactionFieldName": $transaction, """ + mv.map({ case (k: String, v: String) => s""""$k": $v""" }).mkString(", ") + "}"
  }

  def buildDelete(transaction: Long): MatchQueryBuilder = QueryBuilders.matchQuery(transactionFieldName, transaction)
}
