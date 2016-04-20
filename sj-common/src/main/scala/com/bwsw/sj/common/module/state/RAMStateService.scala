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
 *
 * @param producer
 * @param consumer
 * *@param stateVariables Provides key/value storage to keeping state
 * *@param stateChanges Provides key/value storage to keeping state changes. It's used for partial checkpoints.
 */

class RAMStateService(producer: BasicProducer[Array[Byte], Array[Byte]],
                      consumer: BasicConsumer[Array[Byte], Array[Byte]]) extends IStateService {

  private var lastFullTxnUUID: UUID = null
  private val serializer: ObjectSerializer = new ObjectSerializer()
  val stateVariables: mutable.Map[String, Any] = loadLastState()
  protected val stateChanges: mutable.Map[String, (String, Any)] = mutable.Map[String, (String, Any)]()

  override def get(key: String): Any = {
    stateVariables(key)
  }

  override def set(key: String, value: Any): Unit = {

    stateVariables(key) = value
  }

  override def delete(key: String): Unit = {
    stateVariables.remove(key)
  }

  override def setChange(key: String, value: Any): Unit = stateChanges(key) = ("set", value)

  override def deleteChange(key: String): Unit = stateChanges(key) = ("delete", stateVariables(key))

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
    val maybeTransaction = consumer.getLastTransaction(0)
    if (maybeTransaction.nonEmpty) {
      val lastTransaction: BasicConsumerTransaction[Array[Byte], Array[Byte]] = maybeTransaction.get
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

