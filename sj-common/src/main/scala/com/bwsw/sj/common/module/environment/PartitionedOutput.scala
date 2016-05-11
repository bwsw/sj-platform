package com.bwsw.sj.common.module.environment

import com.bwsw.tstreams.agents.producer.{BasicProducer, BasicProducerTransaction, ProducerPolicies}
import scala.collection._
/**
 * Provides an output stream that defined for each partition
 * Created: 20/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param producer Producer for specific output of stream
 */

class PartitionedOutput(producer: BasicProducer[Array[Byte], Array[Byte]]) {

  private val txns = mutable.Map[Int, BasicProducerTransaction[Array[Byte], Array[Byte]]]()

  def put(data: Array[Byte], partition: Int) =
    if (txns.contains(partition)) txns(partition).send(data)
    else txns(partition) = producer.newTransaction(ProducerPolicies.errorIfOpen, partition)
}
