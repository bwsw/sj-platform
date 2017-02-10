package com.bwsw.sj.engine.windowed.task.input

import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class EnvelopeFetcher(taskInput: RetrievableTaskInput[_ >: TStreamEnvelope with KafkaEnvelope <: Envelope]) {
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
