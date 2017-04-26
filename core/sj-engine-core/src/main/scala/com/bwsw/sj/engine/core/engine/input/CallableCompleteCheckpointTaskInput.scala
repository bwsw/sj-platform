package com.bwsw.sj.engine.core.engine.input

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.tstreams.agents.group.CheckpointGroup

/**
  * Class is responsible for handling kafka inputs and t-stream inputs
  *
  * @author Kseniya Mikhaleva
  * @param manager       Manager of environment of task of regular/batch module
  * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
  *                      which will be retrieved into a module
  */
class CallableCompleteCheckpointTaskInput[T <: AnyRef](manager: CommonTaskManager,
                                                       blockingQueue: ArrayBlockingQueue[Envelope],
                                                       override val checkpointGroup: CheckpointGroup = new CheckpointGroup()) extends CallableCheckpointTaskInput[Envelope](manager.inputs) {

  private val callableKafkaTaskInput = new CallableKafkaCheckpointTaskInput[T](manager, blockingQueue, checkpointGroup)
  private val callableTStreamTaskInput = new CallableTStreamCheckpointTaskInput[T](manager, blockingQueue, checkpointGroup)

  def call() = {
    callableTStreamTaskInput.call()
    callableKafkaTaskInput.call()
  }

  override def doCheckpoint(): Unit = {
    callableKafkaTaskInput.doCheckpoint()
    super.doCheckpoint()
  }

  override def close(): Unit = {
    callableTStreamTaskInput.close()
    callableKafkaTaskInput.close()
  }
}
