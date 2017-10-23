package com.bwsw.sj.engine.output.benchmark.data_checkers

import com.bwsw.sj.common.dal.model.stream.{StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import com.bwsw.sj.engine.core.testutils.checkers.TStreamsReader
import com.bwsw.sj.engine.output.benchmark.DataFactory.{connectionRepository, createConsumer, tstreamInputName}

/**
  * Provides methods to create data readers
  *
  * @author Pavel Tomskikh
  */
object ElementsReaderFactory {
  def createInputElementsReader = {
    val streamService: GenericMongoRepository[StreamDomain] = connectionRepository.getStreamRepository
    val tStream: TStreamStreamDomain = streamService.get(tstreamInputName).get.asInstanceOf[TStreamStreamDomain]

    new TStreamsReader[(Int, String)](Seq(createConsumer(tStream)))
  }
}
