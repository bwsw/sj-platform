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
import com.bwsw.sj.common.engine.core.environment.{InputEnvironmentManager, TStreamsSenderThread}
import com.bwsw.sj.common.engine.core.input.{InputStreamingExecutor, Interval}
import com.bwsw.sj.common.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.common.si.model.instance.InputInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.{NumericalCheckpointTaskEngine, TimeCheckpointTaskEngine}
import com.bwsw.sj.engine.input.config.InputEngineConfigNames
import com.bwsw.sj.engine.input.connection.tcp.server.ChannelHandlerContextState
import com.bwsw.sj.engine.input.eviction_policy.InputInstanceEvictionPolicy
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import io.netty.channel.{ChannelFuture, ChannelHandlerContext}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}


/**
  * Class contains methods for running input module
  *
  * @param manager             allows to manage an environment of input streaming task
  * @param performanceMetrics  set of metrics that characterize performance of an input streaming module
  * @param channelContextQueue queue for keeping a channel context [[io.netty.channel.ChannelHandlerContext]]
  *                            to process messages ([[io.netty.buffer.ByteBuf]]) in their turn
  * @param stateByContext      map for keeping a state of a channel context
  *                            [[com.bwsw.sj.engine.input.connection.tcp.server.ChannelHandlerContextState]]
  *                            with the appropriate channel context [[io.netty.channel.ChannelHandlerContext]]
  * @author Kseniya Mikhaleva
  */
