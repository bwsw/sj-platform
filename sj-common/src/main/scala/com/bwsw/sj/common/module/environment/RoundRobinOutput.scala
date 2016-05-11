package com.bwsw.sj.common.module.environment

import com.bwsw.tstreams.agents.producer.{ProducerPolicies, BasicProducerTransaction, BasicProducer}

/**
 * Provides an output stream that defined for stream in whole.
 * Recording of transaction occurs with the use of round-robin policy
 * Created: 20/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param producer Producer for specific output of stream
 */

class RoundRobinOutput(producer: BasicProducer[Array[Byte], Array[Byte]]) {

  private var txn: Option[BasicProducerTransaction[Array[Byte], Array[Byte]]] = None

  def put(data: Array[Byte]) =
    if (txn.isDefined) txn.get.send(data)
    else txn = Some(producer.newTransaction(ProducerPolicies.errorIfOpen))
}
