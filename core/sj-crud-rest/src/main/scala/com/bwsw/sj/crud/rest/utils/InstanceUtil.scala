package com.bwsw.sj.crud.rest.utils

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.crud.rest.instance.InstanceStopper
import org.slf4j.LoggerFactory

/**
  *
  *
  * @author Kseniya Tomskikh
  */
object InstanceUtil {
  private val logger = LoggerFactory.getLogger(this.getClass.getName)
  private val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService

  /**
    * Check instances
    *
    * If instances has status "starting", "stopping" or "deleting"
    * instance will be stopped
    */
  def checkStatusInstances() = {
    logger.info("Run crud-rest. Check instances.")
    val instances = instanceDAO.getAll.filter { instance =>
      instance.status.equals(starting) ||
        instance.status.equals(stopping) ||
        instance.status.equals(deleting)
    }

    instances.foreach { instance =>
      new Thread(new InstanceStopper(instance, 1000)).start()
    }
  }

}