abstract class InputTaskEngine(manager: InputTaskManager,
                               performanceMetrics: PerformanceMetrics,
                               channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                               stateByContext: scala.collection.concurrent.Map[ChannelHandlerContext, ChannelHandlerContextState],
                               connectionRepository: ConnectionRepository) extends TaskEngine {

  import InputTaskEngine.logger

  private val logPrefix = s"Task name: ${manager.taskName}. "
  private val currentThread: Thread = Thread.currentThread()
  currentThread.setName(s"input-task-${manager.taskName}-engine")
  private val producers = manager.outputProducers
  private val checkpointGroup = manager.createCheckpointGroup()
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
  private val contextsToSendCheckpointResponse: mutable.Set[ChannelHandlerContext] = mutable.Set[ChannelHandlerContext]()

  addProducersToCheckpointGroup()

  private val senderThread = new TStreamsSenderThread(
    producers, performanceMetrics, s"input-task-${manager.taskName}-sender")
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
    logInfo("Run input task engine in a separate thread of execution service.")
    while (true) {
      getChannelContext() match {
        case Some((channelContext, state)) =>
          logDebug(s"Task name: ${manager.taskName}. Invoke tokenize() method of executor.")
          val maybeInterval = executor.tokenize(state.buffer)
          if (maybeInterval.isDefined) {
            logDebug(s"Task name: ${manager.taskName}. Tokenize() method returned a defined interval.")
            tryToRead(channelContext, state, maybeInterval.get)
          }

          if (state.isActive)
            contextsToSendCheckpointResponse += channelContext

        case None =>
      }

      if (isItTimeToCheckpoint(environmentManager.isCheckpointInitiated)) doCheckpoint()
    }
  }

  private def getChannelContext(): Option[(ChannelHandlerContext, ChannelHandlerContextState)] = {
    val maybeChannelContext = Option(channelContextQueue.poll(EngineLiterals.eventWaitTimeout, TimeUnit.MILLISECONDS))

    if (maybeChannelContext.isEmpty)
      stateByContext.find(_._2.buffer.isReadable)
    else
      maybeChannelContext.flatMap { channelContext =>
        stateByContext.get(channelContext).map { state =>
          state.isQueued = false

          (channelContext, state)
        }
      }
  }

  private def tryToRead(channelContext: ChannelHandlerContext,
                        state: ChannelHandlerContextState,
                        interval: Interval): Unit = {
    if (state.buffer.isReadable(interval.finalValue - interval.initialValue)) {
      logDebug("The end index of interval is valid.")
      logDebug("Invoke parse() method of executor.")
      val inputEnvelope = executor.parse(state.buffer, interval)
      clearBufferAfterParse(channelContext, state, interval.finalValue)
      if (state.buffer.isReadable) {
        Try(channelContextQueue.add(channelContext)) match {
          case Success(_) => state.isQueued = true
          case Failure(_) =>
        }
      }
      val isNotDuplicateOrEmpty = processEnvelope(inputEnvelope)
      if (state.isActive)
        sendClientResponse(inputEnvelope, isNotDuplicateOrEmpty, channelContext)
    } else {
      val errorMessage = s"Method tokenize() returned an interval with a final value: ${interval.finalValue} " +
        s"that an input stream is not defined at (buffer write index: ${state.buffer.writerIndex()})."
      logError(errorMessage)
      throw new IndexOutOfBoundsException(logPrefix + " " + errorMessage)
    }
  }

  /**
    * Removes the read bytes of the byte buffer
    *
    * @param channelContext  channel context
    * @param state           contains a buffer for keeping incoming bytes and a state of channelContext
    * @param endReadingIndex index that was last at reading
    */
  private def clearBufferAfterParse(channelContext: ChannelHandlerContext,
                                    state: ChannelHandlerContextState,
                                    endReadingIndex: Int): Unit = {
    state.buffer.readerIndex(endReadingIndex + 1)
    state.buffer.discardSomeReadBytes()
    if (!state.isActive && !state.buffer.isReadable)
      stateByContext -= channelContext
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
        logDebug("Envelope is defined. Process it.")
        performanceMetrics.addEnvelopeToInputStream(inputEnvelope)
        if (!isDuplicate(inputEnvelope.key, inputEnvelope.duplicateCheck)) {
          logDebug("Envelope is not duplicate so send it.")
          val bytes = executor.serialize(inputEnvelope.data)
          inputEnvelope.outputMetadata.foreach {
            case (stream, partition) => senderThread.send(bytes, stream, partition)
          }
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
    logDebug(s"Try to check key: '$key' for duplication with a setting duplicateCheck = '$duplicateCheck' " +
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
  private def sendClientResponse(envelope: Option[InputEnvelope[AnyRef]],
                                 isNotEmptyOrDuplicate: Boolean,
                                 ctx: ChannelHandlerContext): ChannelFuture = {
    val inputStreamingResponse = executor.createProcessedMessageResponse(envelope, isNotEmptyOrDuplicate)
    if (inputStreamingResponse.sendResponsesNow) ctx.write(inputStreamingResponse.message)
    else ctx.writeAndFlush(inputStreamingResponse.message)
  }

  /**
    * Does group checkpoint of t-streams consumers/producers
    */
  private def doCheckpoint(): Unit = {
    logInfo("It's time to checkpoint.")
    senderThread.prepareToCheckpoint()
    checkpointGroup.checkpoint()
    checkpointInitiated()
    prepareForNextCheckpoint()
  }

  /**
    * It is responsible for sending a response to client about the fact that a checkpoint has been done
    * It will be invoked after commit of checkpoint group
    */
  private def checkpointInitiated(): Unit = {
    val inputStreamingResponse = executor.createCheckpointResponse()
    contextsToSendCheckpointResponse.retain(c => stateByContext.get(c).exists(_.isActive))
    if (inputStreamingResponse.sendResponsesNow)
      contextsToSendCheckpointResponse.foreach(x => x.write(inputStreamingResponse.message))
    else
      contextsToSendCheckpointResponse.foreach(x => x.writeAndFlush(inputStreamingResponse.message))
  }

  protected def prepareForNextCheckpoint(): Unit

  /**
    * Check whether a group checkpoint of t-streams consumers/producers have to be done or not
    *
    * @param isCheckpointInitiated flag points whether checkpoint was initiated inside input module (not on the schedule) or not.
    */
  protected def isItTimeToCheckpoint(isCheckpointInitiated: Boolean): Boolean


  private def logInfo(message: String): Unit = logger.info(logPrefix + message)

  private def logDebug(message: String): Unit = logger.debug(logPrefix + message)

  private def logError(message: String): Unit = logger.error(logPrefix + message)

  private def addProducersToCheckpointGroup(): Unit = {
    logger.debug(s"Task: ${manager.taskName}. Start adding t-stream producers to checkpoint group.")
    producers.foreach(x => checkpointGroup.add(x._2))
    logger.debug(s"Task: ${manager.taskName}. The t-stream producers are added to checkpoint group.")
  }
}

object InputTaskEngine {
  private val logger: Logger = Logger(this.getClass)

  /**
    * Creates InputTaskEngine is in charge of a basic execution logic of task of input module
    *
    * @return Engine of input task
    */
  def apply(manager: InputTaskManager,
            performanceMetrics: PerformanceMetrics,
            channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
            stateByContext: scala.collection.concurrent.Map[ChannelHandlerContext, ChannelHandlerContextState],
            connectionRepository: ConnectionRepository): InputTaskEngine = {

    manager.inputInstance.checkpointMode match {
      case EngineLiterals.`timeIntervalMode` =>
        logger.info(s"Task: ${manager.taskName}." +
          s" Input module has a '${EngineLiterals.timeIntervalMode}' checkpoint mode, create an appropriate task engine.")
        new InputTaskEngine(manager, performanceMetrics, channelContextQueue, stateByContext, connectionRepository)
          with TimeCheckpointTaskEngine

      case EngineLiterals.`everyNthMode` =>
        logger.info(s"Task: ${manager.taskName}." +
          s" Input module has an '${EngineLiterals.everyNthMode}' checkpoint mode, create an appropriate task engine.")
        new InputTaskEngine(manager, performanceMetrics, channelContextQueue, stateByContext, connectionRepository)
          with NumericalCheckpointTaskEngine
    }
  }
}