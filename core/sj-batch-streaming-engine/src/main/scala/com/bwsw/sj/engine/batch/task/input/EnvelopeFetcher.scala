package com.bwsw.sj.engine.batch.task.input

import java.util.concurrent.Executors

import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory

import scala.collection.mutable

class EnvelopeFetcher(taskInput: RetrievableCheckpointTaskInput[Envelope]) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("EnvelopeFetcher-%d").build())
  private val lowWatermark = ConfigurationSettingsUtils.getLowWatermark()
  private val envelopesByStream = taskInput.inputs.map(x => (x._1.name, new mutable.Queue[Envelope]()))

  scheduledExecutor.scheduleWithFixedDelay(fillQueue(), 0, EngineLiterals.eventWaitTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)

  def get(stream: String): Option[Envelope] = {
    logger.debug(s"Get an envelope from queue of stream: $stream.")
    synchronized {
      if (envelopesByStream(stream).isEmpty) None
      else Some(envelopesByStream(stream).dequeue())
    }
  }

  private def fillQueue() = new Runnable {
    override def run(): Unit = {
      if (envelopesByStream.forall(x => x._2.size < lowWatermark)) {
        logger.debug(s"An envelope queue has got less than $lowWatermark elements so it needs to be filled.")
        val unarrangedEnvelopes = taskInput.get()

        unarrangedEnvelopes.foreach(x => synchronized {
          envelopesByStream(x.stream) += x
        })
      }
    }
  }

  def registerEnvelope(envelope: Envelope): Unit = taskInput.registerEnvelope(envelope)

  def doCheckpoint(): Unit = taskInput.doCheckpoint()

  def checkpointGroup: CheckpointGroup = taskInput.checkpointGroup

  def close(): Unit = taskInput.close()
}