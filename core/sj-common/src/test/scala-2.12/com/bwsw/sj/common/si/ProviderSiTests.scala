package com.bwsw.sj.common.si

import java.util.UUID

import com.bwsw.sj.common.dal.model.provider.{JDBCProviderDomain, ProviderDomain}
import com.bwsw.sj.common.dal.model.service._
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.provider.Provider
import com.bwsw.sj.common.si.result._
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import scaldi.Module

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class ProviderSiTests extends FlatSpec with Matchers {

  "ProviderSI" should "create correct provider" in new ProviderMocks {
    val correctProviderName = "correct-provider"
    val correctProviderDomain = mock[ProviderDomain]
    when(correctProviderDomain.name).thenReturn(correctProviderName)
    val correctProvider = mock[Provider]
    when(correctProvider.name).thenReturn(correctProviderName)
    when(correctProvider.to()).thenReturn(correctProviderDomain)
    when(correctProvider.validate()).thenReturn(ArrayBuffer[String]())

    providerSI.create(correctProvider) shouldBe Created
    providerStorage.toSet shouldBe (initProviderStorage + correctProviderDomain)
  }

  it should "not create incorrect provider" in new ProviderMocks {
    val errors = ArrayBuffer("Not valid")
    val incorrectProviderName = "incorrect-provider"
    val incorrectProviderDomain = mock[ProviderDomain]
    when(incorrectProviderDomain.name).thenReturn(incorrectProviderName)
    val incorrectProvider = mock[Provider]
    when(incorrectProvider.name).thenReturn(incorrectProviderName)
    when(incorrectProvider.to()).thenReturn(incorrectProviderDomain)
    when(incorrectProvider.validate()).thenReturn(errors)

    providerSI.create(incorrectProvider) shouldBe NotCreated(errors)
    providerStorage.toSet shouldBe initProviderStorage
  }

  it should "return related services if provider has them" in new ProviderMocksWithServices {
    val related = providerSI.getRelated(providerWithServicesName)
    related.map(_.toSet) shouldBe Right(relatedServices)
  }

  it should "return empty array if provider does not have related services" in new ProviderMocksWithServices {
    val related = providerSI.getRelated(providerWithoutServicesName)
    related shouldBe Right(mutable.Buffer.empty[String])
  }

  it should "return Left(false) if provider does not exists" in new ProviderMocksWithServices {
    val related = providerSI.getRelated(notExistsProviderName)
    related shouldBe Left(false)
  }

  it should "delete provider if it does not have related services" in new ProviderMocksWithServices {
    providerSI.delete(providerWithoutServicesName) shouldBe Deleted
    providerStorage.toSet shouldBe (initProviderStorage - providerWithoutServicesDomain)
  }

  it should "not delete provider if it does not exists" in new ProviderMocksWithServices {
    providerSI.delete(notExistsProviderName) shouldBe EntityNotFound
    providerStorage.toSet shouldBe initProviderStorage
  }

  it should "not delete provider if it have related services" in new ProviderMocksWithServices {
    val deletionError = s"Cannot delete provider '$providerWithServicesName'. Provider is used in services."

    providerSI.delete(providerWithServicesName) shouldBe DeletionError(deletionError)
    providerStorage.toSet shouldBe initProviderStorage
  }

  it should "return Right(true) if provider can connect" in new ProviderMocks {
    val successConnectionProviderName = "success-connection-provider"
    val successConnectionProviderDomain = mock[ProviderDomain]
    when(successConnectionProviderDomain.name).thenReturn(successConnectionProviderName)
    when(successConnectionProviderDomain.checkConnection()(injector)).thenReturn(ArrayBuffer[String]())
    providerStorage += successConnectionProviderDomain

    providerSI.checkConnection(successConnectionProviderName) shouldBe Right(true)
  }

  it should "return Left(errors) if provider cannot connect" in new ProviderMocks {
    val errors = ArrayBuffer("Conection error")
    val failedConnectionProviderName = "failed-connection-provider"
    val failedConnectionProviderDomain = mock[ProviderDomain]
    when(failedConnectionProviderDomain.name).thenReturn(failedConnectionProviderName)
    when(failedConnectionProviderDomain.checkConnection()(injector)).thenReturn(errors)
    providerStorage += failedConnectionProviderDomain

    providerSI.checkConnection(failedConnectionProviderName) shouldBe Left(errors)
  }

  it should "return Right(false) if provider does not exists" in new ProviderMocks {
    providerSI.checkConnection(notExistsProviderName) shouldBe Right(false)
  }
}

trait ProviderMocks extends MockitoSugar {
  val notExistsProviderName = "not-exist-provider"

  val initProviderStorageSize = 10
  val providerStorage: mutable.Buffer[ProviderDomain] = Range(0, initProviderStorageSize).map { _ =>
    val p = mock[ProviderDomain]
    when(p.name).thenReturn(UUID.randomUUID().toString)
    p
  }.toBuffer
  val initProviderStorage: Set[ProviderDomain] = providerStorage.toSet

