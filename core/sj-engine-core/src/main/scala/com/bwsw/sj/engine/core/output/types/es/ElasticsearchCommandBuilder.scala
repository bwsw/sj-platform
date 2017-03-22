package com.bwsw.sj.engine.core.output.types.es

import com.bwsw.common.{ElasticsearchClient, JsonSerializer}
import com.bwsw.sj.engine.core.output.Entity
import org.elasticsearch.index.query.QueryBuilders

/**
  * Created by Ivan Kudryavtsev on 05.03.2017.
  */
class ElasticsearchCommandBuilder(transactionFieldName: String, entity: Entity[String]) {
  private val jsonSerializer = new JsonSerializer()

  def buildInsert(transaction: Long, m: Map[String, AnyRef]): String = {
    val mv = entity.getFields.map(f => if (m.contains(f))
      f -> entity.getField(f).transform(m(f))
    else
      f -> entity.getField(f).transform(entity.getField(f).getDefaultValue))

    s"""{"$transactionFieldName": $transaction, """ + mv.map({ case (k: String, v: String) => s""""$k": $v""" }).mkString(", ") + "}"
  }

  def buildDelete(transaction: Long): String = s"""{"$transactionFieldName": $transaction }"""

//  def buildIndex(index: String, streamName: String, client: ElasticsearchClient) = {
//    val fields = entity.getFieldsMap
//    client.createMapping(index, streamName, jsonSerializer.serialize(fields))
//  }

  def buildIndexExists(index: String, client: ElasticsearchClient): Boolean = {
    client.doesIndexExist(index)
  }

  def buildRemove(index: String, streamName: String, transaction: Long, client: ElasticsearchClient) = {
    val query = QueryBuilders.matchQuery("txn", transaction)
    val outputData = client.search(index, streamName, query)
    outputData.getHits.foreach(hit => client.deleteDocumentByTypeAndId(index, streamName, hit.getId))
  }
}
