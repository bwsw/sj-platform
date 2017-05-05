package com.bwsw.sj.crud.rest.instance

import java.util.Calendar

import com.bwsw.sj.common.dal.model.module.Instance
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.crud.rest.marathon.MarathonTask

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

  protected def getRestAddress(leaderTask: Option[MarathonTask]) =
    leaderTask.map(t => s"http://${t.host}:${t.ports.asInstanceOf[List[String]].head}")

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
