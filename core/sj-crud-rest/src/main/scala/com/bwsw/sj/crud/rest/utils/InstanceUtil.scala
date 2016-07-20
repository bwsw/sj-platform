package com.bwsw.sj.crud.rest.utils

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.ModuleConstants._

/**
  * Created: 20/07/2016
  *
  * @author Kseniya Tomskikh
  */
object InstanceUtil {
  private val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService

  def checkStatusInstances() = {
    val instances = instanceDAO.getAll.filter { instance =>
      instance.status.equals(starting) ||
        instance.status.equals(stopping) ||
        instance.status.equals(deleting)
    }

    instances.foreach { instance =>

    }
  }

}
