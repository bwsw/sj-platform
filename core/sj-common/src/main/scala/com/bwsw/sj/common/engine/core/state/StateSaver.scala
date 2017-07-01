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
import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewProducerTransactionPolicy
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Provides methods for saving states
  *
  * @param manager         manager of environment of regular module task
  * @param checkpointGroup group of t-stream agents that have to make a checkpoint at the same time
  * @param stateStream     stream that keeps a module state
  * @author Pavel Tomskikh
  */
class StateSaver(manager: CommonTaskManager, checkpointGroup: CheckpointGroup, stateStream: TStreamStreamDomain)
  extends StateSaverInterface {

  /**
    * @inheritdoc
    */
  override var lastFullStateID: Option[Long] = None

  private val logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Producer is responsible for saving a partial changes of state or a full state
    */
  private val stateProducer = manager.createProducer(stateStream)
  addProducerToCheckpointGroup()

  /**
    * Provides a serialization from a transaction data to a state variable or state change
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
    * Does checkpoint of changes of state
    *
    * @param id      ID of transaction for which a changes was applied
    * @param changes state changes
    */
  private def sendChanges(id: Long, changes: mutable.Map[String, (String, Any)]): Unit = {
    logger.debug(s"Save a partial state in t-stream intended for storing/restoring a state.")
    val transaction = stateProducer.newTransaction(NewProducerTransactionPolicy.ErrorIfOpened)
    transaction.send(serializer.serialize(Long.box(id)))
    changes.foreach((x: (String, (String, Any))) => transaction.send(serializer.serialize(x)))
  }

  /**
    * Adds a state producer to checkpoint group
    */
  private def addProducerToCheckpointGroup(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Start adding state producer to checkpoint group.")
    checkpointGroup.add(stateProducer)
    logger.debug(s"Task: ${manager.taskName}. Adding state producer to checkpoint group is finished.")
  }
}

/**
  * Provides methods for storing states
  */
trait StateSaverInterface {

  /**
    * ID of the transaction by which a last full state was been stored
    */
  var lastFullStateID: Option[Long]

  /**
    * Stores changes of state
    *
    * @param stateChanges   key/value storage that keeps changes of state
    * @param stateVariables key/value storage that keeps state
    */
  def savePartialState(stateChanges: mutable.Map[String, (String, Any)], stateVariables: mutable.Map[String, Any]): Unit

  /**
    * Stores a full state
    *
    * @param stateChanges   key/value storage that keeps changes of state
    * @param stateVariables key/value storage that keeps state
    */
  def saveFullState(stateChanges: mutable.Map[String, (String, Any)], stateVariables: mutable.Map[String, Any]): Unit
}
