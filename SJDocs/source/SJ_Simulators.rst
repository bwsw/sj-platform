Testing Modules on Simulators
================================

Stream Juggler Platform provides a user with a range of simulators for module testing purposes. A simulator is a ready-to-use environment that allows you to rapidly test modules in the development process.

Four types of simulators are provided - one per each module type. Choose which you want to use in accordance with the type of a module you want to test. And follow the instructions below.

.. _Input_Engine_Simulator:

Input Engine Simulator
-----------------------------

It is a class for testing the implementation of an `input module <http://streamjuggler.readthedocs.io/en/develop/SJ_Modules.html#input-module>`_ (Executor).

Simulator imitates the behavior of the :ref:`Input_Streaming_Engine`: it sends byte buffer to Executor, gets input envelopes from it, checks envelopes for duplicates (if it is necessary), and builds :ref:`Input_Engine_Simulator_Output_Data`.

To use the simulator you need to add the dependency to the ``build.sbt``::

 resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots" 
 libraryDependencies += "com.bwsw" %% "sj-engine-simulators" % "1.0-SNAPSHOT" % "test"

Constructor arguments
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. csv-table:: 
 :header: "Argument", "Type", "Description"
 :widths: 25, 25, 50  

 "executor*", "InputStreamingExecutor[T]", "Implementation of the `input module <http://streamjuggler.readthedocs.io/en/develop/SJ_Modules.html#input-module>`_ under testing"
 "evictionPolicy*", "InputInstanceEvictionPolicy", "A field of an instance (see :ref:`REST_API_Instance_Create`)"
 "separator", "String", "Delimeter between data records (empty string by default)"
 "charset", "Charset", "Encoding of incoming data (UTF-8 by default)"

.. note:: `*` - a required field
.. important:: T - is a type of data created by Executor 

The data record is a string that will be parsed by ``executor.parse()`` to some entity.

The simulator provides the following methods:

* ``prepare(record: String)`` - writes one data record to a byte buffer.
* ``prepare(records: Seq[String])`` - writes a collection of data records to a byte buffer.
* ``process(duplicateCheck: Boolean, clearBuffer: Boolean = true): Seq[OutputData[T]]`` - sends byte buffer to the executor as long as it can tokenize the buffer. The ``duplicateCheck`` argument indicates that every envelope has to be checked on duplication. The ``clearBuffer`` argument indicates that byte buffer with data records has to be cleared after processing ('true' by default). The method returns list of :ref:`Input_Engine_Simulator_Output_Data`.
* ``clear()`` - clears a byte buffer with data records.

.. _Input_Engine_Simulator_Output_Data:

Output Data
~~~~~~~~~~~~~~~~~

This simulator provides information on the processing of incoming data by the  `input module <http://streamjuggler.readthedocs.io/en/develop/SJ_Modules.html#input-module>`_.

.. csv-table:: 
 :header: "Field", "Format", "Description"
 :widths: 25, 25, 50  

 "inputEnvelope", "Option[InputEnvelope[T]]", "Result of the ``executor.parse()`` method"
 "isNotDuplicate", "Option[Boolean]", "Indicates that ``inputEnvelope`` is not a duplicate if ``inputEnvelope`` is defined; otherwise it is 'None'"
 "response", "InputStreamingResponse", "Response that will be sent to a client after an ``inputEnvelope`` has been processed"

.. important:: T - is a type of data created by Executor 

Usage example
~~~~~~~~~~~~~~~~~~~~~~~~~~

E.g. you have your own Executor that splits byte buffer by a comma and tries to parse it into 'Integer'::

 class SomeExecutor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Integer](manager) {
  override def tokenize(buffer: ByteBuf): Option[Interval] = { ... }

  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope[Integer]] = { ... }
 }


If you want to see what is returned by Executor after processing, Input Engine Simulator can be used in the following way::
 
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

It is a class for testing the implementation of a `regular module <http://streamjuggler.readthedocs.io/en/develop/SJ_Modules.html#regular-module>`_ (Executor).

