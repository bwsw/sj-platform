package com.bwsw.sj.engine.windowed.task.input

import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.engine.core.entities.Envelope
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnvelopeFetcher[T](taskInput: RetrievableTaskInput[Envelope]) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val lowWatermark = ConfigurationSettingsUtils.getLowWatermark()
  private val envelopesByStream = mutable.Map[String, mutable.Queue[Envelope]]()

  def get(streams: Seq[String]) = {
    val envelopes = streams.map(stream => {
      (stream, synchronized {
        envelopesByStream(stream).dequeueAll(_ => true) //todo можно запрашивать по кол-ву
      })
    }).toMap

    Future(fillQueue())

    envelopes
  }

  private def fillQueue() = {
    if (envelopesByStream.forall(x => x._2.size < lowWatermark)) {
      val unarrangedEnvelopes = taskInput.get()

      unarrangedEnvelopes.foreach(x => synchronized {
        envelopesByStream(x.stream) += x
      })
    }
  }
}
