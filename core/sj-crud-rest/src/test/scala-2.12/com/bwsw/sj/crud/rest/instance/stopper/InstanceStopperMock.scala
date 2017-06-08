package com.bwsw.sj.crud.rest.instance.stopper

import com.bwsw.common.http.HttpClient
import com.bwsw.common.marathon._
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.crud.rest.instance.{InstanceDomainRenewer, InstanceStopper}
import org.scalatest.mockito.MockitoSugar
import scaldi.Injector

class InstanceStopperMock(_marathonManager: MarathonApi,
                          _instanceManager: InstanceDomainRenewer,
                          instance: Instance,
                          marathonAddress: String)(implicit override val injector: Injector)
  extends InstanceStopper(instance, marathonAddress, 100) with MockitoSugar {

  override protected val instanceManager: InstanceDomainRenewer = _instanceManager
  override protected val client: HttpClient = mock[HttpClient]
  override protected val marathonManager: MarathonApi = _marathonManager

  override protected def markInstanceAsStopped(): Unit = super.markInstanceAsStopped()

  override protected def stopFramework(): Unit = super.stopFramework()

  override protected def waitForFrameworkToStop(): Unit = super.waitForFrameworkToStop()
}