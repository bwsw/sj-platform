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
import com.bwsw.tstreams.agents.consumer.ConsumerTransaction
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.agents.group.CheckpointGroup
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Provides method for loading a last state
  *
  * @param manager         manager of environment of regular module task
  * @param checkpointGroup group of t-stream agents that have to make a checkpoint at the same time
  * @param stateStream     stream that keeps a module state
  * @author Pavel Tomskikh
  */
class StateLoader(manager: CommonTaskManager, checkpointGroup: CheckpointGroup, stateStream: TStreamStreamDomain)
  extends StateLoaderInterface {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val partition = 0

  /**
    * Consumer is responsible for retrieving a partial or full state
    */
  private val stateConsumer = manager.createConsumer(stateStream, List(partition, partition), Oldest)
  stateConsumer.start()
  addConsumerToCheckpointGroup()

  /**
    * Provides a serialization from a transaction data to a state variable or state change
    */
  private val serializer: ObjectSerializer = new ObjectSerializer()

  /**
    * @inheritdoc
    */
  override def loadLastState(): (Option[Long], mutable.Map[String, Any]) = {
    logger.debug(s"Restore a state.")
    val initialState = mutable.Map[String, Any]()
    val maybeTxn = stateConsumer.getLastTransaction(partition)
    if (maybeTxn.nonEmpty) {
      logger.debug(s"Get a transaction that was last. It contains a full or partial state.")
      val tempTransaction = maybeTxn.get
      val lastTransaction = stateConsumer.buildTransactionObject(
        tempTransaction.getPartition,
        tempTransaction.getTransactionID,
        tempTransaction.getState,
        tempTransaction.getCount).get //todo fix it next milestone TR1216

      serializer.deserialize(lastTransaction.next()) match {
        case (variable: Any, value: Any) =>
          logger.debug(s"Last transaction contains a full state.")
          val lastFullStateID = Some(lastTransaction.getTransactionID)
          initialState(variable.asInstanceOf[String]) = value
          fillFullState(initialState, lastTransaction)
          (lastFullStateID, initialState)

        case value =>
          logger.debug(s"Last transaction contains a partial state. Start restoring it.")
          val lastFullStateID = Some(Long.unbox(value))
          val lastFullStateTxn = stateConsumer.getTransactionById(partition, lastFullStateID.get).get
          fillFullState(initialState, lastFullStateTxn)
          stateConsumer.setStreamPartitionOffset(partition, lastFullStateID.get)

          var maybeTxn = stateConsumer.getTransaction(partition)
          while (maybeTxn.nonEmpty) {
            val partialState = mutable.Map[String, (String, Any)]()
            val partialStateTxn = maybeTxn.get

            partialStateTxn.next()
            while (partialStateTxn.hasNext) {
              val value = serializer.deserialize(partialStateTxn.next)
              val variable = value.asInstanceOf[(String, (String, Any))]
              partialState(variable._1) = variable._2
            }
            applyPartialChanges(initialState, partialState)
            maybeTxn = stateConsumer.getTransaction(partition)
          }
          logger.debug(s"Restore of state is finished.")
          (lastFullStateID, initialState)
      }
    } else {
      logger.debug(s"There was no one checkpoint of state.")
      (None, initialState)
    }
  }

  /**
    * Allow getting a state by gathering together all data from transaction
    *
    * @param initialState State from which to need start
    * @param transaction  Transaction containing a state
    */
  private def fillFullState(initialState: mutable.Map[String, Any], transaction: ConsumerTransaction): Unit = {
    logger.debug(s"Fill full state.")
    while (transaction.hasNext) {
      val value = serializer.deserialize(transaction.next())
      val variable = value.asInstanceOf[(String, Any)]
      initialState(variable._1) = variable._2
    }
  }

  /**
    * Allows restoring a state consistently applying all partial changes of state
    *
    * @param fullState    Last state that has been saved
    * @param partialState Partial changes of state
    */
  private def applyPartialChanges(fullState: mutable.Map[String, Any],
                                  partialState: mutable.Map[String, (String, Any)]): Unit = {
    logger.debug(s"Apply partial changes to state sequentially.")
    partialState.foreach {
      case (key, ("set", value)) => fullState(key) = value
      case (key, ("delete", _)) => fullState.remove(key)
    }
  }

  /**
    * Adds a state consumer to checkpoint group
    */
  private def addConsumerToCheckpointGroup(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Start adding state consumer to checkpoint group.")
    checkpointGroup.add(stateConsumer)
    logger.debug(s"Task: ${manager.taskName}. Adding state consumer to checkpoint group is finished.")
  }
}

/**
  * Provides method for loading a last state
  */
trait StateLoaderInterface {

  /**
    * Allows getting last state. Needed for restoring after crashing
    *
    * @return (ID of the last state, state variables)
    */
  def loadLastState(): (Option[Long], mutable.Map[String, Any])
}
