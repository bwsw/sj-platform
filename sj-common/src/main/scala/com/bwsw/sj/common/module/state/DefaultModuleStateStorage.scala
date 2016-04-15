package com.bwsw.sj.common.module.state

import java.util.UUID

import com.bwsw.common.ObjectSerializer
import com.bwsw.tstreams.agents.consumer.BasicConsumer
import com.bwsw.tstreams.agents.producer.{BasicProducerTransaction, BasicProducer}

import scala.collection.mutable

/**
 * Сlass representing state of module that keeps in RAM and use t-stream for checkpoints
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 */

class DefaultModuleStateStorage(producer: BasicProducer[Array[Byte], Array[Byte]],
                                consumer: BasicConsumer[Array[Byte], Array[Byte]]) extends ModuleStateStorage {

  override protected var stateVariables: mutable.Map[String, Any] = loadLastState()
  override protected val stateChanges: mutable.Map[String, (String, Any)] = mutable.Map[String, (String, Any)]()

  private var lastFullTxnUUID: UUID = null
  private val serializer: ObjectSerializer = new ObjectSerializer()
  private val middleVariables: mutable.Map[String, Any] = stateVariables.clone()

  private def loadLastState(): mutable.Map[String, Any] = {
    val loadedTransaction: Object = null //consumer gets the newest transaction and deserialize
    if (loadedTransaction.isInstanceOf[mutable.Map[String, Any]]) {
      //lastFullTxnUUID = loadedTransaction.getUUUID()
      //return serializer.deserialize(loadedTransaction.getBytes()).asInstanceOf[mutable.Map[String, Any]]
      loadedTransaction.asInstanceOf[mutable.Map[String, Any]]
    } else {
      val (uuid, partialState) = loadedTransaction.asInstanceOf[(UUID, mutable.Map[String, (String, Any)])]
        val fullState = mutable.Map[String, Any]() //get UUID-transaction
        applyPartialChanges(fullState, partialState) //apply changes in a loop
        fullState
    }
  }

  private def applyPartialChanges(fullState: mutable.Map[String, Any], partialState: mutable.Map[String, (String, Any)]) = {
    partialState.foreach {
      case (key, ("set", value)) => fullState(key) = value
      case (key, ("delete", _)) => fullState.remove(key)
    }
  }

  private def sendState(state: mutable.Map[String, Any]): UUID = {
    val transaction: BasicProducerTransaction[Array[Byte], Array[Byte]] = producer.newTransaction(true)
    transaction.send(serializer.serialize(state))
    transaction.close()
    transaction.getTxnUUID
  }

  private def sendChanges(changes: (UUID, mutable.Map[String, (String, Any)])) = {
    val transaction: BasicProducerTransaction[Array[Byte], Array[Byte]] = producer.newTransaction(true)
    transaction.send(serializer.serialize(changes))
    transaction.close()
  }

  private def fillChanges(variables: mutable.Map[String, Any], newVariables: mutable.Map[String, Any]) = {
    newVariables.filter(x => {
      if (variables.contains(x._1)) {
        val oldValue = variables(x._1)
        x._2 != oldValue
      } else true
    }).foreach(x => stateChanges(x._1) = ("set", x._2))

    variables.filter(x => !newVariables.contains(x._1)).foreach(x => stateChanges(x._1) = ("delete", x._2))
  }

  override def getState() = middleVariables

  override def checkpoint(): Unit = {
    if (stateChanges.nonEmpty) {
      fillChanges(stateVariables, middleVariables)
      sendChanges((lastFullTxnUUID, stateChanges))
      stateVariables = middleVariables.clone()
      stateChanges.clear()
    }
  }

  override def fullCheckpoint(): Unit = {
    lastFullTxnUUID = sendState(middleVariables)
    stateVariables = middleVariables.clone()
    stateChanges.clear()
  }
}

