package com.bwsw.sj.common.module.state

import java.util.UUID

import com.bwsw.common.ObjectSerializer
import com.bwsw.tstreams.agents.consumer.{BasicConsumer, BasicConsumerTransaction}
import com.bwsw.tstreams.agents.producer.{BasicProducer, ProducerPolicies}

import scala.collection.mutable

/**
 * Сlass representing storage for default state that keeps in RAM and use t-stream for checkpoints
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

class DefaultModuleStateStorage(producer: BasicProducer[Array[Byte], Array[Byte]],
                                consumer: BasicConsumer[Array[Byte], Array[Byte]]) extends ModuleStateStorage {

  override protected val stateVariables: mutable.Map[String, Any] = loadLastState()
  override protected val stateChanges: mutable.Map[String, (String, Any)] = mutable.Map[String, (String, Any)]()

  private var lastFullTxnUUID: UUID = null
  private val serializer: ObjectSerializer = new ObjectSerializer()
  private val state = new ModuleState(stateVariables, stateChanges)

  override def getState: State = state

  override def checkpoint(): Unit = {
    if (stateChanges.nonEmpty) {
      sendChanges((lastFullTxnUUID, stateChanges))
      stateChanges.clear()
    }
  }

  override def fullCheckpoint(): Unit = {
    lastFullTxnUUID = sendState(stateVariables)
    stateChanges.clear()
  }

  private def loadLastState(): mutable.Map[String, Any] = {
    val maybeTransaction = consumer.getTransaction
    if (maybeTransaction.nonEmpty) {
      val lastTransaction: BasicConsumerTransaction[Array[Byte], Array[Byte]] = null //consumer gets the newest transaction
      val stateData = serializer.deserialize(lastTransaction.next())
      if (stateData.isInstanceOf[mutable.Map[String, Any]]) {
        lastFullTxnUUID = lastTransaction.getTxnUUID

        stateData.asInstanceOf[mutable.Map[String, Any]]
      } else {
        val (uuid, _) = stateData.asInstanceOf[(UUID, Any)]
        val lastFullStateTransaction = consumer.getTransactionById(0, uuid).get
        val fullState = serializer.deserialize(lastFullStateTransaction.next()).asInstanceOf[mutable.Map[String, Any]]
        consumer.setLocalOffset(0, lastFullStateTransaction.getTxnUUID)

        var maybeTransaction = consumer.getTransaction
        while (maybeTransaction.nonEmpty) {
          val partialState = serializer.deserialize(maybeTransaction.get.next()).asInstanceOf[mutable.Map[String, (String, Any)]]
          applyPartialChanges(fullState, partialState)
          maybeTransaction = consumer.getTransaction
        }

        fullState
      }
    } else {
      val initialState = mutable.Map[String, Any]()
      lastFullTxnUUID = sendState(initialState)

      initialState
    }
  }

  private def applyPartialChanges(fullState: mutable.Map[String, Any], partialState: mutable.Map[String, (String, Any)]) = {
    partialState.foreach {
      case (key, ("set", value)) => fullState(key) = value
      case (key, ("delete", _)) => fullState.remove(key)
    }
  }

  private def sendState(state: mutable.Map[String, Any]): UUID = {
    val transaction = producer.newTransaction(ProducerPolicies.errorIfOpen)
    transaction.send(serializer.serialize(state))
    transaction.close()
    transaction.getTxnUUID
  }

  private def sendChanges(changes: (UUID, mutable.Map[String, (String, Any)])) = {
    val transaction = producer.newTransaction(ProducerPolicies.errorIfOpen)
    transaction.send(serializer.serialize(changes))
    transaction.close()
  }
}