The simulator imitates the behavior of the :ref:`Regular_Streaming_Engine` (stateful mode): it sends envelopes to Executor, allows invoking checkpoint's handlers, gets data from output streams and state.

To use the simulator you need to add the dependency to the ``build.sbt``::
 
 resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots" 
 libraryDependencies += "com.bwsw" %% "sj-engine-simulators" % "1.0-SNAPSHOT" % "test"

Constructor arguments
~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table:: 
 :header: "Argument", "Type", "Description"
 :widths: 25, 25, 50 

 "executor", "RegularStreamingExecutor[T]", "Implementation of the `regular module <http://streamjuggler.readthedocs.io/en/develop/SJ_Modules.html#regular-module>`_ under testing"   
 "manager", "ModuleEnvironmentManagerMock", "Mock for StatefulModuleEnvironmentManager (see :ref:`Module-Environment-Manager-Mock`)"

.. important:: T - the type of data received by Executor.

Provided methods
~~~~~~~~~~~~~~~~~~~~~~~

* ``prepareState(state: Map[String, Any])`` - loads state into a state storage.
 * ``state`` - key/value map.
* ``prepareTstream(entities: Seq[T], stream: String, consumerName: String = "default-consumer-name"): Long`` - creates *one* t-stream envelope (``TStreamEnvelope[T]`` type) and saves it in a local buffer. Returns an ID of the envelope.
 * ``entities`` - list of incoming data elements.
 * ``stream`` - name of a stream with incoming data.
 * ``consumerName`` - name of a consumer ('default-consumer-name' by default).
* ``prepareKafka(entity: T, stream: String): Long`` - creates *one* kafka envelope (``KafkaEnvelope[T]`` type) and saves it in a local buffer. Returns an ID of that envelope.
 * ``entity`` - an incoming data element.
 * ``stream`` - name of a stream with incoming data.
* ``prepareKafka(entities: Seq[T], stream: String): Seq[Long]`` - creates a *list* of kafka envelopes (``KafkaEnvelope[T]`` type) - *one* envelope for *each* element from ``entities``, and saves it in a local buffer. Returns a list of envelopes' IDs.
 * ``entities`` - list of incoming data elements.
 * ``stream`` - name of a stream with incoming data.
* ``process(envelopesNumberBeforeIdle: Int = 0, clearBuffer: Boolean = true): SimulationResult`` - sends all envelopes from local buffer and returns output streams and state (see :ref:`Simulation-Result`).
 * ``envelopesNumberBeforeIdle`` - number of envelopes after which ``executor.onIdle()`` will be invoked ('0' by default). '0' means that ``executor.onIdle()`` will never be called.
 * ``clearBuffer`` - indicates that all envelopes will be removed from a local buffer after processing.
* ``beforeCheckpoint(isFullState: Boolean): SimulationResult`` - imitates the behavior of the :ref:`Regular_Streaming_Engine` before checkpoint: invokes ``executor.onBeforeCheckpoint()``, then invokes ``executor.onBeforeStateSave(isFullState)`` and returns output streams and state (see :ref:`Simulation-Result`).
 * ``isFullState`` - a flag denoting that either the full state ('true') or a partial change of state ('false') is going to be saved. 
* ``timer(jitter: Long): SimulationResult`` - imitates that a timer went out (invokes ``executor.onTimer(jitter)``).
 * ``jitter`` - a delay between a real response time and an invocation of this handler.
* ``clear()`` - removes all envelopes from a local buffer.

Usage Example
~~~~~~~~~~~~~~~~~~~~~~~~~~

E.g. you have your own Executor that takes strings and calculates their length::

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
 val tStreamsSenderThreadMock = new TStreamsSenderThreadMock(Set(output.name))
 val manager = new ModuleEnvironmentManagerMock(stateStorage, options, Array(output), tStreamsSenderThreadMock)
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
 println(results)

