package com.bwsw.sj.crud.rest.instance

import java.util.Calendar

import com.bwsw.sj.common.dal.model.module.InstanceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.crud.rest.marathon.MarathonTask

private[instance] trait InstanceManager extends InstanceMarathonManager {
  private val instanceRepository = ConnectionRepository.getInstanceRepository

  def getFrameworkName(instance: InstanceDomain) = s"${instance.name}-${instance.frameworkId}"

  protected def updateInstanceStatus(instance: InstanceDomain, status: String) = {
    instance.status = status
    instanceRepository.save(instance)
  }

  protected def updateInstanceRestAddress(instance: InstanceDomain, restAddress: String) = {
    instance.restAddress = restAddress
    instanceRepository.save(instance)
  }

  protected def getRestAddress(leaderTask: Option[MarathonTask]) = {
    var res: String = null
    if (leaderTask.isDefined) res = s"http://${leaderTask.get.host}:${leaderTask.get.ports.asInstanceOf[List[String]].head}"
    res
  }

  protected def updateFrameworkStage(instance: InstanceDomain, status: String) = {
    if (instance.stage.state.equals(status)) {
      instance.stage.duration = Calendar.getInstance().getTime.getTime - instance.stage.datetime.getTime
    } else {
      instance.stage.state = status
      instance.stage.datetime = Calendar.getInstance().getTime
      instance.stage.duration = 0
    }

    instanceRepository.save(instance)
  }
}