  val providerRepository = mock[GenericMongoRepository[ProviderDomain]]
  when(providerRepository.getAll).thenReturn({
    providerStorage
  })
  when(providerRepository.save(any[ProviderDomain]()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val providerDomain = invocationOnMock.getArgument[ProviderDomain](0)
      providerStorage += providerDomain
    })
  when(providerRepository.delete(anyString()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val providerName = invocationOnMock.getArgument[String](0)
      val providerDomain = providerStorage.find(_.name == providerName).get
      providerStorage -= providerDomain
    })
  when(providerRepository.get(anyString()))
    .thenAnswer((invocationOnMock: InvocationOnMock) => {
      val providerName = invocationOnMock.getArgument[String](0)
      providerStorage.find(_.name == providerName)
    })

  val connectionRepository = mock[ConnectionRepository]
  when(connectionRepository.getProviderRepository).thenReturn(providerRepository)

  val module = new Module {
    bind[ConnectionRepository] to connectionRepository
  }
  val injector = module.injector
  val providerSI = new ProviderSI()(injector)
}

trait ProviderMocksWithServices extends ProviderMocks {
  val providerWithoutServicesName = "provider-without-services"
  val providerWithoutServicesDomain = mock[ProviderDomain]
  when(providerWithoutServicesDomain.name).thenReturn(providerWithoutServicesName)

  val providerWithServicesName = "provider-with-services"
  val providerWithServicesDomain = mock[ProviderDomain]
  when(providerWithServicesDomain.name).thenReturn(providerWithServicesName)

  val otherProviderName = "other-provider"
  val otherProviderDomain = mock[ProviderDomain]
  when(otherProviderDomain.name).thenReturn(otherProviderName)

  val jdbcProviderName = "jdbc-provider"
  val jdbcProviderDomain = mock[JDBCProviderDomain]
  when(jdbcProviderDomain.name).thenReturn(jdbcProviderName)

  providerStorage ++= mutable.Buffer(
    providerWithServicesDomain,
    providerWithoutServicesDomain,
    otherProviderDomain,
    jdbcProviderDomain)

  val esServiceRelatedName = "esServiceRelated"
  val esServiceRelated = mock[ESServiceDomain]
  when(esServiceRelated.name).thenReturn(esServiceRelatedName)
  when(esServiceRelated.provider).thenReturn(providerWithServicesDomain)
  val zkServiceRelatedName = "zkServiceRelated"
  val zkServiceRelated = mock[ZKServiceDomain]
  when(zkServiceRelated.name).thenReturn(zkServiceRelatedName)
  when(zkServiceRelated.provider).thenReturn(providerWithServicesDomain)
  val kfkServiceRelatedName = "kfkServiceRelated"
  val kfkServiceRelated = mock[KafkaServiceDomain]
  when(kfkServiceRelated.name).thenReturn(kfkServiceRelatedName)
  when(kfkServiceRelated.provider).thenReturn(providerWithServicesDomain)
  when(kfkServiceRelated.zkProvider).thenReturn(otherProviderDomain)

  val relatedServices = Set(esServiceRelatedName, zkServiceRelatedName, kfkServiceRelatedName)

  val restServiceNotRelatedName = "restServiceNotRelated"
  val restServiceNotRelated = mock[RestServiceDomain]
  when(restServiceNotRelated.name).thenReturn(restServiceNotRelatedName)
  when(restServiceNotRelated.provider).thenReturn(otherProviderDomain)
  val tServiceNotRelatedName = "tServiceNotRelated"
  val tServiceNotRelated = mock[TStreamServiceDomain]
  when(tServiceNotRelated.name).thenReturn(tServiceNotRelatedName)
  when(tServiceNotRelated.provider).thenReturn(otherProviderDomain)
  val jdbcServiceNotRelatedName = "jdbcServiceNotRelated"
  val jdbcServiceNotRelated = mock[JDBCServiceDomain]
  when(jdbcServiceNotRelated.name).thenReturn(jdbcServiceNotRelatedName)
  when(jdbcServiceNotRelated.provider).thenReturn(jdbcProviderDomain)

  val notRelatedServices = Set(restServiceNotRelatedName, tServiceNotRelatedName, jdbcServiceNotRelatedName)

  val serviceStorage = mutable.Buffer[ServiceDomain](
    esServiceRelated,
    zkServiceRelated,
    kfkServiceRelated,
    restServiceNotRelated,
    tServiceNotRelated,
    jdbcServiceNotRelated)

  val serviceRepository = mock[GenericMongoRepository[ServiceDomain]]
  when(serviceRepository.getAll).thenReturn(serviceStorage)

  when(connectionRepository.getServiceRepository).thenReturn(serviceRepository)

  override val initProviderStorage: Set[ProviderDomain] = providerStorage.toSet
  override val providerSI = new ProviderSI()(injector)
}