``println(results)`` will print::
 
 SimulationResult(ArrayBuffer(StreamData(out,List(PartitionData(0,List(8)), PartitionData(1,List(14))))),Map(symbols -> 14, idleCalls -> 2))

The ``mock`` method is from the ``org.mockito.Mockito.mock`` library.

To see more complicated examples, please, visit `sj-fping-process-test <https://github.com/bwsw/sj-fping-demo/blob/develop/ps-process/src/test/scala/com/bwsw/sj/examples/pingstation/module/regular/ExecutorTests.scala>`_.

.. _Batch_Engine_Simulator:

Batch Engine Simulator
-------------------------------

It is a class for testing the implementation of a `batch module <http://streamjuggler.readthedocs.io/en/develop/SJ_Modules.html#batch-module>`_ (Executor).

The simulator imitates the behavior of the :ref:`Batch_Streaming_Engine` (stateful mode): it collects data elements in batches, then collects batches in a window, sends data in a window to the Executor, allows invoking checkpoint's handlers, gets data from output streams and state.

To use simulator you need to add this dependency to the ``build.sbt``::

 resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots" 
 libraryDependencies += "com.bwsw" %% "sj-engine-simulators" % "1.0-SNAPSHOT" % "test"

Constructor arguments
~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. csv-table:: 
 :header: "Argument", "Type", "Description"
 :widths: 25, 25, 50 

 "executor", "BatchStreamingExecutor[T]", "Implementation of the `batch module <http://streamjuggler.readthedocs.io/en/develop/SJ_Modules.html#batch-module>`_ under testing"
 "manager", "ModuleEnvironmentManagerMock", "Mock for `StatefulModuleEnvironmentManager` (see :ref:`Module-Environment-Manager-Mock`)"
 "batchCollector", "BatchCollector", "Implementation of :ref:`Batch-Collector`"

.. important:: T - the type of data received by Executor

Provided methods
~~~~~~~~~~~~~~~~~~~~~~~~~

* ``prepareState(state: Map[String, Any])`` - loads state into a state storage.
 - ``state`` - key/value map.

* ``prepareTstream(entities: Seq[T], stream: String, consumerName: String = "default-consumer-name"): Long`` - creates *one* t-stream envelope (``TStreamEnvelope[T]`` type) and saves it in a local buffer. Returns an ID of the envelope.
 - ``entities`` - the list of incoming data elements.
 - ``stream`` - the name of a stream with incoming data.
 - ``consumerName`` - the name of a consumer ('default-consumer-name' by default).

* ``prepareKafka(entity: T, stream: String): Long`` - creates *one* kafka envelope ('KafkaEnvelope[T]' type) and saves it in a local buffer. Returns an ID of that envelope.
 - ``entity`` - an incoming data element.
 - ``stream`` - the name of a stream with incoming data.

* ``prepareKafka(entities: Seq[T], stream: String): Seq[Long]`` - creates a *list* of kafka envelopes ('KafkaEnvelope[T]' type) - *one* envelope for *each* element from ``entities``, and saves it in a local buffer. Returns a list of envelopes' IDs.
 - ``entities`` - the list of incoming data elements.
 - ``stream`` - the name of a stream of incoming data.

* ``process(batchesNumberBeforeIdle: Int = 0, window: Int, slidingInterval: Int, saveFullState: Boolean = false, removeProcessedEnvelopes: Boolean = true): BatchSimulationResult`` - sends all envelopes from local buffer and returns output streams, state and envelopes that haven't been processed (see :ref:`Batch-Simulation-Result`). This method retrieves batches using ``batchCollector``. Then it creates a window repository and invokes the Executor methods for every stage of the processing cycle. The methods are invoked in the following order: ``onWindow``, ``onEnter``, ``onLeaderEnter``, ``onBeforeCheckpoint``, ``onBeforeStateSave``. At the end of this method all envelopes will be removed from ``batchCollector``.
 - ``batchesNumberBeforeIdle`` - the number of retrieved batches between invocations of ``executor.onIdle()`` ('0' by default). '0' means that ``executor.onIdle()`` will never be called.
 - ``window`` - count of batches that will be contained into a window (see :ref:`Batch-streaming_instance_fields`).
 - ``slidingInterval`` - the interval at which a window will be shifted (count of processed batches that will be removed from the window) (see :ref:`Batch-streaming_instance_fields`).
 - ``saveFullState`` - the flag denoting that either the full state ('true') or a partial change of the state ('false') is going to be saved after every checkpoint.
 - ``removeProcessedEnvelopes`` - indicates that all processed envelopes will be removed from a local buffer after processing.

