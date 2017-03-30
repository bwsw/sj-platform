package com.bwsw.sj.engine.core.output.types.es

import com.bwsw.common.ElasticsearchClient
import com.bwsw.sj.engine.core.output.Entity
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Created by Ivan Kudryavtsev on 05.03.2017.
  */
class ElasticsearchCommandBuilder(transactionFieldName: String, entity: Entity[String]) {

  def buildInsert(transaction: Long, values: Map[String, Any]): String = {
    val mv = entity.getFields.map(f => if (values.contains(f))
      f -> entity.getField(f).transform(values(f))
    else
      f -> entity.getField(f).transform(entity.getField(f).getDefaultValue))

    s"""{"$transactionFieldName": $transaction, """ + mv.map({ case (k: String, v: String) => s""""$k": $v""" }).mkString(", ") + "}"
  }

  def buildDelete(transaction: Long): QueryBuilder = QueryBuilders.matchQuery(transactionFieldName, transaction)

  def buildIndexExists(index: String, client: ElasticsearchClient): Boolean = {
    client.doesIndexExist(index)
  }

  def buildRemove(index: String, streamName: String, transaction: Long, client: ElasticsearchClient) = {
    val query = QueryBuilders.matchQuery("txn", transaction)
    val outputData = client.search(index, streamName, query)
   // outputData.getHits.foreach(hit => client.deleteDocumentByTypeAndId(index, streamName, hit.getId))
  }
}
