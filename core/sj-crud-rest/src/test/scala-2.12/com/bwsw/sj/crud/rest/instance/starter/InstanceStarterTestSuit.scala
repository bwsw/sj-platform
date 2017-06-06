package com.bwsw.sj.crud.rest.instance.starter

import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.charset.StandardCharsets

import com.bwsw.common.JsonSerializer
import com.bwsw.common.marathon._
import com.bwsw.sj.common.config.SettingsUtils
import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.utils.{EngineLiterals, FrameworkLiterals}
import com.bwsw.sj.crud.rest.common.InstanceRepositoryMock
import com.bwsw.sj.crud.rest.instance.starter.InstanceSettingsUtilsMock._
import com.bwsw.sj.crud.rest.instance.{InstanceAdditionalFieldCreator, InstanceDomainRenewer, InstanceStarter}
import com.bwsw.sj.crud.rest.utils.RestLiterals
import org.apache.http.{HttpEntity, HttpStatus, StatusLine}
import org.apache.http.client.methods.CloseableHttpResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.Mockito.{verify, times}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}
import scaldi.{Injector, Module}

class InstanceStarterTestSuit extends FlatSpec with Matchers with PrivateMethodTester {
  it should "hasFrameworkStarted() method returns true if count of running tasks is equal 1" in new InstanceStarterMocks {
    //arrange
    val emptyMarathonApplicationStub = MarathonApplication(MarathonApplicationInfo(null, Map(), 1, List(), null))
    val hasFrameworkStarted = PrivateMethod[Boolean]('hasFrameworkStarted)

    //act
    val frameworkStarted = instanceStarter invokePrivate hasFrameworkStarted(emptyMarathonApplicationStub)

    //assert
    frameworkStarted shouldBe true
  }

  it should "hasFrameworkStarted() method returns false if count of running tasks isn't equal 1" in new InstanceStarterMocks {
    //arrange
    val emptyMarathonApplicationStub = MarathonApplication(MarathonApplicationInfo(null, Map(), 5, List(), null))
    val hasFrameworkStarted = PrivateMethod[Boolean]('hasFrameworkStarted)

    //act
    val frameworkStarted = instanceStarter invokePrivate hasFrameworkStarted(emptyMarathonApplicationStub)

    //assert
    frameworkStarted shouldBe false
  }

  it should "getFrameworkEnvironmentVariables() method returns a set of environment variables containing " +
    s"${FrameworkLiterals.instanceIdLabel}, ${FrameworkLiterals.frameworkIdLabel}, ${FrameworkLiterals.mesosMasterLabel}, " +
    s"mongo envs and envs from instance" in new InstanceStarterMocks {
    //arrange
    val getFrameworkEnvironmentVariables = PrivateMethod[Map[String, String]]('getFrameworkEnvironmentVariables)

    //act
    val envs = instanceStarter invokePrivate getFrameworkEnvironmentVariables(master)

    //assert
    envs shouldBe Map(FrameworkLiterals.frameworkIdLabel -> frameworkName,
      FrameworkLiterals.instanceIdLabel -> instanceName,
      FrameworkLiterals.mesosMasterLabel -> master) ++ mongoEnv ++ instanceEnv
  }

  it should "getZooKeeperServers() method returns a zookeeper address that has been passed to InstanceStarter" in new InstanceStarterMocks {
    //arrange
    val zkHost = "localhost"
    val zkPort = "2181"
    val instanceStarterWithZk = new InstanceStarter(instanceMock, marathonAddress, Some(zkHost), Some(zkPort.toInt))(injector)
    val marathonMaster = s"zk://${zkHost}dummy:$zkPort/mesos"
    val getZooKeeperServers = PrivateMethod[String]('getZooKeeperServers)

    //act
    val zkServers = instanceStarterWithZk invokePrivate getZooKeeperServers(marathonMaster)

    //assert
    zkServers shouldBe (zkHost + ":" + zkPort)
  }

  it should "getZooKeeperServers() method returns a zookeeper address extracted from passed value (marathon master address) " +
    s"if there are no zookeeper settings" in new InstanceStarterMocks {
    //arrange
    val expectedZkHost = "host"
    val expectedZkPort = "2181"
    val marathonMaster = s"zk://$expectedZkHost:$expectedZkPort/mesos"
    val getZooKeeperServers = PrivateMethod[String]('getZooKeeperServers)

    //act
    val zkServers = instanceStarter invokePrivate getZooKeeperServers(marathonMaster)

    //assert
    zkServers shouldBe (expectedZkHost + ":" + expectedZkPort)
  }

  it should "createRequestForFrameworkCreation() method returns a proper marathon request to launch framework" in new InstanceStarterMocks {
    //arrange
    val zkHost = "host"
    val zkPort = "2181"
    val marathonMaster = s"zk://$zkHost:$zkPort/mesos"
    val restAddress: String = RestLiterals.createUri(crudRestHostStub, crudRestPortStub)
    val createRequestForFrameworkCreation = PrivateMethod[MarathonRequest]('createRequestForFrameworkCreation)

    //act
    val marathonRequest = instanceStarter invokePrivate createRequestForFrameworkCreation(marathonMaster)

    //assert
    marathonRequest shouldBe MarathonRequest(
      id = frameworkName,
      cmd = FrameworkLiterals.createCommandToLaunch(frameworkJarNameStub),
      instances = 1,
      env = Map(FrameworkLiterals.frameworkIdLabel -> frameworkName,
        FrameworkLiterals.instanceIdLabel -> instanceName,
        FrameworkLiterals.mesosMasterLabel -> marathonMaster) ++ mongoEnv ++ instanceEnv,
      uris = List(new URI(s"$restAddress/v1/custom/jars/$frameworkJarNameStub").toString),
      backoffSeconds = backoffSecondsStub,
      backoffFactor = backoffFactorStub,
      maxLaunchDelaySeconds = maxLaunchDelaySecondsStub
    )
  }

  it should "startInstance() method works properly if framework has been run without any exceptions" in new InstanceStarterMocks {
    //arrange
    val zkHost = "host"
    val zkPort = "2181"
    val marathonMaster = s"zk://$zkHost:$zkPort/mesos"
    val startInstance = PrivateMethod('startInstance)

    val okMarathonResponse = getClosableHttpResponseMock(marathonInfoStub, HttpStatus.SC_OK)
    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getMarathonInfo()).thenReturn(okMarathonResponse)
    when(marathonManager.getMarathonMaster(any())).thenReturn(master)
    when(marathonManager.getApplicationInfo(any())).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.scaleMarathonApplication(any(), any())).thenReturn(HttpStatus.SC_OK)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act and assert
    instanceStarterMock(marathonManager, instanceManager) invokePrivate startInstance()
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.starting)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.started)
  }

  it should "startInstance() method fails if framework hasn't been run" in new InstanceStarterMocks {
    //arrange
    val zkHost = "host"
    val zkPort = "2181"
    val marathonMaster = s"zk://$zkHost:$zkPort/mesos"
    val startInstance = PrivateMethod('startInstance)

    val okMarathonResponse = getClosableHttpResponseMock(marathonInfoStub, HttpStatus.SC_OK)
    val okFrameworkResponse = getClosableHttpResponseMock(failedMarathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getMarathonInfo()).thenReturn(okMarathonResponse)
    when(marathonManager.getMarathonMaster(any())).thenReturn(master)
    when(marathonManager.getApplicationInfo(any())).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(failedMarathonApplicationStub)
    when(marathonManager.scaleMarathonApplication(any(), any())).thenReturn(HttpStatus.SC_OK)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act and assert
    assertThrows[InterruptedException](instanceStarterMock(marathonManager, instanceManager) invokePrivate startInstance())
  }
}

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
  val marathonApplicationStub = MarathonApplication(marathonApplicationInfoStub)
  val failedMarathonApplicationStub = MarathonApplication(failedMarathonApplicationInfoStub)

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
}

class InstanceSettingsUtilsMock extends MockitoSugar {
  val settingsUtils: SettingsUtils = mock[SettingsUtils]

  when(settingsUtils.getFrameworkMaxLaunchDelaySeconds()).thenReturn(maxLaunchDelaySecondsStub)
  when(settingsUtils.getFrameworkBackoffFactor()).thenReturn(backoffFactorStub)
  when(settingsUtils.getFrameworkBackoffSeconds()).thenReturn(backoffSecondsStub)
  when(settingsUtils.getBackoffSettings()).thenReturn((backoffSecondsStub, backoffFactorStub, maxLaunchDelaySecondsStub))

  when(settingsUtils.getFrameworkJarName()).thenReturn(frameworkJarNameStub)

  when(settingsUtils.getCrudRestHost()).thenReturn(crudRestHostStub)
  when(settingsUtils.getCrudRestPort()).thenReturn(crudRestPortStub)
}

object InstanceSettingsUtilsMock {
  val (backoffSecondsStub, backoffFactorStub, maxLaunchDelaySecondsStub) = (5, 1.5, 1000)

  val frameworkJarNameStub = "framework.jar"
  val crudRestHostStub = "rest-host"
  val crudRestPortStub = 18080
}