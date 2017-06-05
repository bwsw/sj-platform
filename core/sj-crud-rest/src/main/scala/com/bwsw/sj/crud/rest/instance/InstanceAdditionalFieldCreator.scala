package com.bwsw.sj.crud.rest.instance

import com.bwsw.common.marathon.MarathonTask
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.EngineLiterals

private[instance] object InstanceAdditionalFieldCreator {
  def getFrameworkName(instance: Instance) = s"${instance.name}-${instance.frameworkId}"

  def getRestAddress(leaderTask: Option[MarathonTask]): Option[String] =
    leaderTask.map(t => s"${EngineLiterals.httpPrefix}${t.host}:${t.ports.asInstanceOf[List[String]].head}")
}
