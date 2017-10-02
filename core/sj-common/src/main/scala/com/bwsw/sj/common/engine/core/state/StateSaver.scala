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
package com.bwsw.sj.common.engine.core.state

import com.bwsw.common.ObjectSerializer
import com.bwsw.tstreams.agents.producer.{NewProducerTransactionPolicy, Producer}
import com.typesafe.scalalogging.Logger

import scala.collection.mutable

/**
  * Provides methods for saving states
  *
  * @param stateProducer t-stream producer for saving states
  * @author Pavel Tomskikh
  */
class StateSaver(stateProducer: Producer) extends StateSaverInterface {

  override var lastFullStateID: Option[Long] = None

  private val logger = Logger(this.getClass)

  /**
    * Provides a serialization from a transaction data to state variables or state changes
    */
  private val serializer: ObjectSerializer = new ObjectSerializer()

  /**
    * @inheritdoc
    */
  override def savePartialState(stateChanges: mutable.Map[String, (String, Any)],
                                stateVariables: mutable.Map[String, Any]): Unit = {
    if (stateChanges.nonEmpty) {
      lastFullStateID match {
        case Some(id) => sendChanges(id, stateChanges)
        case None => saveFullState(stateChanges, stateVariables)
      }
    }
  }

  /**
    * @inheritdoc
    */
  override def saveFullState(stateChanges: mutable.Map[String, (String, Any)],
                             stateVariables: mutable.Map[String, Any]): Unit = {
    if (stateVariables.nonEmpty) {
      logger.debug(s"Save a full state in t-stream intended for storing/restoring a state.")
      val transaction = stateProducer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened)
      stateVariables.foreach((x: (String, Any)) => transaction.send(serializer.serialize(x)))
      lastFullStateID = Some(transaction.getTransactionID)
    }
  }

  /**
    * Saves the changes of state
    *
    * @param id      ID of transaction for which the changes has been applied
    * @param changes state changes
    */
  private def sendChanges(id: Long, changes: mutable.Map[String, (String, Any)]): Unit = {
    logger.debug(s"Save a partial state in t-stream for storing/restoring a state.")
    val transaction = stateProducer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened)
    transaction.send(serializer.serialize(Long.box(id)))
    changes.foreach((x: (String, (String, Any))) => transaction.send(serializer.serialize(x)))
  }
}

/**
  * Provides methods to save a state
  */
trait StateSaverInterface {

  /**
    * ID of the transaction by which a last full state was been stored
    */
  var lastFullStateID: Option[Long]

  /**
    * Saves the changes of state
    *
    * @param stateChanges   key/value storage that keeps changes of state
    * @param stateVariables key/value storage that keeps state
    */
  def savePartialState(stateChanges: mutable.Map[String, (String, Any)], stateVariables: mutable.Map[String, Any]): Unit

  /**
    * Saves a full state
    *
    * @param stateChanges   key/value storage that keeps changes of state
    * @param stateVariables key/value storage that keeps state
    */
  def saveFullState(stateChanges: mutable.Map[String, (String, Any)], stateVariables: mutable.Map[String, Any]): Unit
}
