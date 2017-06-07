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
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}
import scaldi.{Injector, Module}

class InstanceStarterTestSuit extends FlatSpec with Matchers with PrivateMethodTester with InstanceStarterMocks {
  it should "hasFrameworkStarted() method returns true if count of running tasks is equal 1" in {
    //arrange
    val emptyMarathonApplicationStub = MarathonApplication(MarathonApplicationInfo(null, Map(), 1, List(), null))
    val hasFrameworkStarted = PrivateMethod[Boolean]('hasFrameworkStarted)

    //act
    val frameworkStarted = instanceStarter invokePrivate hasFrameworkStarted(emptyMarathonApplicationStub)

    //assert
    frameworkStarted shouldBe true
  }

  it should "hasFrameworkStarted() method returns false if count of running tasks isn't equal 1" in {
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
    s"mongo envs and envs from instance" in {
    //arrange
    val getFrameworkEnvironmentVariables = PrivateMethod[Map[String, String]]('getFrameworkEnvironmentVariables)

    //act
    val envs = instanceStarter invokePrivate getFrameworkEnvironmentVariables(master)

    //assert
    envs shouldBe Map(FrameworkLiterals.frameworkIdLabel -> frameworkName,
      FrameworkLiterals.instanceIdLabel -> instanceName,
      FrameworkLiterals.mesosMasterLabel -> master) ++ mongoEnv ++ instanceEnv
  }

  it should "getZooKeeperServers() method returns a zookeeper address that has been passed to InstanceStarter" in {
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
    s"if there are no zookeeper settings" in {
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

  it should "createRequestForFrameworkCreation() method returns a proper marathon request to launch framework" in {
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

  it should "startInstance() method works properly if framework has been run without any exceptions" in {
    //arrange
    val startInstance = PrivateMethod('startInstance)
    val restAddress = InstanceAdditionalFieldCreator.getRestAddress(Some(marathonTasksStub))

    val okMarathonResponse = getClosableHttpResponseMock(marathonInfoStub, HttpStatus.SC_OK)
    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getMarathonInfo()).thenReturn(okMarathonResponse)
    when(marathonManager.getMarathonMaster(any())).thenReturn(master)
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.scaleMarathonApplication(any(), any())).thenReturn(HttpStatus.SC_OK)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act and assert
    instanceStarterMock(marathonManager, instanceManager) invokePrivate startInstance()

    //assert
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.starting)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, restAddress)
  }

  it should s"startInstance() method doesn't start an instance (set '${EngineLiterals.failed}' status) if marathon has got some problems" in {
    //arrange
    val startInstance = PrivateMethod('startInstance)

    val notOkMarathonResponse = getClosableHttpResponseMock(marathonInfoStub, errorStatus)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getMarathonInfo()).thenReturn(notOkMarathonResponse)

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager) invokePrivate startInstance()

    //assert
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.failed)
  }

  it should "startFramework() method launches the existent marathon app if framework has been created earlier" in {
    //arrange
    val startFramework = PrivateMethod[String]('startFramework)
    val restAddress = InstanceAdditionalFieldCreator.getRestAddress(Some(marathonTasksStub))


    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.scaleMarathonApplication(any(), any())).thenReturn(HttpStatus.SC_OK)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager) invokePrivate startFramework(master)

    //assert
    verify(marathonManager, times(1)).scaleMarathonApplication(frameworkName, 1)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, restAddress)
  }

  it should "startFramework() method creates a new marathon app if framework hasn't been created earlier" in {
    //arrange
    val startFramework = PrivateMethod[String]('startFramework)
    val restAddress = InstanceAdditionalFieldCreator.getRestAddress(Some(marathonTasksStub))

    val notOkFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, errorStatus)
    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(notOkFrameworkResponse, okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.startMarathonApplication(any())).thenReturn(HttpStatus.SC_CREATED)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager) invokePrivate startFramework(master)

    //assert
    verify(marathonManager, times(1)).startMarathonApplication(any())
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, restAddress)
  }

  it should "launchFramework() method launches the existent marathon app" in {
    //arrange
    val launchFramework = PrivateMethod('launchFramework)
    val restAddress = InstanceAdditionalFieldCreator.getRestAddress(Some(marathonTasksStub))


    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.scaleMarathonApplication(any(), any())).thenReturn(HttpStatus.SC_OK)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager) invokePrivate launchFramework()

    //assert
    verify(marathonManager, times(1)).scaleMarathonApplication(frameworkName, 1)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, restAddress)
  }

  it should "launchFramework() method doesn't launch the existent marathon app " +
    "if marathon has got some problems with scaling of app" in {
    //arrange
    val launchFramework = PrivateMethod('launchFramework)

    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.scaleMarathonApplication(any(), any())).thenReturn(errorStatus)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager) invokePrivate launchFramework()

    //assert
    verify(marathonManager, times(1)).scaleMarathonApplication(frameworkName, 1)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.failed)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.failed)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, None)
  }

  it should "createFramework() method creates a new marathon app" in {
    //arrange
    val createFramework = PrivateMethod[String]('createFramework)
    val restAddress = InstanceAdditionalFieldCreator.getRestAddress(Some(marathonTasksStub))

    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.startMarathonApplication(any())).thenReturn(HttpStatus.SC_CREATED)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager) invokePrivate createFramework(master)

    //assert
    verify(marathonManager, times(1)).startMarathonApplication(any())
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, restAddress)
  }

  it should "createFramework() method doesn't create a new marathon app " +
    "if marathon has got some problems with creation process of app" in {
    //arrange
    val createFramework = PrivateMethod[String]('createFramework)

    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.startMarathonApplication(any())).thenReturn(errorStatus)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager) invokePrivate createFramework(master)

    //assert
    verify(marathonManager, times(1)).startMarathonApplication(any())
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.failed)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.failed)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, None)
  }

  it should "waitForFrameworkToStart() method works properly if there are no errors" in {
    //arrange
    val waitForFrameworkToStart = PrivateMethod('waitForFrameworkToStart)
    val restAddress = InstanceAdditionalFieldCreator.getRestAddress(Some(marathonTasksStub))

    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager) invokePrivate waitForFrameworkToStart()

    //assert
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, restAddress)
  }

  it should "waitForFrameworkToStart() method do multiple tries to wait until all tasks start if app tasks don't start the first time" in {
    //arrange
    val numberOfTries = 5
    val notStartedMarathonApps = Array.fill(numberOfTries - 1)(notStartedMarathonApplicationStub).toList
    val waitForFrameworkToStart = PrivateMethod('waitForFrameworkToStart)
    val restAddress = InstanceAdditionalFieldCreator.getRestAddress(Some(marathonTasksStub))

    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any()))
      .thenReturn(notStartedMarathonApplicationStub, notStartedMarathonApps.:+(marathonApplicationStub): _*)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager) invokePrivate waitForFrameworkToStart()

    //assert
    verify(instanceManager, times(numberOfTries)).updateFrameworkStage(instanceMock, EngineLiterals.starting)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, restAddress)
  }

  it should s"waitForFrameworkToStart() method set an instance status to '${EngineLiterals.failed}' and throws exception" +
    "if there has been some problems with creation/launching process of app" in {
    //arrange
    val waitForFrameworkToStart = PrivateMethod('waitForFrameworkToStart)

    val notOkFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, errorStatus)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(notOkFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act and assert
    assertThrows[Exception](instanceStarterMock(marathonManager, instanceManager) invokePrivate waitForFrameworkToStart())
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.failed)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, None)
  }

  it should s"waitForFrameworkToStart() method throws exception, " +
    s"but first destroys a marathon app and set an instance status to '${EngineLiterals.failed}'" +
    "if there has been some problems with launching process of app tasks" in {
    //arrange
    val waitForFrameworkToStart = PrivateMethod('waitForFrameworkToStart)

    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(failedMarathonApplicationStub)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act and assert
    assertThrows[InterruptedException](instanceStarterMock(marathonManager, instanceManager) invokePrivate waitForFrameworkToStart())
    verify(marathonManager, times(1)).destroyMarathonApplication(frameworkName)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.failed)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, None)
  }

  it should "run() method works properly if framework has been run without any exceptions" in {
    //arrange
    val restAddress = InstanceAdditionalFieldCreator.getRestAddress(Some(marathonTasksStub))
    val okMarathonResponse = getClosableHttpResponseMock(marathonInfoStub, HttpStatus.SC_OK)
    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getMarathonInfo()).thenReturn(okMarathonResponse)
    when(marathonManager.getMarathonMaster(any())).thenReturn(master)
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse)
    when(marathonManager.getApplicationEntity(any())).thenReturn(marathonApplicationStub)
    when(marathonManager.scaleMarathonApplication(any(), any())).thenReturn(HttpStatus.SC_OK)
    when(marathonManager.getLeaderTask(any())).thenReturn(Some(marathonTasksStub))

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager).run()

    //assert
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.starting)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.starting)
    verify(instanceManager, times(1)).updateFrameworkStage(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.started)
    verify(instanceManager, times(1)).updateInstanceRestAddress(instanceMock, restAddress)
  }

  it should s"run() method doesn't start an instance (set '${EngineLiterals.failed}' status) if there are some exceptions during start process" in {
    //arrange
    val okMarathonResponse = getClosableHttpResponseMock(marathonInfoStub, HttpStatus.SC_OK)
    val okFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, HttpStatus.SC_OK)
    val notOkFrameworkResponse = getClosableHttpResponseMock(marathonApplicationStub, errorStatus)
    val marathonManager = mock[MarathonApi]
    when(marathonManager.getMarathonInfo()).thenReturn(okMarathonResponse)
    when(marathonManager.getMarathonMaster(any())).thenReturn(master)
    when(marathonManager.getApplicationInfo(frameworkName)).thenReturn(okFrameworkResponse, notOkFrameworkResponse)
    when(marathonManager.scaleMarathonApplication(any(), any())).thenReturn(HttpStatus.SC_OK)

    val instanceManager = mock[InstanceDomainRenewer]

    //act
    instanceStarterMock(marathonManager, instanceManager).run()

    //assert
    verify(instanceManager, times(1)).updateInstanceStatus(instanceMock, EngineLiterals.failed)
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

  val errorStatus = HttpStatus.SC_INTERNAL_SERVER_ERROR
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