Testing Modules on Simulators
================================

.. warning:: *The section is under development!*

Stream Juggler provides a user with a range of simulators for module testing purposes. A simulator is a ready-to-use environment that allows you to rapidly test modules during the development process.

Four types of simulators is provided - one per each module type. Choose that you want to use in accordance with the type of a module you want to test. And follow the instructions below.

.. _Input_Engine_Simulator:

Input Engine Simulator
-----------------------------

.. warning:: *The section is under development!*

It is a class for testing an implementation of an :ref:`input_module`.

Simulator imitates the behavior of the :ref:`Input_Streaming_Engine`: sends byte buffer to the executor, gets input envelopes from it, checks envelopes on duplicate (if it is necessary), and builds :ref:`Input_Engine_Simulator_Output_Data`.

Constructor arguments
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. csv-table:: 
 :header: "Argument", "Type", "Description"
 :widths: 25, 25, 50  

 "executor*", "InputStreamingExecutor[T]", "Implementation of :ref:`input_module` under testing"
 "evictionPolicy*", "InputInstanceEvictionPolicy", "A field of an instance (:ref:`REST_API_Instance_Create`)"
 "separator", "String", "Delimeter between data records (empty string by default)"
 "charset", "Charset", "Encoding of incoming data (UTF-8 by default)"

.. note:: `*` - required field
.. important:: T - is a type of data created by the executor 

The data record is a string that will be parsed by ``executor.parse()`` to some entity.

The simulator provides the following methods:

* ``prepare(record: String)`` - writes one data record to a byte buffer.
* ``prepare(records: Seq[String])`` - writes a collection of data records to a byte buffer.
* ``process(duplicateCheck: Boolean, clearBuffer: Boolean = true): Seq[OutputData[T]]`` - sends byte buffer to the executor as long as it can tokenize the buffer. The ``duplicateCheck`` argument indicates that every envelope has to be checked on duplication, the ``clearBuffer`` argument indicates that byte buffer with data records has to be cleared after processing ('true' by default). Method returns list of :ref:`Input_Engine_Simulator_Output_Data`.
* ``clear()`` - clears a byte buffer with data records.

.. _Input_Engine_Simulator_Output_Data:

Output Data
~~~~~~~~~~~~~~~~~

Provides information on the processing of incoming data by the :ref:`input_module`.

.. csv-table:: 
 :header: "Field", "Format", "Description"
 :widths: 25, 25, 50  

 "inputEnvelope", "Option[InputEnvelope[T]]", "Result of the ``executor.parse()`` method"
 "isNotDuplicate", "Option[Boolean]", "Indicates that ``inputEnvelope`` is not a duplicate if ``inputEnvelope`` is defined; otherwise it is 'None' "
 "response", "InputStreamingResponse", "Response that will be sent to a client after an ``inputEnvelope`` has been processed"

.. important:: T - is a type of data created by the executor 

Usage example
~~~~~~~~~~~~~~~~~~~~~~~~~~

E.g. you implement your own executor that splits byte buffer by a comma and tries parsing it to 'Integer'::

 class SomeExecutor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Integer](manager) {
  override def tokenize(buffer: ByteBuf): Option[Interval] = { ... }

  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope[Integer]] = { ... }
 }


If you want to see what the executor returns after processing, Input Engine Simulator can be used in the following way::
 
 val manager: InputEnvironmentManager
 val executor = new SomeExecutor(manager)

 val hazelcastConfig = HazelcastConfig(600, 1, 1, EngineLiterals.lruDefaultEvictionPolicy, 100)
 val hazelcast = new HazelcastMock(hazelcastConfig)
 val evictionPolicy = InputInstanceEvictionPolicy(EngineLiterals.fixTimeEvictionPolicy, hazelcast)

 val simulator = new InputEngineSimulator(executor, evictionPolicy, ",")
 simulator.prepare(Seq("1", "2", "a", "3", "b")) // byte buffer in simulator will contain "1,2,a,3,b,"

 val outputDataList = simulator.process(duplicateCheck = true)
 println(outputDataList)


For more complicated examples see: `sj-csv-input-test <https://github.com/bwsw/sj-platform/blob/develop/contrib/sj-platform/sj-csv-input/src/test/scala/com/bwsw/sj/module/input/csv/CSVInputExecutorTests.scala>`_, `sj-regex-input-test <https://github.com/bwsw/sj-platform/blob/develop/contrib/sj-platform/sj-regex-input/src/test/scala/com/bwsw/sj/module/input/regex/RegexInputExecutorTests.scala>`_.

.. _Regular_Engine_Simulator:

Regular Engine Simulator
------------------------------

.. warning:: *The section is under development!*

.. _Batch_Engine_Simulator:

Batch Engine Simulator
-------------------------------

.. warning:: *The section is under development!*

.. _Output_Engine_Simulator:

Output Engine Simulator
----------------------------

.. warning:: *The section is under development!*

Object For Simulators With States
-------------------------------------

.. warning:: *The section is under development!*
