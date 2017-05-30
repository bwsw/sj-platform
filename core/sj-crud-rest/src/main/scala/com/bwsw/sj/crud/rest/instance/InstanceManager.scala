package com.bwsw.sj.crud.rest.instance

import java.util.Calendar

import com.bwsw.sj.common.SjInjector
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.crud.rest.marathon.MarathonTask
import scaldi.Injectable.inject

private[instance] trait InstanceManager extends InstanceMarathonManager with SjInjector {
  protected val instanceRepository = inject[ConnectionRepository].getInstanceRepository

  def getFrameworkName(instance: Instance) = s"${instance.name}-${instance.frameworkId}"

  protected def updateInstanceStatus(instance: Instance, status: String) = {
    instance.status = status
    instanceRepository.save(instance.to)
  }

  protected def updateInstanceRestAddress(instance: Instance, restAddress: String) = {
    instance.restAddress = Option(restAddress)
    instanceRepository.save(instance.to)
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

    instanceRepository.save(instance.to)
  }
}
