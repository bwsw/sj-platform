package com.bwsw.sj.engine.output.benchmark

import com.bwsw.sj.common.DAL.model.ESSjStream
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._

/**
  * Created: 20/06/2016
  *
  * @author Kseniya Tomskikh
  */
object OutputModuleDataChecker extends App {

  val streamService = ConnectionRepository.getStreamService
  val esChkStream = streamService.get(esChkStreamName).asInstanceOf[ESSjStream]
  val esStream = streamService.get(esStreamName).asInstanceOf[ESSjStream]

  val (esClient, esService) = openDbConnection(esStream)
  val (esChkClient, esChkService) = openDbConnection(esChkStream)

  val esChkRequest = esChkClient.prepareIndex(esChkService.index, esChkStream.name)
  val inputData = null

  val esRequest = esClient.prepareIndex(esService.index, esStream.name)
  val outputData = null


}
