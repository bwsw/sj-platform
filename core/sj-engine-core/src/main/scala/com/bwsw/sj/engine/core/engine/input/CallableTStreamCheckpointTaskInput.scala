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
package com.bwsw.sj.engine.core.engine.input

import java.util.Date

import com.bwsw.common.SerializerInterface
import com.bwsw.sj.common.dal.model.stream.TStreamStreamDomain
import com.bwsw.sj.common.engine.core.entities.{EnvelopeInterface, TStreamEnvelope, WeightedBlockingQueue}
import com.bwsw.sj.common.engine.core.managment.TaskManager
import com.bwsw.sj.common.si.model.instance.{BatchInstance, OutputInstance, RegularInstance}
import com.bwsw.sj.common.utils.{EngineLiterals, StreamLiterals}
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.{DateTime, IOffset, Newest, Oldest}
import com.bwsw.tstreams.agents.consumer.subscriber.Subscriber
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.typesafe.scalalogging.Logger

import scala.collection.mutable

/**
  * Class is responsible for launching t-stream subscribing consumers
  * that put consumed message, which are wrapped in envelope, into a common queue,
  * and processing the envelopes
  *
  * @param manager       Manager of environment of task
  * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
  *                      which will be retrieved into a module
  * @author Kseniya Mikhaleva
  *
  */
class CallableTStreamCheckpointTaskInput[T <: AnyRef](manager: TaskManager,
                                                      blockingQueue: WeightedBlockingQueue[EnvelopeInterface],
                                                      checkpointGroup: CheckpointGroup,
                                                      envelopeDataSerializer: SerializerInterface)
  extends CallableCheckpointTaskInput[TStreamEnvelope[T]](manager.inputs) {

  private val logger = Logger(this.getClass)
  private val (subscribingConsumers, consumerClones) = createConsumers()

  private def createConsumers(): (Seq[Subscriber], mutable.Map[String, Consumer]) = {
    logger.debug(s"Task: ${manager.taskName}. Start creating subscribing consumers.")
    val consumerClones = mutable.Map[String, Consumer]()
    val inputs = manager.inputs
    val offset = getOffset()

    val consumers = inputs
      .filter { case (stream, _) => stream.streamType == StreamLiterals.tstreamsType }
      .map { case (stream, partitions) => (stream.asInstanceOf[TStreamStreamDomain], partitions.toList) }
      .map {
        case (stream, partitions) =>
          val callback = new ConsumerCallback[T](envelopeDataSerializer, blockingQueue, stream)
          val consumer = manager.createSubscribingConsumer(stream, partitions, offset, callback)
          val clone = manager.createConsumer(stream, partitions, offset, Some(consumer.name))

          consumerClones += (clone.name -> clone)
          consumer
      }.toSeq
    logger.debug(s"Task: ${manager.taskName}. Creation of subscribing consumers is finished.")

    (consumers, consumerClones)
  }

  private def getOffset(): IOffset = {
    logger.debug(s"Task: ${manager.taskName}. Get an offset parameter from instance.")
    val instance = manager.instance
    val offset = instance match {
      case instance: RegularInstance => instance.startFrom
      case instance: BatchInstance => instance.startFrom
      case instance: OutputInstance => instance.startFrom
      case badInstance =>
        logger.error(s"Task: ${manager.taskName}. Instance type isn't supported.")
        throw new TypeNotPresentException(badInstance.getClass.getName,
          new Throwable("Instance type isn't supported"))
    }

    chooseOffset(offset)
  }

  /**
    * Chooses offset policy for t-streams consumers
    *
    * @param startFrom Offset policy name or specific date
    * @return Offset
    */
  private def chooseOffset(startFrom: String): IOffset = {
    logger.debug(s"Choose offset policy for t-streams consumer.")
    startFrom match {
      case EngineLiterals.oldestStartMode => Oldest
      case EngineLiterals.newestStartMode => Newest
      case time => DateTime(new Date(time.toLong * 1000))
    }
  }

  def call(): Unit = {
    addClonesToCheckpointGroup()
    launchConsumers()
  }

  private def addClonesToCheckpointGroup(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Start adding clones of subscribing consumers to checkpoint group.")
    consumerClones.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. Adding clones of subscribing consumers to checkpoint group is finished.")
  }

  private def launchConsumers(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Launch subscribing consumers and their clones.")
    subscribingConsumers.foreach(_.start())
    consumerClones.foreach(_._2.start())
    logger.debug(s"Task: ${manager.taskName}. Subscribing consumers and their clones are launched.")
  }

  override def setConsumerOffset(envelope: TStreamEnvelope[T]): Unit = {
    logger.debug(s"Task: ${manager.taskName}. " +
      s"Change local offset of consumer: ${envelope.consumerName} to txn: ${envelope.id}.")
    consumerClones(envelope.consumerName).setStreamPartitionOffset(envelope.partition, envelope.id)
  }

  override def close(): Unit = {
    subscribingConsumers.foreach(_.stop())
    consumerClones.foreach(_._2.stop())
  }
}
