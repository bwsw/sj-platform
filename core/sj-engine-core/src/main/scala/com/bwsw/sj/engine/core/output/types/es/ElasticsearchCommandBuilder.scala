/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.engine.core.output.types.es

import com.bwsw.sj.engine.core.output.Entity
import com.bwsw.sj.engine.core.output.types.CommandBuilder
import org.elasticsearch.index.query.QueryBuilders

/**
  * Provides methods for building es query to CRUD data
  *
  * @param transactionFieldName name of transaction field to check data on duplicate
  * @param entity               data
  * @author Ivan Kudryavtsev
  */

class ElasticsearchCommandBuilder(transactionFieldName: String, entity: Entity[String]) extends CommandBuilder[String] {

  /**
    * @inheritdoc
    */
  override def buildInsert(transaction: Long, values: Map[String, Any]): String = {
    val mv = entity.getFields.map(f => if (values.contains(f))
      f -> entity.getField(f).transform(values(f))
    else
      f -> entity.getField(f).transform(entity.getField(f).getDefaultValue))

    s"""{"$transactionFieldName": $transaction, """ + mv.map({ case (k: String, v: String) => s""""$k": $v""" }).mkString(", ") + "}"
  }

  /**
    * @inheritdoc
    */
  override def buildDelete(transaction: Long): String = QueryBuilders.matchQuery(transactionFieldName, transaction).toString
}