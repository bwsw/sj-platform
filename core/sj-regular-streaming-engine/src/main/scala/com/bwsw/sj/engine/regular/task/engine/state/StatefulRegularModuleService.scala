package com.bwsw.sj.engine.regular.task.engine.state

import java.util.Random

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.{TStreamEnvelope, KafkaEnvelope, Envelope}
import com.bwsw.sj.engine.core.environment.{ModuleEnvironmentManager, StatefulModuleEnvironmentManager}
import com.bwsw.sj.engine.core.regular.RegularStreamingExecutor
import com.bwsw.sj.engine.core.state.StateStorage
import com.bwsw.sj.engine.regular.state.RAMStateService
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import com.bwsw.tstreams.agents.group.CheckpointGroup

/**
 * Class is in charge of creating a StatefulModuleEnvironmentManager (and executor)
 * and performing a saving of state
 *
 * @param manager Manager of environment of task of regular module
 * @param checkpointGroup Group of t-stream agents that have to make a checkpoint at the same time
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 */
class StatefulRegularModuleService(manager: RegularTaskManager, checkpointGroup: CheckpointGroup, performanceMetrics: RegularStreamingPerformanceMetrics)
  extends RegularModuleService(manager, checkpointGroup, performanceMetrics) {

  private val streamService = ConnectionRepository.getStreamService
  private var countOfCheckpoints = 1
  private val stateService = new RAMStateService(manager, checkpointGroup)

  val regularEnvironmentManager = new StatefulModuleEnvironmentManager(
    new StateStorage(stateService),
    regularInstance.getOptionsAsMap(),
    outputProducers,
    regularInstance.outputs
      .flatMap(x => streamService.get(x)),
    outputTags,
    moduleTimer,
    performanceMetrics
  )

  //val executor = manager.getExecutor(regularEnvironmentManager).asInstanceOf[RegularStreamingExecutor]
  val executor = new Executor(regularEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

  /**
   * Does group checkpoint of t-streams state consumers/producers
   */
  override def doCheckpoint() = {
    if (countOfCheckpoints != regularInstance.stateFullCheckpoint) {
      doCheckpointOfPartOfState()
    } else {
      doCheckpointOfFullState()
    }
  }

  /**
   * Saves a partial state changes
   */
  private def doCheckpointOfPartOfState() = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of a part of state\n")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler\n")
    executor.onBeforeStateSave(false)
    stateService.savePartialState()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler\n")
    executor.onAfterStateSave(false)
    countOfCheckpoints += 1
  }

   /**
   * Saves a state
   */
  private def doCheckpointOfFullState() = {
    logger.info(s"Task: ${manager.taskName}. It's time to checkpoint of full state\n")
    logger.debug(s"Task: ${manager.taskName}. Invoke onBeforeStateSave() handler\n")
    executor.onBeforeStateSave(true)
    stateService.saveFullState()
    logger.debug(s"Task: ${manager.taskName}. Invoke onAfterStateSave() handler\n")
    executor.onAfterStateSave(true)
    countOfCheckpoints = 1
  }
}

class Executor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor(manager) {

  val objectSerializer = new ObjectSerializer()
  val state: StateStorage = manager.getState

  override def onInit(): Unit = {
    if (!state.isExist("sum")) state.set("sum", 0)
    println("new init")
  }

  override def onAfterCheckpoint(): Unit = {
    println("on after checkpoint")
  }

  override def onMessage(envelope: Envelope): Unit = {
    val outputs = manager.getStreamsByTags(Array("output"))
    val output = manager.getRoundRobinOutput(outputs(new Random().nextInt(outputs.length)))
    var sum = state.get("sum").asInstanceOf[Int]

    if (new Random().nextInt(100) < 20) {
      println("it happened")
      throw new Exception("it happened")
    }
    envelope match {
      case kafkaEnvelope: KafkaEnvelope =>
        println("element: " +
          objectSerializer.deserialize(kafkaEnvelope.data).asInstanceOf[Int])
        output.put(kafkaEnvelope.data)
        sum += objectSerializer.deserialize(kafkaEnvelope.data).asInstanceOf[Int]
        state.set("sum", sum)
      case tstreamEnvelope: TStreamEnvelope =>
        println("elements: " +
          tstreamEnvelope.data.map(x => objectSerializer.deserialize(x).asInstanceOf[Int]).mkString(","))

        tstreamEnvelope.data.foreach(x => {
          sum += objectSerializer.deserialize(x).asInstanceOf[Int]
        })
        tstreamEnvelope.data.foreach(output.put)
        state.set("sum", sum)
    }
    println("stream name = " + envelope.stream)
  }

  override def onTimer(jitter: Long): Unit = {
    println("onTimer")
  }

  override def onAfterStateSave(isFull: Boolean): Unit = {
    if (isFull) {
      println("on after full state saving")
    } else println("on after partial state saving")
  }

  override def onBeforeCheckpoint(): Unit = {
    println("on before checkpoint")
  }

  override def onIdle(): Unit = {
    println("on Idle")
  }

  /**
   * Handler triggered before save state
   *
   * @param isFullState Flag denotes that full state (true) or partial changes of state (false) will be saved
   */
  override def onBeforeStateSave(isFullState: Boolean): Unit = {
    println("on before state saving")
  }
}