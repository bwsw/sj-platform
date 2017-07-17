Testing Modules on Simulators
================================

.. warning:: *The section is under development!*

Stream Juggler provides a user with a range of simulators for module testing purposes. A simulator is a ready-to-use environment that allows you to rapidly test modules during the development process.

Four types of simulators is provided - one per each module type. Choose that you want to use in accordance with the type of a module you want to test. And follow the instructions below.

.. _Input_Engine_Simulator:

Input Engine Simulator
-----------------------------

.. warning:: *The section is under development!*

It is a class for testing an implementation of an :ref:`input_module` (Executor).

Simulator imitates the behavior of the :ref:`Input_Streaming_Engine`: it sends byte buffer to Executor, gets input envelopes from it, checks envelopes on duplicate (if it is necessary), and builds :ref:`Input_Engine_Simulator_Output_Data`.

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
.. important:: T - is a type of data created by Executor 

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

.. important:: T - is a type of data created by Executor 

Usage example
~~~~~~~~~~~~~~~~~~~~~~~~~~

E.g. you implement your own Executor that splits byte buffer by a comma and tries parsing it to 'Integer'::

 class SomeExecutor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Integer](manager) {
  override def tokenize(buffer: ByteBuf): Option[Interval] = { ... }

  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope[Integer]] = { ... }
 }


If you want to see what Executor returns after processing, Input Engine Simulator can be used in the following way::
 
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

It is a class for testing an implementation of :ref:`regular_module` (Executor).

The simulator imitates the behavior of the :ref:`Regular_Streaming_Engine` (stateful mode): it sends envelopes to Executor, allows for invoking checkpoint's handlers, gets data from output streams and state.

Constructor arguments
~~~~~~~~~~~~~~~~~~~~~~~~~~
.. csv-table:: 
 :header: "Argument", "Type", "Description"
 :widths: 25, 25, 50 

 "``executor``", "RegularStreamingExecutor[T]", "Implementation of a :ref:`regular_module` under testing   
 "``manager``", "ModuleEnvironmentManagerMock", "Mock for StatefulModuleEnvironmentManager (see :ref:`Module-Environment-Manager-Mock`)"

.. note:: T - the type of data received by Executor.

Provided methods
~~~~~~~~~~~~~~~~~~~~~~~

* ``prepareState(state: Map[String, Any])`` - loads state in a state storage
 * ``state`` - key/value map
* ``prepareTstream(entities: Seq[T], stream: String, consumerName: String = "default-consumer-name"): Long`` - creates *one* t-stream envelope (``TStreamEnvelope[T]`` type) and saves it in a local buffer. Returns ID of the envelope.
 * ``entities`` - list of incoming data
 * ``stream`` - name of a stream with incoming data
 * ``consumerName`` - name of a consumer ('default-consumer-name' by default)
* ``prepareKafka(entity: T, stream: String): Long`` - creates *one* kafka envelope (``KafkaEnvelope[T]`` type) and saves it in a local buffer. Returns ID of that envelope.
 * ``entity`` - incoming data
 * ``stream`` - name of a stream with incoming data
* ``prepareKafka(entities: Seq[T], stream: String): Seq[Long]`` - creates a *list* of kafka envelopes (``KafkaEnvelope[T]`` type) - *one* envelope for *one* element from ``entities``, and saves it in a local buffer. Returns a list of envelope IDs.
 * ``entities`` - list of incoming data
 * ``stream`` - name of a stream with incoming data
* ``process(envelopesNumberBeforeIdle: Int = 0, clearBuffer: Boolean = true): SimulationResult`` - sends all envelopes from local buffer and returns output streams and state (see :ref:`Simulation-Result`).
 * ``envelopesNumberBeforeIdle`` - number of envelopes after which ``executor.onIdle()`` will be invoked ('0' by default). '0' means that ``executor.onIdle()`` will never be called.
 * ``clearBuffer`` - indicates that all envelopes will be removed from a local buffer after processing.
* ``beforeCheckpoint(isFullState: Boolean): SimulationResult`` - imitates the behavior of the :ref:`Regular_Streaming_Engine` before checkpoint: invokes ``executor.onBeforeCheckpoint()``, then invokes ``executor.onBeforeStateSave(isFullState)`` and returns output streams and state (see :ref:`Simulation-Result`).
 * ``isFullState`` - a flag denoting that the full state ('true') or partial changes of state ('false') have been saved. 
* ``timer(jitter: Long): SimulationResult`` - imitates that a timer went out (invokes ``executor.onTimer(jitter)``).
 * ``jitter`` - a delay between a real response time and an invocation of this handler.
* ``clear()`` - removes all envelopes from a local buffer.

Usage Example
~~~~~~~~~~~~~~~~~~~~~~~~~~

E.g. you implement your own Executor that takes strings and calculates their length::

 class SomeExecutor(manager: ModuleEnvironmentManager) extends RegularStreamingExecutor[String](manager) {
  private val state = manager.getState
  private val output = manager.getRoundRobinOutput("output")

  override def onIdle(): Unit = {
    val idleCalls = state.get("idleCalls").asInstanceOf[Int]
    state.set("idleCalls", idleCalls + 1)
    val symbols: Integer = state.get("symbols").asInstanceOf[Int]
    output.put(symbols)
  }

  override def onMessage(envelope: KafkaEnvelope[String]): Unit = {
    val symbols = state.get("symbols").asInstanceOf[Int]
    val length = envelope.data.length
    state.set("symbols", symbols + length)
  }

  override def onMessage(envelope: TStreamEnvelope[String]): Unit = {
    val symbols = state.get("symbols").asInstanceOf[Int]
    val length = envelope.data.toList.mkString.length
    state.set("symbols", symbols + length)
  }
 }

If you want to see what the executor puts into an output stream and to the state after processing, Regular Engine Simulator can be used in the following way::

 val stateSaver = mock(classOf[StateSaverInterface])
 val stateLoader = new StateLoaderMock
 val stateService = new RAMStateService(stateSaver, stateLoader)
 val stateStorage = new StateStorage(stateService)
 val options = ""
 val output = new TStreamStreamDomain("out", mock(classOf[TStreamServiceDomain]), 3, tags = Array("output"))
 val manager = new ModuleEnvironmentManagerMock(stateStorage, options, Array(output))
 val executor: RegularStreamingExecutor[String] = new SomeExecutor(manager)
 val tstreamInput = "t-stream-input"
 val kafkaInput = "kafka-input"

 val simulator = new RegularEngineSimulator(executor, manager)
 simulator.prepareState(Map("idleCalls" -> 0, "symbols" -> 0))
 simulator.prepareTstream(Seq("ab", "c", "de"), tstreamInput)
 simulator.prepareKafka(Seq("fgh", "g"), kafkaInput)
 simulator.prepareTstream(Seq("ijk", "lm"), tstreamInput)

 val envelopesNumberBeforeIdle = 2
 val results = simulator.process(envelopesNumberBeforeIdle)
 println(results)</code></pre>

``println(results)`` will print::
 
 SimulationResult(ArrayBuffer(StreamData(out,List(PartitionData(0,List(8)), PartitionData(1,List(14))))),Map(symbols -> 14, idleCalls -> 2))

The ``mock`` method is from the ``org.mockito.Mockito.mock`` library.

For more complicated examples see `sj-fping-process-test <https://github.com/bwsw/sj-fping-demo/blob/develop/ps-process/src/test/scala/com/bwsw/sj/examples/pingstation/module/regular/ExecutorTests.scala>`_.

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

.. _Module-Environment-Manager-Mock::

.. _Simulation-Result::