* ``beforeCheckpoint(isFullState: Boolean): SimulationResult`` - imitates the behavior of the :ref:`Batch_Streaming_Engine` before checkpoint: invokes ``executor.onBeforeCheckpoint()``, then invokes ``executor.onBeforeStateSave(isFullState)`` and returns output streams and state (see :ref:`Simulation-Result`).
 - ``isFullState`` - the flag denoting that either the full state ('true') or partial changes of state ('false') is going to be saved.

* ``timer(jitter: Long): SimulationResult`` - imitates that a timer went out (invokes ``executor.onTimer(jitter)``).
 - ``jitter`` - the delay between a real response time and an invocation of this handler.

* ``clear()`` - removes all envelopes from a local buffer.

.. _Batch-Simulation-Result:

Batch Simulation Result
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After invocation of the ``process`` method some envelopes could remain not processed by Executor when there are not enough batches for completing a window.

``case class BatchSimulationResult(simulationResult: SimulationResult, remainingEnvelopes: Seq[Envelope])`` - contains output streams, state (see :ref:`Simulation-Result`) (``simulationResult``) and envelopes that haven't been processed (``remainingEnvelopes``).

Usage Example
~~~~~~~~~~~~~~~~~~~~~~

E.g. you have your own Executor that takes strings and calculates their length::

 class SomeExecutor(manager: ModuleEnvironmentManager) extends BatchStreamingExecutor[String](manager) {
  private val state = manager.getState
  private val output = manager.getRoundRobinOutput("out")

  override def onIdle(): Unit = {
    val idleCalls = state.get("idleCalls").asInstanceOf[Int]
    state.set("idleCalls", idleCalls + 1)
  }

  override def onWindow(windowRepository: WindowRepository): Unit = {
    val symbols = state.get("symbols").asInstanceOf[Int]

    val batches = {
      if (symbols == 0)
        windowRepository.getAll().values.flatMap(_.batches)
      else
        windowRepository.getAll().values.flatMap(_.batches.takeRight(windowRepository.slidingInterval))
    }

    val length = batches.flatMap(_.envelopes).map {
      case t: TStreamEnvelope[String] =>
        t.data.dequeueAll(_ => true).mkString
      case k: KafkaEnvelope[String] =>
        k.data
    }.mkString.length
    state.set("symbols", symbols + length)
  }

  override def onBeforeCheckpoint(): Unit = {
    val symbols: Integer = state.get("symbols").asInstanceOf[Int]
    output.put(symbols)
  }
 }
 
