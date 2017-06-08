package com.bwsw.sj.crud.rest.instance.starter

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.bwsw.common.JsonSerializer
import com.bwsw.common.marathon._
import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.FrameworkLiterals
import com.bwsw.sj.crud.rest.common.InstanceRepositoryMock
import com.bwsw.sj.crud.rest.instance.{InstanceAdditionalFieldCreator, InstanceDomainRenewer, InstanceSettingsUtilsMock, InstanceStarter}
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.{HttpEntity, HttpStatus, StatusLine}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import scaldi.{Injector, Module}

trait InstanceStarterMocks extends MockitoSugar {
  protected val marathonAddress = "http://host:8080"
  private val instanceRepositoryMock = new InstanceRepositoryMock()
  private val settingsUtilsMock = new InstanceSettingsUtilsMock()

  val mongoEnv: Map[String, String] = Map[String, String]("MONGO_ENV" -> "env")
  private val connectionRepository = mock[ConnectionRepository]
  when(connectionRepository.getInstanceRepository).thenReturn(instanceRepositoryMock.repository)
  when(connectionRepository.mongoEnvironment).thenReturn(mongoEnv)

  private val module = new Module {
    bind[ConnectionRepository] to connectionRepository
    bind[SettingsUtils] to settingsUtilsMock.settingsUtils
  }
  protected val injector: Injector = module.injector

  val instanceName = "instance-name"
  val frameworkId = "framework-id"
  val instanceEnv: Map[String, String] = Map[String, String]("INSTANCE_ENV" -> "env")
  val instanceMock: Instance = mock[Instance]
  when(instanceMock.name).thenReturn(instanceName)
  when(instanceMock.frameworkId).thenReturn(frameworkId)
  when(instanceMock.environmentVariables).thenReturn(instanceEnv)

  private val serializer = new JsonSerializer(true)

  val master = "zk://localhost:2181/mesos"
  val marathonConfigStub = MarathonConfig(master)
  val marathonInfoStub = MarathonInfo(marathonConfigStub)

  val marathonTasksStub = MarathonTask("id", "127.0.0.1", List(31045))
  private val frameworkIdStub = "framework_id"
  private val marathonTaskFailureStub = MarathonTaskFailure("127.0.0.1", "Abnormal executor termination", "TASK_FAILED", "2014-09-12T23:23:41.711Z")
  private val marathonApplicationInfoStub = MarathonApplicationInfo("id", Map(FrameworkLiterals.frameworkIdLabel -> frameworkIdStub), 1, List(marathonTasksStub), null)
  private val failedMarathonApplicationInfoStub = MarathonApplicationInfo("id", Map(FrameworkLiterals.frameworkIdLabel -> frameworkIdStub), 0, List(marathonTasksStub), marathonTaskFailureStub)
  private val notStartedMarathonApplicationInfoStub = MarathonApplicationInfo("id", Map(FrameworkLiterals.frameworkIdLabel -> frameworkIdStub), 0, List(marathonTasksStub), null)
  val marathonApplicationStub = MarathonApplication(marathonApplicationInfoStub)
  val failedMarathonApplicationStub = MarathonApplication(failedMarathonApplicationInfoStub)
  val notStartedMarathonApplicationStub = MarathonApplication(notStartedMarathonApplicationInfoStub)

  def getClosableHttpResponseMock(content: Serializable, status: Int): CloseableHttpResponse = {
    val statusLineMock = mock[StatusLine]
    when(statusLineMock.getStatusCode).thenReturn(status)

    val entity = getHttpEntityMock(content)

    val responseMock = mock[CloseableHttpResponse]
    when(responseMock.getStatusLine).thenReturn(statusLineMock)
    when(responseMock.getEntity).thenReturn(entity)

    responseMock
  }

  private def getHttpEntityMock(content: Serializable): HttpEntity = {
    val serializedContent = new ByteArrayInputStream(serializer.serialize(content).getBytes(StandardCharsets.UTF_8))

    val entityMock = mock[HttpEntity]
    when(entityMock.getContent).thenReturn(serializedContent)

    entityMock
  }

  val instanceStarter = new InstanceStarter(instanceMock, marathonAddress)(injector)

  def instanceStarterMock(marathonApi: MarathonApi = mock[MarathonApi], instanceManager: InstanceDomainRenewer = mock[InstanceDomainRenewer]): InstanceStarterMock = {
    new InstanceStarterMock(marathonApi, instanceManager, instanceMock, marathonAddress)(injector)
  }

  def getInstanceRepository: GenericMongoRepository[InstanceDomain] = instanceRepositoryMock.repository

  def getSettingsUtils: SettingsUtils = settingsUtilsMock.settingsUtils

  val frameworkName: String = InstanceAdditionalFieldCreator.getFrameworkName(instanceMock)

  val okStatus = HttpStatus.SC_OK
  val errorStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
}
