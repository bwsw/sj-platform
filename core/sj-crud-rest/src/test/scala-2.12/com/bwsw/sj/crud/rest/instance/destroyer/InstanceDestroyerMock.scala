package com.bwsw.sj.crud.rest.instance.destroyer

import com.bwsw.common.http.HttpClient
import com.bwsw.common.marathon._
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.crud.rest.instance.{InstanceDestroyer, InstanceDomainRenewer}
import org.scalatest.mockito.MockitoSugar
import scaldi.Injector

class InstanceDestroyerMock(_marathonManager: MarathonApi,
                            _instanceManager: InstanceDomainRenewer,
                            instance: Instance)(implicit override val injector: Injector)
  extends InstanceDestroyer(instance, "stub", 100) with MockitoSugar {

  override protected val instanceManager: InstanceDomainRenewer = _instanceManager
  override protected val client: HttpClient = mock[HttpClient]
  override protected val marathonManager: MarathonApi = _marathonManager

  override protected def deleteFramework(): Unit = super.deleteFramework()

  override protected def waitForFrameworkToDelete(): Unit = super.waitForFrameworkToDelete()
}