If you want to see what the Executor puts into an output stream and into the state after processing, Batch Engine Simulator can be used in the following way::

 val stateSaver = mock(classOf[StateSaverInterface])
 val stateLoader = new StateLoaderMock
 val stateService = new RAMStateService(stateSaver, stateLoader)
 val stateStorage = new StateStorage(stateService)
 val options = ""
 val output = new TStreamStreamDomain("out", mock(classOf[TStreamServiceDomain]), 3, tags = Array("output"))
 val tStreamsSenderThreadMock = new TStreamsSenderThreadMock(Set(output.name))
 val manager = new ModuleEnvironmentManagerMock(stateStorage, options, Array(output), tStreamsSenderThreadMock)
 val executor: BatchStreamingExecutor[String] = new SomeExecutor(manager)
 val tstreamInput = new TStreamStreamDomain("t-stream-input", mock(classOf[TStreamServiceDomain]), 1)
 val kafkaInput = new KafkaStreamDomain("kafka-input", mock(classOf[KafkaServiceDomain]), 1, 1)
 val inputs = Array(tstreamInput, kafkaInput)

 val batchInstanceDomain = mock(classOf[BatchInstanceDomain])
 when(batchInstanceDomain.getInputsWithoutStreamMode).thenReturn(inputs.map(_.name))

 val batchCollector = new SomeBatchCollector(batchInstanceDomain, mock(classOf[BatchStreamingPerformanceMetrics]), inputs)

 val simulator = new BatchEngineSimulator(executor, manager, batchCollector)
 simulator.prepareState(Map("idleCalls" -> 0, "symbols" -> 0))
 simulator.prepareTstream(Seq("a", "b"), tstreamInput.name)
 simulator.prepareTstream(Seq("c", "de"), tstreamInput.name)
 simulator.prepareKafka(Seq("fgh", "g"), kafkaInput.name)
 simulator.prepareTstream(Seq("ijk", "lm"), tstreamInput.name)
 simulator.prepareTstream(Seq("n"), tstreamInput.name)
 simulator.prepareKafka(Seq("p", "r", "s"), kafkaInput.name)

 val batchesNumberBeforeIdle = 2
 val window = 4
 val slidingInterval = 2
 val results = simulator.process(batchesNumberBeforeIdle, window, slidingInterval)

 println(results)
 
``println(results)`` will print::
 
 BatchSimulationResult(SimulationResult(List(StreamData(out,List(PartitionData(0,List(17))))),Map(symbols -> 17, idleCalls -> 4)),ArrayBuffer(<last envelope>))
 
<last-envelope> is a `KafkaEnvelope[String]` that contains string "s".

The ``mock`` method is from the ``org.mockito.Mockito.mock`` library.

``SomeBatchCollector`` is an example of the ``BatchCollector`` implementation. The ``getBatchesToCollect`` method returns all nonempty batches, ``afterEnvelopeReceive`` counts envelopes in batches, ``prepareForNextCollecting`` resets counters. 

Accumulation of batches is implemented in ``BatchCollector``::

 class SomeBatchCollector(instance: BatchInstanceDomain,
                         performanceMetrics: BatchStreamingPerformanceMetrics,
                         inputs: Array[StreamDomain])
  extends BatchCollector(instance, performanceMetrics, inputs) {
  private val countOfEnvelopesPerStream = mutable.Map(instance.getInputsWithoutStreamMode.map(x => (x, 0)): _*)

  def getBatchesToCollect(): Seq[String] =
    countOfEnvelopesPerStream.filter(x => x._2 > 0).keys.toSeq

  def afterEnvelopeReceive(envelope: Envelope): Unit =
    countOfEnvelopesPerStream(envelope.stream) += 1

  def prepareForNextCollecting(streamName: String): Unit =
    countOfEnvelopesPerStream(streamName) = 0
 }

For more complicated examples, please, visit `sj-sflow-process-test <https://github.com/bwsw/sj-sflow-demo/blob/develop/sflow-process/src/test/scala/com/bwsw/sj/examples/sflow/module/process/ExecutorTests.scala>`_.

.. _Output_Engine_Simulator:

Output Engine Simulator
----------------------------

It is a class for testing the implementation of the `output module <http://streamjuggler.readthedocs.io/en/develop/SJ_Modules.html#output-module>`_ (Executor). 

Simulator imitates the behavior of the :ref:`Output_Streaming_Engine`: it sends transactions to the Executor, gets output envelopes from it and builds requests for loading data to an output service. Simulator uses :ref:`Output_Request_Builder` to build requests.

To use the simulator you need to add the dependency to the ``build.sbt``::
 
 resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots" 
 libraryDependencies += "com.bwsw" %% "sj-engine-simulators" % "1.0-SNAPSHOT" % "test"

