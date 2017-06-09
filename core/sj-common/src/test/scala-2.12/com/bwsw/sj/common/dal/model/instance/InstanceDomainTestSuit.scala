package com.bwsw.sj.common.dal.model.instance


import com.bwsw.sj.common.utils.EngineLiterals
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

class InstanceDomainTestSuit extends FlatSpec with Matchers with PrivateMethodTester with MockitoSugar {
  it should s"getInputsWithoutStreamMode() method returns empty list for '${EngineLiterals.inputStreamingType}' instance" in {
    //arrange
    val inputInstanceDomain = mock[InputInstanceDomain]
    when(inputInstanceDomain.getInputsWithoutStreamMode).thenCallRealMethod()

    //act
    val inputs = inputInstanceDomain.getInputsWithoutStreamMode

    //assert
    inputs shouldBe empty
  }

  it should s"getInputsWithoutStreamMode() method returns cleared inputs for '${EngineLiterals.regularStreamingType}' instance" in {
    //arrange
    val numberOfInputs = 5
    val expectedInputs = Array.fill(numberOfInputs)("input")
    val inputsWithMode = expectedInputs.map(_ + "/" + EngineLiterals.splitStreamMode)
    val regularInstanceDomain = mock[RegularInstanceDomain]
    when(regularInstanceDomain.getInputsWithoutStreamMode).thenCallRealMethod()
    when(regularInstanceDomain.inputs).thenReturn(inputsWithMode)

    //act
    val inputs = regularInstanceDomain.getInputsWithoutStreamMode

    //assert
    inputs shouldBe expectedInputs
  }

  it should s"getInputsWithoutStreamMode() method returns cleared inputs for '${EngineLiterals.batchStreamingType}' instance" in {
    //arrange
    val numberOfInputs = 5
    val expectedInputs = Array.fill(numberOfInputs)("input")
    val inputsWithMode = expectedInputs.map(_ + "/" + EngineLiterals.splitStreamMode)
    val batchInstanceDomain = mock[BatchInstanceDomain]
    when(batchInstanceDomain.getInputsWithoutStreamMode).thenCallRealMethod()
    when(batchInstanceDomain.inputs).thenReturn(inputsWithMode)

    //act
    val inputs = batchInstanceDomain.getInputsWithoutStreamMode

    //assert
    inputs shouldBe expectedInputs
  }

  it should s"getInputsWithoutStreamMode() method returns cleared inputs for '${EngineLiterals.outputStreamingType}' instance" in {
    //arrange
    val numberOfInputs = 5
    val expectedInputs = Array.fill(numberOfInputs)("input")
    val inputsWithMode = expectedInputs.map(_ + "/" + EngineLiterals.splitStreamMode)
    val outputInstanceDomain = mock[OutputInstanceDomain]
    when(outputInstanceDomain.getInputsWithoutStreamMode).thenCallRealMethod()
    when(outputInstanceDomain.inputs).thenReturn(inputsWithMode)

    //act
    val inputs = outputInstanceDomain.getInputsWithoutStreamMode

    //assert
    inputs shouldBe expectedInputs
  }
}