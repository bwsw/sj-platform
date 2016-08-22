package com.bwsw.sj.engine.output

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module.{Instance, OutputInstance}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import org.slf4j.LoggerFactory

/**
 * Data factory of output streaming engine
 * Created: 26/05/2016
 *
 * @author Kseniya Tomskikh
 */
object OutputDataFactory {

  private val logger = LoggerFactory.getLogger(getClass.getName)

  val instanceName: String = System.getenv("INSTANCE_NAME")
  val taskName: String = System.getenv("TASK_NAME")
  val agentHost: String = System.getenv("AGENTS_HOST")
  val agentsPorts: Array[String] = System.getenv("AGENTS_PORTS").split(",")

  assert(agentsPorts.length == 2, "Not enough ports for t-stream consumers/producers ")

  private val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService
  private val streamDAO = ConnectionRepository.getStreamService

  val instance: OutputInstance = instanceDAO.get(instanceName).asInstanceOf[OutputInstance]

  val inputStream: SjStream = streamDAO.get(instance.inputs.head).get
  val outputStream: SjStream = streamDAO.get(instance.outputs.head).get

  val inputStreamService = inputStream.service.asInstanceOf[TStreamService]

}
