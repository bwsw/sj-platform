package com.bwsw.sj.crud.rest.instance

import java.util.Calendar

import com.bwsw.sj.common.SjInjector
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.crud.rest.marathon.MarathonTask
import scaldi.Injectable.inject

private[instance] trait InstanceManager extends InstanceMarathonManager with SjInjector {
  private val instanceRepository = inject[ConnectionRepository].getInstanceRepository

  def getFrameworkName(instance: Instance) = s"${instance.name}-${instance.frameworkId}"

  def updateInstanceStatus(instance: Instance, status: String): Unit = {
    instance.status = status
    instanceRepository.save(instance.to)
  }

  def updateInstanceRestAddress(instance: Instance, restAddress: String): Unit = {
    instance.restAddress = Option(restAddress)
    instanceRepository.save(instance.to)
  }

  def getRestAddress(leaderTask: Option[MarathonTask]): Option[String] =
    leaderTask.map(t => s"http://${t.host}:${t.ports.asInstanceOf[List[String]].head}")

  def updateFrameworkStage(instance: Instance, status: String): Unit = {
    if (instance.stage.state.equals(status)) {
      instance.stage.duration = Calendar.getInstance().getTime.getTime - instance.stage.datetime.getTime
    } else {
      instance.stage.state = status
      instance.stage.datetime = Calendar.getInstance().getTime
      instance.stage.duration = 0
    }

    instanceRepository.save(instance.to)
  }

  def deleteInstance(name: String): Unit = instanceRepository.delete(name)
}
