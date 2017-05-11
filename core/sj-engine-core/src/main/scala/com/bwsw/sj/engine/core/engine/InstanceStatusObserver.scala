package com.bwsw.sj.engine.core.engine

import java.util.concurrent.Callable

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals
import org.slf4j.LoggerFactory

class InstanceStatusObserver(instanceName: String) extends Callable[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass)

  override def call(): Unit = {
    while (true) {
      logger.debug("Check an instance status in case of an instance failure to stop a work of task.")
      ConnectionRepository.getInstanceRepository.get(instanceName) match {
        case Some(instance) =>
          if (instance.status != EngineLiterals.started) {
            logger.error(s"Task cannot continue to work because of '${instance.status}' status of instance")
            throw new InterruptedException(s"Task cannot continue to work because of '${instance.status}' status of instance")
          }

        case None =>
          throw new UnknownError(s"Instance: $instanceName has been removed. It seems that there is a bug.")
      }

      Thread.sleep(1000)
    }
  }
}
