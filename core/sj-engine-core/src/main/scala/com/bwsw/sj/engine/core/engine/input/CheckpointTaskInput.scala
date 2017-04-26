package com.bwsw.sj.engine.core.engine.input

import com.bwsw.tstreams.agents.group.CheckpointGroup

/**
  * Task input that is able to do checkpoint of consuming or processing messages
  *
  * @author Kseniya Mikhaleva
  */
trait CheckpointTaskInput {
  val checkpointGroup: CheckpointGroup

  def doCheckpoint() = {
    checkpointGroup.checkpoint()
  }
}