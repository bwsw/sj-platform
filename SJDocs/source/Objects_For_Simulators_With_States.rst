Objects For Simulators With States
===============================================

.. Contents::

.. _Simulation_Result:

Simulation Result
------------------------

``case class PartitionData(partition: Int, dataList: Seq[AnyRef])`` - contains data elements that has been sent in a partition of an output stream.

``case class StreamData(stream: String, partitionDataList: Seq[PartitionData])`` - contains data elements that has been sent in an output stream.

``case class SimulationResult(streamDataList: Seq[StreamData], state: Map[String, Any])`` - contains data elements for each output stream and a state at a certain point in time.

Module Environment Manager Mock
--------------------------------------

It is a mock for StatefulModuleEnvironmentManager. Creates **PartitionedOutputMock** instead PartitionedOutput and **RoundRobinOutputMock** instead RoundRobinOutput.

.. csv-table:: Constructor arguments
 :header: "Argument", "Type", "Description"
 :widths: 25, 25, 50 

 "stateStorage", "StateStorage", "storage of state"
 "options", "String ", "user defined options from instance"
 "outputs", "Array[TStreamStreamDomain]", "list of output streams from instance"

Module Output Mocks
-----------------------------

They have a buffer that contains output elements listed in Simulation_Result_.

Provided methods:

- ``getOutputElements: mutable.Buffer[OutputElement]`` - returns a buffer with output elements.

- ``clear()`` - removes all output elements from a buffer.


Partitioned Output Mock
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The mock for ``PartitionedOutput`` provides an output stream that puts data into a specific partition.

Provided methods:

- ``put(data: AnyRef, partition: Int)`` - creates an output element with data and partition and puts it in a buffer.

Round Robin Output Mock
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The mock for ``RoundRobinOutput`` provides an output stream that puts data using the round-robin policy.

Provided methods:

- ``put(data: AnyRef)`` - creates an output element with data and next partition then puts it in a buffer.

