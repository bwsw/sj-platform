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
package com.bwsw.sj.engine.input.task

import java.util.concurrent.{ArrayBlockingQueue, TimeUnit}

import com.bwsw.common.hazelcast.{Hazelcast, HazelcastConfig}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.engine.TaskEngine
import com.bwsw.sj.common.engine.core.entities.InputEnvelope
import com.bwsw.sj.common.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.common.engine.core.input.{InputStreamingExecutor, Interval}
import com.bwsw.sj.common.si.model.instance.InputInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.{NumericalCheckpointTaskEngine, TimeCheckpointTaskEngine}
import com.bwsw.sj.engine.input.config.InputEngineConfigNames
import com.bwsw.sj.engine.input.eviction_policy.InputInstanceEvictionPolicy
import com.bwsw.sj.engine.input.task.reporting.{InputStreamingPerformanceMetrics, InputStreamingPerformanceMetricsThread}
import com.typesafe.config.ConfigFactory
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelFuture, ChannelHandlerContext}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try


/**
  * Class contains methods for running input module
  *
  * @param manager              allows to manage an environment of input streaming task
  * @param performanceMetrics   set of metrics that characterize performance of an input streaming module
  * @param channelContextQueue  queue for keeping a channel context [[io.netty.channel.ChannelHandlerContext]]
  *                             to process messages ([[io.netty.buffer.ByteBuf]]) in their turn
  * @param bufferForEachContext map for keeping a buffer containing incoming bytes [[io.netty.buffer.ByteBuf]]
  *                             with the appropriate channel context [[io.netty.channel.ChannelHandlerContext]]
  * @author Kseniya Mikhaleva
  */
