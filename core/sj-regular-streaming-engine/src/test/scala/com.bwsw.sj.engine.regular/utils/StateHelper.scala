package com.bwsw.sj.engine.regular.utils

import com.bwsw.common.ObjectSerializer
import com.bwsw.tstreams.agents.consumer.{Consumer, ConsumerTransaction}

import scala.collection.mutable

object StateHelper {

  private val partition = 0

  def getState(consumer: Consumer[Array[Byte]], objectSerializer: ObjectSerializer) = {

    val initialState = mutable.Map[String, Any]()
    val tempTransaction = consumer.getLastTransaction(0).get
    val lastTxn = consumer.buildTransactionObject(tempTransaction.getPartition(), tempTransaction.getTransactionID(), tempTransaction.getCount()).get //todo fix it next milestone TR1216
    var value = objectSerializer.deserialize(lastTxn.next())
    value match {
      case variable: (Any, Any) =>
        initialState(variable._1.asInstanceOf[String]) = variable._2
        fillFullState(initialState, lastTxn, objectSerializer)
      case _ =>
        val lastFullStateID = Some(Long.unbox(value))
        val lastFullState = consumer.getTransactionById(partition, lastFullStateID.get).get
        fillFullState(initialState, lastFullState, objectSerializer)
        consumer.setStreamPartitionOffset(partition, lastFullStateID.get)

        var maybeTxn = consumer.getTransaction(partition)
        while (maybeTxn.nonEmpty) {
          val partialState = mutable.Map[String, (String, Any)]()
          val partialStateTxn = maybeTxn.get

          partialStateTxn.next()
          while (partialStateTxn.hasNext()) {
            value = objectSerializer.deserialize(partialStateTxn.next())
            val variable = value.asInstanceOf[(String, (String, Any))]
            partialState(variable._1) = variable._2
          }
          applyPartialChanges(initialState, partialState)
          maybeTxn = consumer.getTransaction(partition)
        }
    }

    initialState
  }

  def fillFullState(initialState: mutable.Map[String, Any], transaction: ConsumerTransaction[Array[Byte]], objectSerializer: ObjectSerializer) = {
    var value: Object = null
    var variable: (String, Any) = null

    while (transaction.hasNext()) {
      value = objectSerializer.deserialize(transaction.next())
      variable = value.asInstanceOf[(String, Any)]
      initialState(variable._1) = variable._2
    }
  }

  def applyPartialChanges(fullState: mutable.Map[String, Any], partialState: mutable.Map[String, (String, Any)]) = {
    partialState.foreach {
      case (key, ("set", value)) => fullState(key) = value
      case (key, ("delete", _)) => fullState.remove(key)
    }
  }

}
