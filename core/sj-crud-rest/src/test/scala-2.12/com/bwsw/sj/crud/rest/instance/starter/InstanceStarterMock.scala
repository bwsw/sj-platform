package com.bwsw.sj.crud.rest.instance.starter

import com.bwsw.common.LeaderLatch
import com.bwsw.common.http.HttpClient
import com.bwsw.common.marathon._
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.crud.rest.instance.{InstanceDomainRenewer, InstanceStarter}
import org.scalatest.mockito.MockitoSugar
import scaldi.Injector

class InstanceStarterMock(_marathonManager: MarathonApi,
                          _instanceManager: InstanceDomainRenewer,
                          instance: Instance,
                          marathonAddress: String,
                          zookeeperHost: Option[String] = None,
                          zookeeperPort: Option[Int] = None)(implicit override val injector: Injector)
  extends InstanceStarter(instance, marathonAddress, zookeeperHost, zookeeperPort) with MockitoSugar {

  override protected val instanceManager: InstanceDomainRenewer = _instanceManager
  override protected val client: HttpClient = mock[HttpClient]
  override protected val marathonManager: MarathonApi = _marathonManager

  override def createLeaderLatch(marathonMaster: String): LeaderLatch = mock[LeaderLatch]

  override def startInstance(): Unit = super.startInstance()
}
