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
package com.bwsw.sj.engine.output.benchmark.data_checkers

import com.bwsw.sj.common.dal.model.stream.JDBCStreamDomain
import com.bwsw.sj.engine.output.benchmark.DataFactory.{jdbcStreamName, openJdbcConnection, streamService}

import scala.collection.mutable.ArrayBuffer

/**
  * Validates that data in SQL database corresponds to data in input storage
  *
  * @author Pavel Tomskikh
  */
object JDBCDataChecker extends DataChecker {

  /**
    * Returns a data from SQL database
    *
    * @return a data from SQL database
    */
  override def getOutputElements(): ArrayBuffer[(Int, String)] = {
    val jdbcStream: JDBCStreamDomain = streamService.get(jdbcStreamName).get.asInstanceOf[JDBCStreamDomain]

    val jdbcClient = openJdbcConnection(jdbcStream)
    jdbcClient.start()

    val stmt = jdbcClient.createPreparedStatement(s"SELECT value, string_value FROM ${jdbcStream.name}")
    val res = stmt.executeQuery()
    val outputElements = ArrayBuffer.empty[(Int, String)]
    while (res.next())
      outputElements += ((res.getInt(1), res.getString(2)))

    jdbcClient.close()

    outputElements
  }
}

class JDBCDataChecker