abstract class InputTaskEngine(manager: InputTaskManager,
                               performanceMetrics: InputStreamingPerformanceMetrics,
                               channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                               bufferForEachContext: scala.collection.concurrent.Map[ChannelHandlerContext, ByteBuf],
                               connectionRepository: ConnectionRepository) extends TaskEngine {

  import InputTaskEngine.logger

  private val currentThread: Thread = Thread.currentThread()
  currentThread.setName(s"input-task-${manager.taskName}-engine")
  private val instance = manager.instance.asInstanceOf[InputInstance]
  private val environmentManager = createModuleEnvironmentManager()
  private val executor: InputStreamingExecutor[AnyRef] = manager.getExecutor(environmentManager)
  private val hazelcastMapName: String = instance.name + "-" + "inputEngine"


  private val applicationConfig = ConfigFactory.load()
  private val tcpIpMembers = applicationConfig.getString(InputEngineConfigNames.hosts).split(",")
  private val hazelcastConfig: HazelcastConfig = HazelcastConfig(
    instance.lookupHistory,
    instance.asyncBackupCount,
    instance.backupCount,
    instance.defaultEvictionPolicy,
    instance.queueMaxSize,
    tcpIpMembers)
  private val hazelcast = new Hazelcast(hazelcastMapName, hazelcastConfig)
  private val evictionPolicy = InputInstanceEvictionPolicy(instance.evictionPolicy, hazelcast)
  protected val checkpointInterval: Long = instance.checkpointInterval

  /**
    * Set of channel contexts related to the input envelopes to send a response to client
    * that a checkpoint has been initiated
    */
  private val contextsToSendCheckpointResponse: scala.collection.mutable.Set[ChannelHandlerContext] = collection.mutable.Set[ChannelHandlerContext]()

  private val performanceMetricsThread = new InputStreamingPerformanceMetricsThread(performanceMetrics)
  performanceMetricsThread.start()

  private val senderThread = new SenderThread(manager, performanceMetricsThread)
  senderThread.start()

  private def createModuleEnvironmentManager(): InputEnvironmentManager = {
    val streamService = connectionRepository.getStreamRepository
    val taggedOutputs = instance.outputs.flatMap(x => streamService.get(x))

    new InputEnvironmentManager(instance.options, taggedOutputs, connectionRepository.getFileStorage)
  }

  /**
    * It is in charge of running the basic execution logic of [[InputTaskEngine]]
    */
  def call(): Unit = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Run input task engine in a separate thread of execution service.")
    while (true) {
      getChannelContext() match {
        case Some(channelContext) =>
          contextsToSendCheckpointResponse.add(channelContext)
          logger.debug(s"Task name: ${manager.taskName}. Invoke tokenize() method of executor.")
          val maybeInterval = executor.tokenize(bufferForEachContext(channelContext))
          if (maybeInterval.isDefined) {
            logger.debug(s"Task name: ${manager.taskName}. Tokenize() method returned a defined interval.")
            tryToRead(channelContext, maybeInterval.get)
          }
        case None =>
      }

      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }

  private def getChannelContext(): Option[ChannelHandlerContext] = {
    var channelContext = Option(channelContextQueue.poll(EngineLiterals.eventWaitTimeout, TimeUnit.MILLISECONDS))
    if (channelContext.isEmpty) channelContext = getCtxOfNonEmptyBuffer

    channelContext
  }

  private def getCtxOfNonEmptyBuffer: Option[ChannelHandlerContext] =
    bufferForEachContext.find(x => x._2.readableBytes() > 0).map(_._1)

  private def tryToRead(channelContext: ChannelHandlerContext, interval: Interval): ChannelFuture = {
    val buffer = bufferForEachContext(channelContext)
    if (buffer.isReadable(interval.finalValue - interval.initialValue)) {
      logger.debug(s"Task name: ${manager.taskName}. The end index of interval is valid.")
      logger.debug(s"Task name: ${manager.taskName}. Invoke parse() method of executor.")
      val inputEnvelope = executor.parse(buffer, interval)
      clearBufferAfterParse(buffer, interval.finalValue)
      if (buffer.isReadable)
        Try(channelContextQueue.add(channelContext))
      val isNotDuplicateOrEmpty = processEnvelope(inputEnvelope)
      sendClientResponse(inputEnvelope, isNotDuplicateOrEmpty, channelContext)
    } else {
      logger.error(s"Task name: ${manager.taskName}. " +
        s"Method tokenize() returned an interval with a final value: ${interval.finalValue} " +
        s"that an input stream is not defined at (buffer write index: ${buffer.writerIndex()}).")
      throw new IndexOutOfBoundsException(s"Task name: ${manager.taskName}. " +
        s"Method tokenize() returned an interval with a final value: ${interval.finalValue} " +
        s"that an input stream is not defined at (buffer write index: ${buffer.writerIndex()})")
    }
  }

  /**
    * Removes the read bytes of the byte buffer
    *
    * @param buffer          a buffer for keeping incoming bytes
    * @param endReadingIndex index that was last at reading
    */
  private def clearBufferAfterParse(buffer: ByteBuf, endReadingIndex: Int): ByteBuf = {
    buffer.readerIndex(endReadingIndex + 1)
    buffer.discardSomeReadBytes()
  }

  /**
    * It is responsible for processing of envelope:
    * 1) checks whether an input envelope is defined and isn't duplicate or not
    * 2) if (1) is true an input envelope is sent to output stream(s)
    *
    * @param envelope may be input envelope
    * @return True if a processed envelope is processed, e.i. it is not duplicate or empty, and false otherwise
    */
  private def processEnvelope(envelope: Option[InputEnvelope[AnyRef]]): Boolean = {
    envelope match {
      case Some(inputEnvelope) =>
        logger.info(s"Task name: ${manager.taskName}. Envelope is defined. Process it.")
        performanceMetricsThread.addEnvelopeToInputStream(inputEnvelope)
        if (!isDuplicate(inputEnvelope.key, inputEnvelope.duplicateCheck)) {
          logger.debug(s"Task name: ${manager.taskName}. Envelope is not duplicate so send it.")
          val bytes = executor.serialize(inputEnvelope.data)
          senderThread.send(SerializedEnvelope(bytes, inputEnvelope.outputMetadata))
          afterReceivingEnvelope()

          true
        } else false
      case None => false
    }
  }

  /**
    * Checks whether a key is duplicate or not if it's necessary
    *
    * @param key            key for checking
    * @param duplicateCheck flag points a key has to be checked or not.
    * @return True if a processed envelope is a duplicate and false otherwise
    */
  private def isDuplicate(key: String, duplicateCheck: Option[Boolean]): Boolean = {
    logger.info(s"Task name: ${manager.taskName}. " +
      s"Try to check key: '$key' for duplication with a setting duplicateCheck = '$duplicateCheck' " +
      s"and an instance setting - 'duplicate-check' : '${instance.duplicateCheck}'.")
    if (duplicateCheck.isDefined) {
      if (duplicateCheck.get) evictionPolicy.isDuplicate(key) else false
    } else {
      if (instance.duplicateCheck) evictionPolicy.isDuplicate(key) else false
    }
  }

  protected def afterReceivingEnvelope(): Unit

  /**
    * It is responsible for sending an answer to client about the fact that an envelope is processed
    *
    * @param envelope              Input envelope
    * @param isNotEmptyOrDuplicate Flag points whether a processed envelope is empty or duplicate  or not.
    *                              If it is true it means a processed envelope is duplicate or empty and false otherwise
    * @param ctx                   Channel context related with this input envelope to send a message about this event
    */
  private def sendClientResponse(envelope: Option[InputEnvelope[AnyRef]], isNotEmptyOrDuplicate: Boolean, ctx: ChannelHandlerContext): ChannelFuture = {
    val inputStreamingResponse = executor.createProcessedMessageResponse(envelope, isNotEmptyOrDuplicate)
    if (inputStreamingResponse.sendResponsesNow) ctx.write(inputStreamingResponse.message)
    else ctx.writeAndFlush(inputStreamingResponse.message)
  }

  /**
    * Does group checkpoint of t-streams consumers/producers
    */
  private def doCheckpoint(): Unit = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint.")
    senderThread.checkpoint()
    checkpointInitiated()
    prepareForNextCheckpoint()
  }

  /**
    * It is responsible for sending a response to client about the fact that a checkpoint has been done
    * It will be invoked after commit of checkpoint group
    */
  private def checkpointInitiated(): Unit = {
    val inputStreamingResponse = executor.createCheckpointResponse()
    if (inputStreamingResponse.sendResponsesNow) contextsToSendCheckpointResponse.foreach(x => x.write(inputStreamingResponse.message))
    else contextsToSendCheckpointResponse.foreach(x => x.writeAndFlush(inputStreamingResponse.message))
  }

  protected def prepareForNextCheckpoint(): Unit

  /**
    * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
    *
    * @param isCheckpointInitiated flag points whether checkpoint was initiated inside input module (not on the schedule) or not.
    */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean
}

object InputTaskEngine {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  /**
    * Creates InputTaskEngine is in charge of a basic execution logic of task of input module
    *
    * @return Engine of input task
    */
  def apply(manager: InputTaskManager,
            performanceMetrics: InputStreamingPerformanceMetrics,
            channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
            bufferForEachContext: scala.collection.concurrent.Map[ChannelHandlerContext, ByteBuf],
            connectionRepository: ConnectionRepository): InputTaskEngine = {

    manager.inputInstance.checkpointMode match {
      case EngineLiterals.`timeIntervalMode` =>
        logger.info(s"Task: ${manager.taskName}. Input module has a '${EngineLiterals.timeIntervalMode}' checkpoint mode, create an appropriate task engine.")
        new InputTaskEngine(manager, performanceMetrics, channelContextQueue, bufferForEachContext, connectionRepository) with TimeCheckpointTaskEngine
      case EngineLiterals.`everyNthMode` =>
        logger.info(s"Task: ${manager.taskName}. Input module has an '${EngineLiterals.everyNthMode}' checkpoint mode, create an appropriate task engine.")
        new InputTaskEngine(manager, performanceMetrics, channelContextQueue, bufferForEachContext, connectionRepository) with NumericalCheckpointTaskEngine
    }
  }
}