//package com.bwsw.sj.crud.rest.validator.instance
//
//import com.bwsw.sj.common.dal.model.stream.StreamDomain
//import com.bwsw.sj.common.utils.EngineLiterals
//import org.scalatest.{FlatSpec, Matchers}
//
//import scala.collection.mutable.ArrayBuffer
//
//class InputInstanceValidatorTestSuit extends FlatSpec with Matchers {
//  private val defaultStreamMode = EngineLiterals.splitStreamMode
//
//  it should "not have implementation of validate() method by default" in new InstanceValidatorMocks {
//    //act and assert
//    assertThrows[NotImplementedError] {
//      instanceValidator.validate(null, null)
//    }
//  }
//
//  it should s"getStreamMode() method returns $defaultStreamMode if a stream hasn't got a stream mode" in new InstanceValidatorMocks {
//    //arrange
//    val streamWithoutMode = "stream_name"
//
//    //act
//    val streamMode: String = instanceValidator.getStreamMode(streamWithoutMode)
//
//    //assert
//    streamMode shouldBe defaultStreamMode
//  }
//
//  it should s"getStreamMode() method returns a stream mode of stream that is separated of stream name by '/' " in new InstanceValidatorMocks {
//    //arrange
//    val streamWithoutMode = "stream_name"
//    val actualStreamMode = "stream_mode"
//    val streamWithMode = actualStreamMode + "/" + streamWithoutMode
//
//    //act
//    val streamMode: String = instanceValidator.getStreamMode(streamWithMode)
//
//    //assert
//    streamMode shouldBe actualStreamMode
//  }
//
//  it should s"getStreamServices() method returns empty list if no streams are passed" in new InstanceValidatorMocks {
//    //arrange
//    val streams: ArrayBuffer[StreamDomain] = ArrayBuffer()
//
//    //act
//    val streamServices = instanceValidator.getStreamServices(streams)
//
//    //assert
//    streamServices shouldBe empty
//  }
//
//  it should s"getStreamServices() method returns the set of services names" in new InstanceValidatorMocks {
//    //arrange
//    val services: ArrayBuffer[String] = ArrayBuffer()
//    val streams = getStreamStorage.getAll
//    streams.foreach(services += _.service.name)
//
//    //act
//    val streamServices = instanceValidator.getStreamServices(streams)
//
//    //assert
//    services should contain theSameElementsAs streamServices
//  }
//}
//
//trait InstanceValidatorMocks extends MockitoSugar {
//  private val instanceRepositoryMock = new InstanceRepositoryMock()
//  private val serviceRepositoryMock = new ServiceRepositoryMock()
//  private val streamRepositoryMock = new StreamRepositoryMock()
//
//  private val connectionRepository = mock[ConnectionRepository]
//  when(connectionRepository.getInstanceRepository).thenReturn(instanceRepositoryMock.repository)
//  when(connectionRepository.getServiceRepository).thenReturn(serviceRepositoryMock.repository)
//  when(connectionRepository.getStreamRepository).thenReturn(streamRepositoryMock.repository)
//
//  private val module = new Module {
//    bind[ConnectionRepository] to connectionRepository
//  }
//  private val injector = module.injector
//
//  val instanceValidator = new InstanceValidator()(injector) {
//    override type T = Instance
//
//    /**
//      * Validating input parameters for streaming module
//      *
//      * @param instance - input parameters for running module
//      * @return - List of errors
//      */
//    override def validate(instance: Instance, specification: Specification) = ???
//
//    override protected def validateStreamOptions(instance: T, specification: Specification) = ???
//  }
//
//  def getServiceStorage: GenericMongoRepository[ServiceDomain] = serviceRepositoryMock.repository
//
//  def getStreamStorage: GenericMongoRepository[StreamDomain] = streamRepositoryMock.repository
//
//  def getInstanceStorage: GenericMongoRepository[InstanceDomain] = instanceRepositoryMock.repository
//}