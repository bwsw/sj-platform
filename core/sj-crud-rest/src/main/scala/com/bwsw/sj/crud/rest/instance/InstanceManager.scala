package com.bwsw.sj.crud.rest.instance

import java.util.Calendar

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

private[instance] trait InstanceManager extends InstanceMarathonManager {
  private val instanceDAO = ConnectionRepository.getInstanceService

  def getFrameworkName(instance: Instance) = s"${instance.name}-${instance.frameworkId}"

  protected def updateInstanceStatus(instance: Instance, status: String) = {
    instance.status = status
    instanceDAO.save(instance)
  }

  protected def updateInstanceRestAddress(instance: Instance, restAddress: String) = {
    instance.restAddress = restAddress
    instanceDAO.save(instance)
  }

  protected def getRestAddress(leaderTask: Option[MarathonTask]) = {
    var res: String = null
    if (leaderTask.isDefined) res = s"${leaderTask.get.host}:${leaderTask.get.ports.asInstanceOf[List[String]].head}"
    res
  }

  protected def updateFrameworkStage(instance: Instance, status: String) = {
    if (instance.stage.state.equals(status)) {
      instance.stage.duration = Calendar.getInstance().getTime.getTime - instance.stage.datetime.getTime
    } else {
      instance.stage.state = status
      instance.stage.datetime = Calendar.getInstance().getTime
      instance.stage.duration = 0
    }

    instanceDAO.save(instance)
  }
}