Constructor arguments
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. csv-table:: 
 :header: "Argument", "Type", "Description"
 :widths: 25, 25, 50 

 "executor", "OutputStreamingExecutor[IT]", "Implementation of the `output module <http://streamjuggler.readthedocs.io/en/develop/SJ_Modules.html#output-module>`_ under testing"
 "outputRequestBuilder", "OutputRequestBuilder[OT] (see :ref:`Output_Request_Builder`)", "Builder of requests for output service"
 "manager", "OutputEnvironmentManager", "Instance of the OutputEnvironmentManager used by Executor"

.. important:: * IT - the type of data received by Executor
   * OT - the type of requests that ``outputRequestBuilder`` creates. The type depends on the type of output service (see "Request format" column of the table in the :ref:`Output_Request_Builder` section).

The simulator provides the following methods:

* ``prepare(entities: Seq[IT], stream: String = "default-input-stream", consumerName: String = "default-consumer-name"): Long`` - takes a collection of data (``entities`` argument), creates one transaction (TStreamEnvelope[IT] type) with the stream name that equals to the value of the ``stream`` parameter, saves them in a local buffer and returns an ID of the transaction. The ``consumerName`` argument has a default value ("default-consumer-name"). You should define it only if the executor uses ``consumerName`` from TStreamEnvelope.
* ``process(clearBuffer: Boolean = true): Seq[OT]`` - sends all transactions from local buffer to Executor by calling the ``onMessage`` method for each transaction, gets output envelopes and builds requests for output services. The ``clearBuffer`` argument indicates that local buffer with transactions have to be cleared after processing. That argument has a default value "true".
* ``clear()`` - clears local buffer that contains transactions.

Simulator has the ``beforeFirstCheckpoint`` flag that indicates that the first checkpoint operation has not been performed. Before the first checkpoint operation the Simulator builds a delete request for each incoming transaction (in the ``process`` method). ``beforeFirstCheckpoint`` can be changed automatically by calling ``manager.initiateCheckpoint()`` inside the Executor, or manually.

.. _Output_Request_Builder:

Output Request Builder
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It provides the following methods to build requests based on an output envelope for a specific output service:

* ``buildInsert`` - builds a request to insert data
* ``buildDelete`` - builds a request to delete data

The are three implementations of the ``OutputRequestBuilder`` for each type of output storage:

.. csv-table:: 
 :header: "Classname", "Request format", "Output storage type"
 :widths: 25, 25, 50 

 "EsRequestBuilder", "String", Elasticsearch"
 "JdbcRequestBuilder", "PreparedStatementMock", "SQL database"
 "RestRequestBuilder", "org.eclipse.jetty.client.api.Request", "RESTful service"

.. note:: Constructors of the ``EsRequestBuilder`` and the ``JdbcRequestBuilder`` takes the ``outputEntity`` argument. It should be created using the ``executor.getOutputEntity`` method.

Usage example
~~~~~~~~~~~~~~~~~~~~

E.g. you have your own Executor, that takes pairs (Integer, String) and puts them in Elasticsearch::

 class SomeExecutor(manager: OutputEnvironmentManager) 
  extends OutputStreamingExecutor[(Integer, String)](manager) {
  override def onMessage(envelope: TStreamEnvelope[(Integer, String)]): Seq[OutputEnvelope] = { ... }
  override def getOutputEntity: Entity[String] = { ... }
 }

If you want to see what Executor returns after processing and what requests are used to save processed data, Output Engine Simulator can be used in the following way::

 val manager: OutputEnvironmentManager
 val executor = new SomeExecutor(manager)

 val requestBuilder = new EsRequestBuilder(executor.getOutputEntity)
 val simulator = new OutputEngineSimulator(executor, requestBuilder, manager)
 simulator.prepare(Seq((1, "a"), (2, "b")))
 simulator.prepare(Seq((3, "c")))
 val requestsBeforeFirstCheckpoint = simulator.process()
 println(requestsBeforeFirstCheckpoint)

 // "perform" the first checkpoint
 simulator.beforeFirstCheckpoint = false
 simulator.prepare(Seq((4, "d"), (5, "e")))
 val requestsAfterFirstCheckpoint = simulator.process()
 println(requestsAfterFirstCheckpoint)


