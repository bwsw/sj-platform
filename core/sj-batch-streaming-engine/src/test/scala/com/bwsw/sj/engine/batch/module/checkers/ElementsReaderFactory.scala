package com.bwsw.sj.engine.batch.module.checkers

import com.bwsw.sj.engine.batch.module.DataFactory
import com.bwsw.sj.engine.batch.module.SjBatchModuleBenchmarkConstants.{inputCount, partitions}
import com.bwsw.sj.engine.core.testutils.checkers.{KafkaReader, TStreamsReader}
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

  def createKafkaInputElementsReader: KafkaReader[Int] =
    new KafkaReader[Int](DataFactory.createInputKafkaConsumer(inputCount, partitions))

  def createStateConsumer: Consumer =
    DataFactory.createStateConsumer(DataFactory.connectionRepository.getStreamRepository)

  def createOutputElementsReader: OutputReader = new OutputReader
}
