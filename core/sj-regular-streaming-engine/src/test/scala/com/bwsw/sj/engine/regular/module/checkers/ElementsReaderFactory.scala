package com.bwsw.sj.engine.regular.module.checkers

import com.bwsw.sj.engine.core.testutils.checkers.{KafkaReader, TStreamsReader}
import com.bwsw.sj.engine.regular.module.DataFactory
import com.bwsw.sj.engine.regular.module.SjRegularBenchmarkConstants.{inputCount, outputCount, partitions}
import com.bwsw.tstreams.agents.consumer.Consumer

/**
  * Provides methods to create data readers
  *
  * @author Pavel Tomskikh
  */
object ElementsReaderFactory {
  def createTStreamsInputElementsReader: TStreamsReader[Int] = {
    new TStreamsReader[Int]((1 to inputCount).map(x =>
      DataFactory.createInputTstreamConsumer(partitions, x.toString)))
  }

  def createOutputElementsReader: TStreamsReader[Int] = {
    new TStreamsReader[Int]((1 to outputCount).map(x =>
      DataFactory.createOutputConsumer(partitions, x.toString)))
  }

  def createKafkaInputElementsReader =
    new KafkaReader[Int](DataFactory.createInputKafkaConsumer(inputCount, partitions))

  def createStateConsumer: Consumer =
    DataFactory.createStateConsumer(DataFactory.connectionRepository.getStreamRepository)
}