``requestsBeforeFirstCheckpoint`` will contain delete and insert requests, ``requestsAfterFirstCheckpoint``  will contain insertion requests only.

To see more complicated examples, please, examine the following sections: `sj-fping-output-test <https://github.com/bwsw/sj-fping-demo/blob/develop/ps-output/src/test/scala/com/bwsw/sj/examples/pingstation/module/output/ExecutorTests.scala>`_, `sj-sflow-output-test <https://github.com/bwsw/sj-sflow-demo/blob/develop/sflow-output/src-dst/src/test/scala/com/bwsw/sj/examples/sflow/module/output/srcdst/ExecutorTests.scala>`_

Objects For Simulators With States
-------------------------------------
Under this section the class of objects used for Simulators with states is described. These Simulators are :ref:`Regular_Engine_Simulator` and :ref:`Batch_Engine_Simulator`.

.. _Simulation-Result:

Simulation Result
~~~~~~~~~~~~~~~~~~~~

``case class SimulationResult(streamDataList: Seq[StreamData], state: Map[String, Any])`` - contains data elements for each output stream and the state at a certain point in time.

``case class StreamData(stream: String, partitionDataList: Seq[PartitionData])`` - contains data items that have been sent to an output stream.

``case class PartitionData(partition: Int, dataList: Seq[AnyRef])`` - contains data elements that have been sent to an output stream partition.

.. _Module-Environment-Manager-Mock:

Module Environment Manager Mock
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is a mock for ``StatefulModuleEnvironmentManager``. 

It creates :ref:`PartitionedOutputMock` and :ref:`RoundRobinOutputMock` to save the information on where the data are transferred.

Constructor arguments
"""""""""""""""""""""""""""""""""

.. csv-table:: 
 :header: "Argument", "Type", "Description"
 :widths: 15, 15, 30 

 "stateStorage", "StateStorage", "A storage of the state"
 "options", "String", "User-defined instance options"
 "outputs", "Array[TStreamStreamDomain]", "The list of output streams from an instance"
 "senderThread", "TStreamsSenderThreadMock", "The mock for thread for sending data to the T-Streams service (described below)"
 "fileStorage", 	"FileStorage", 	"A file storage (mocked by default)"

TStreamsSenderThread Mock
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Contains a collection of output elements (see :ref:`Simulation-Result`).

Constructor arguments:

.. csv-table:: 
 :header: "Argument", "Type", "Description"
 :widths: 15, 15, 30 
 
 "streams", "Set[String]", "storage of state"

Provided methods:

* ``def getStreamDataList: Seq[StreamData]`` - returns streams with data.
* ``def prepareToCheckpoint(): Unit`` - removes data from streams.


Module Output Mocks
~~~~~~~~~~~~~~~~~~~~~~
They puts data into TStreamsSenderThreadMock.

.. Module Output Mocks have a buffer that contains output elements (see Simulation-Result_).

.. Provided methods:

.. * ``getOutputElements: mutable.Buffer[OutputElement]`` - returns a buffer with output elements.
.. * ``clear()`` - removes all output elements from a buffer.

.. _PartitionedOutputMock:

Partitioned Output Mock
""""""""""""""""""""""""""""""""

The mock of an output stream that puts data into a specific partition.

Provided methods:

* ``put(data: AnyRef, partition: Int)`` - puts data into a specific partition.

.. _RoundRobinOutputMock:

Round Robin Output Mock
""""""""""""""""""""""""""""""

The mock of an output stream that puts data using the round-robin policy.

Provided methods:

* ``put(data: AnyRef)`` - puts data into a next partition.
