package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.crud.rest.common.InstanceRepositoryMock
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.{Injector, Module}

class InstanceDomainRenewerTestSuit extends FlatSpec with Matchers {

  it should "delete() method works properly" in new InstanceDomainRenewerMocks {
    //arrange
    val instanceName = getInstanceRepository.getAll.head.name

    //act
    instanceManager.deleteInstance(instanceName)

    //assert
    val deletedInstance = getInstanceRepository.get(instanceName)
    deletedInstance shouldBe None
  }

  //todo tests related to updating instance fields are not covered
}

trait InstanceDomainRenewerMocks extends MockitoSugar {
  private val instanceRepositoryMock = new InstanceRepositoryMock()

  private val connectionRepository = mock[ConnectionRepository]
  when(connectionRepository.getInstanceRepository).thenReturn(instanceRepositoryMock.repository)

  private val module = new Module {
    bind[ConnectionRepository] to connectionRepository
  }
  private val injector: Injector = module.injector

  val instanceManager = new InstanceDomainRenewer()(injector)

  def getInstanceRepository: GenericMongoRepository[InstanceDomain] = instanceRepositoryMock.repository
}
