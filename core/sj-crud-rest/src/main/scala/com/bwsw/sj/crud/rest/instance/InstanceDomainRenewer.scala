package com.bwsw.sj.crud.rest.instance

import java.util.Calendar

import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance.Instance
import scaldi.Injectable.inject
import scaldi.Injector


private[instance] class InstanceDomainRenewer(implicit val injector: Injector) {
  private val instanceRepository = inject[ConnectionRepository].getInstanceRepository

  def updateInstanceStatus(instance: Instance, status: String): Unit = {
    instance.status = status
    instanceRepository.save(instance.to())
  }

  def updateInstanceRestAddress(instance: Instance, restAddress: String): Unit = {
    instance.restAddress = Option(restAddress)
    instanceRepository.save(instance.to())
  }

  def updateFrameworkStage(instance: Instance, status: String): Unit = {
    if (instance.stage.state.equals(status)) {
      instance.stage.duration = Calendar.getInstance().getTime.getTime - instance.stage.datetime.getTime
    } else {
      instance.stage.state = status
      instance.stage.datetime = Calendar.getInstance().getTime
      instance.stage.duration = 0
    }

    instanceRepository.save(instance.to())
  }

  def deleteInstance(name: String): Unit = instanceRepository.delete(name)
}
