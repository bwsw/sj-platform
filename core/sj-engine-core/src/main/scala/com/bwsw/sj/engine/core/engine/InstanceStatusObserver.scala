package com.bwsw.sj.engine.core.engine

import java.util.concurrent.Callable

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.utils.EngineLiterals

class InstanceStatusObserver(instanceName : String) extends Callable[Unit] {

  override def call(): Unit = {
    while (true) {
      ConnectionRepository.getInstanceService.get(instanceName) match {
        case Some(instance) =>
          if (instance.status != EngineLiterals.started) {
            throw new InterruptedException(s"Task cannot continue to work because of '${instance.status}' status of instance")
          }
      }

      Thread.sleep(1000)
    }
  }
}
