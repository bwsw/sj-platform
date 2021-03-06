.. _Modules:

Modules: Types, Structure, Pipeline
===================================

.. contents:: Contents
   
A **module** is a processor that handles events of data streams. It is a key program component in the system that contains the logic of data processing and settings validation. It works within an engine that receives raw data and sends them to the module executor. A module uses instances as a range of settings determining the collaborative work of an engine and a module. 

A module includes:

- an executor that processes data streams,
- a validator to check an instance.

.. figure:: _static/moduleExecutorAndValidator.png
   :scale: 120%
   :align: center

   Figure 1.1: Module structure
   
Below you will find more information on each of these two components.

.. _validator:

Streaming Validator
-------------------------

Streaming validator provides two methods:

1. to validate ``options`` parameter of the module.
2. to validate an instance of the module. The method is used when you try to create a new instance of a specific module, and if the validation method returns false value the instance will not be created.

Each of these two methods returns a tuple of values that contains:

- The value that indicates if the validation is sucсessful or not;

- The value that is a list of errors in case of the validation failures (it is an empty list by default). 

Executor
---------------------

An executor is a key module component that performs the data processing. It receives data and processes them in correspondence with the requirements of module specification. It utilizes an instance/instances for processing. An instance is a full range of settings for an exact module. It determins the coordinated work of an engine and a module.

Data Processing Flow in Modules
---------------------------------
In general, data processing in modules can be described in a simple scheme.

The base of the system is an engine: it provides basic I/O functionality. It is started via a Mesos framework which provides distributed task dispatching and then the information on task execution. The engine performs data processing using an uploaded module. 

.. figure:: _static/engine.png
   :scale: 120%
   :align: center
   
   Figure 1.2: An engine in SJ-Platform
   
After its uploading, the engine receives raw data and sends them to the module executor. The executor starts data processing and returns the resulting data back to the engine where they are deserialized to be passed into the stream or a storage.

Module Types
--------------

The system supports 4 types of modules:

1. *Input-streaming* - handles external inputs, does data deduplication, transforms raw data to objects. 

2. *Regular-streaming* (base type) - the most generic module which receives events, transforms data element by element and sends them to the next processing step.

3. *Batch-streaming* - a module where the processing algorithm must observe a range of input messages rather than the current one (as it is in the regular-streaming type). For each stream input messages are collected into batches. Then batches are collected in a window. Windows of several streams are transferred to the module for processing. Thus, the module allows processing of data from several streams at the same time. In SJ-Platform the data are processed using sliding window.

4. *Output* - handles the data outcoming from event processing pipeline to external data destinations (Elasticsearch, SQL database, RESTful).

The modules can be strung in a pipeline as illustrated below:

.. figure:: _static/ModuleStructure5.png
   :scale: 80 %
   :align: center

   Figure 1.3: Pipeline structure 
  
At this page each module is described in detail. You will find more information on the methods provided by module executors as well as entities' description.

.. _input-module:

Modules of Input Type
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Modules of the input-streaming type handle external input streams, does data deduplication, transforms raw data to objects. 

In the SJ-Platform the TCP Input module is currently implemented.

.. figure:: _static/InputModuleInP.png
  :scale: 80 %
  :align: center

  Figure 1.4: Input module direct interaction with pipeline components
  
It performs the transformation of the streams incoming via TCP into T-streams. T-streams are persistent streams designed for exactly-once processing (so they include a transactional producer, a consumer and a subscriber). Find more information about T-streams `here <http://t-streams.com>`_.

In the diagram below you can see the illustration of dataflow for the input module.

.. figure:: _static/InputModuleDataflow1.png
   :scale: 80 %
   :align: center

   Figure 1.5: Processing in the input module

All input data elements are going as a flow of bytes to particular interface provided by Task Engine. That flow is going straight to Streaming Executor and is converted to an object called an Input Envelope. 

An **envelope** is a specialized fundamental data structure containing data and metadata. The metadata is required for exactly-once processing.

The Input Envelope then goes to Task Engine which serializes it to a stream of bytes and then sends to T-Streams. 

An input module executor provides the following methods with default implementation (which can be overridden)f.

1) ``tokenize``: 
      It is invoked every time when a new portion of data is received. It processes a flow of bytes to determine the beginning and the end of the Interval (significant set of bytes in incoming flow of bytes). By default it returns None value (meaning that it is impossible to determine an Interval). If Interval detected, method should return it (indexes of the first and the last elements of the interval in the flow of bytes). The resulting interval can either contain message or not.

2) ``parse``: 
     This method is invoked once the "tokenize" method returns an Interval. It processes both a buffer with incoming data (a flow of bytes) and an Interval (an output of "tokenize" method). Its purpose is to define whether the Interval contains a message or meaningless data. Default return value is None. The same value should be returned if Interval contains meaningless data. If Interval contains a message, the "InputEnvelope" value should be returned.

3) ``createProcessedMessageResponse``:
      It is invoked after each call of the "parse" method. Its purpose is to create response to the source of data - the instance of InputStreamingResponse.

      The parameters of the method are:

	- ``InputEnvelope`` (it can be defined or not)

	- ``isNotEmptyOrDuplicate`` - a boolean flag (denoting whether an "InputEnvelope" is defined and isn't a duplicate (true) or an ``InputEnvelope`` is a duplicate or empty (false))

	Default implementation of the method::

  	 def createProcessedMessageResponse(envelope: Option[InputEnvelope], isNotEmptyOrDuplicate: Boolean): InputStreamingResponse = {
    	  var message = ""
    	  var sendResponsesNow = true
    	  if (isNotEmptyOrDuplicate) {
      	    message = s"Input envelope with key: '${envelope.get.key}' has been sent\n"
      	    sendResponsesNow = false
    	  } else if (envelope.isDefined) {
      	    message = s"Input envelope with key: '${envelope.get.key}' is duplicate\n"
    	  } else {
     	    message = s"Input envelope is empty\n"
    	  }
  	  InputStreamingResponse(message, sendResponsesNow)
 	 }

4) ``createCheckpointResponse``: 
      It is invoked on checkpoint's finish. It's purpose is to create response for data source to inform that checkpoint has been done. It returns an instance of ``InputStreamingResponse``.

      Default implementation of the method::

       def createCheckpointResponse(): InputStreamingResponse = {
   	 InputStreamingResponse(s"Checkpoint has been done\n", isBuffered = false)
       }


There is a manager inside the module which allows to:

 - retrieve a list of output stream names by a set of tags (by calling ``getStreamsByTags()``) 

 - initiate checkpoint at any time (by calling ``initiateCheckpoint()``) which would be performed only at the end of processing step (check diagram at the :ref:`Input_Streaming_Engine` page)

**Entities description**

``InputEnvelope``: 

- key of an envelope 
- information about the destination 
- "check on duplication" boolean flag (it has higher priority than ``duplicateCheck`` in ``InputInstance``)
- message data 

``InputStreamingResponse``: 

- ``message`` - string message
 
- ``sendResponsesNow`` - a boolean flag denoting whether response should be saved in temporary storage or all responses from this storage should be send to the source right now (including this one)
 
To see a flow chart on how these methods intercommunicate, please, visit the :ref:`Input_Streaming_Engine` page.

**Input Modules Provided By SJ-Platform**

The Stream Juggler Platform offers two examples of Input Module implementation. These are ready-to-use input modules for two most general input data formats: CSV and Regex. Find a detailed description of these modules at the :ref:`Provided_Input_Modules` section.

.. _regular-module:

Modules of Regular Type
~~~~~~~~~~~~~~~~~~~~~~~~~~
The most generic modules in the system are modules of a regular-streaming type. A simplified definition of a regular module is a handler that performs data transformation and put the processed data into T-streams.

.. figure:: _static/RegularModuleInP.png
  :scale: 80 %
  :align: center

  Figure 1.6: Regular module direct interaction with pipeline components

The diagram below represents the dataflow in the regular module.

.. figure:: _static/RegularModuleDataflow2.png
  :scale: 80 %
  :align: center

  Figure 1.7: Processing in the regular module
  
The TaskEngine of a regular module receives data from T-streams. It deserializes the flow of bytes to TStreamsEnvelope[T] (where [T] is a type of messages in the envelope) which is then passed to the StreamingExecutor.

The StreamingExecutor processes the received data and sends them to the TaskEngine as a result data.

The TaskEngine serializes all the received data to the flow of bytes and puts it back to T-Streams to send further.

In the Regular module the executor provides the following methods that does not perform any work by default so you should define their implementation by yourself.

1) ``onInit``: 
        It is invoked only once, when a module is launched. This method can be used to initialize some auxiliary variables, or check the state variables on existence and create them if necessary . Thus, you should do preparation of the executor before usage.

	Example of the checking a state variable::

 	 if (!state.isExist(<variable_name>)) state.set(<variable_name>, <variable_value>)

	``<variable_name>`` must have the String type

	``<variable_value>`` can be any type (a user must be careful when casting a state variable value to a particular data type)

2) ``onMessage``: 
    It is invoked for every received message from one of the inputs that are defined within the instance. There are two possible data types of input sources - that's why there are two methods with appropriate signatures::
    
	def onMessage(envelope: TStreamEnvelope[T]): Unit

	def onMessage(envelope: KafkaEnvelope[T]): Unit
 
    Each envelope has a type parameter that defines the type of data in the envelope.

.. note:: The data type of the envelope can be only "KafkaEnvelope" data type or "TStreamEnvelope" data type. A user may specify one of them or both, depending on which type(s) is(are) used. 

3) ``onBeforeCheckpoint``: 
    It is invoked before every checkpoint.
.. 4) "onAfterCheckpoint": 
    It is invoked after every checkpoint.
4) ``onTimer``: 
    It is invoked every time when a set timer expires. Inside the method there is access to a parameter that defines a delay between a real response time and an invocation of this handler.
5) ``onIdle``: 
    It is invoked every time when idle timeout expires but a new message hadn't appeared. It is a moment when there is nothing to process.
6) ``onBeforeStateSave``: 
    It is invoked prior to every saving of the state. Inside the method there is a flag denoting whether the full state (true) or partial changes of state (false) will be saved.
.. 8) "onAfterStateSave": 
    It is invoked after every saving of the state. Inside the method there is a flag denoting the full state (true) or partial changes of state (false) have(s) been saved

The module may have a state. A state is a sort of a key-value storage and can be used to keep some global module variables related to processing. These variables are persisted and are recovered after a fail. 

In case of a fail (when something is going wrong in one of the methods described above) a whole module will be restarted. And the work will start with the `onInit` method call.

Inside the module there is a manager allowing to get access to: 

- an output that is defined within the instance (by calling ``getPartitionedOutput()`` or ``getRoundRobinOutput()``),
- timer (by calling ``setTimer()``)
- state (by calling ``getState()``) if it is a stateful module
- list of output names (by calling ``getStreamsByTags()``). Every output contains its own set of tags which are used to retrieve it. 
- initiation of checkpoint (by calling ``initiateCheckpoint()``).

To see a flow chart on how these methods intercommunicate see the :ref:`Regular_Streaming_Engine` section.

.. _batch-module:

Modules of Batch Type
~~~~~~~~~~~~~~~~~~~~~~~~
Modules of a batch-streaming type process events collecting them in batches. A batch is a minimum data set for a handler to collect the events in the stream. The size of a batch is defined by a user. It can be a period of time or a quantity of events or a specific type of the event after receiving which the batch is considered closed.  Then, the queue of batches is sent further in the flow for the next stage of processing. 

.. _Batch-Collector:

Batch Collector
""""""""""""""""""
In the module a Batch Collector is responsible for the logic of collecting batches. It provides the following methods, implementation of which you should specify. 

1) ``getBatchesToCollect``:
       It should return a list of stream names that are ready to be collected.

2) ``afterEnvelopeReceive``:
       It is invoked when a new envelope is received.

3) ``prepareForNextCollecting``:
     It is invoked when a batch is collected. If several batches are collected at the same time then the method is invoked for each batch.

Let us consider an example:

This is a batch collector defining that a batch consists of a certain number of envelopes::

  class NumericalBatchCollector(instance: BatchInstanceDomain,
                              performanceMetrics: BatchStreamingPerformanceMetrics,
                              streamRepository: Repository[StreamDomain])
  extends BatchCollector(instance, performanceMetrics, streamRepository) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val countOfEnvelopesPerStream = mutable.Map(instance.getInputsWithoutStreamMode.map(x => (x, 0)): _*)           (1)
  private val everyNthCount = 2                                                                                           (2)

  def getBatchesToCollect(): Seq[String] = {
    countOfEnvelopesPerStream.filter(x => x._2 == everyNthCount).keys.toSeq                                               (3)
  }

  def afterEnvelopeReceive(envelope: Envelope): Unit = {
    increaseCounter(envelope)                                                                                             (4)
  }

  private def increaseCounter(envelope: Envelope) = {
    countOfEnvelopesPerStream(envelope.stream) += 1
    logger.debug(s"Increase count of envelopes of stream: ${envelope.stream} to: ${countOfEnvelopesPerStream(envelope.stream)}.")
  }

  def prepareForNextCollecting(streamName: String): Unit = {
    resetCounter(streamName)                                                                                              (5)
  }

  private def resetCounter(streamName: String) = {
    logger.debug(s"Reset a counter of envelopes to 0.")
    countOfEnvelopesPerStream(streamName) = 0
  }
 }

Let's take a look at the main points:

.(1) - create a storage of incoming envelopes for each input stream. 

.(2) - set a size of batch (in envelopes).

.(3) - check that batches contain the necessary number of envelopes.

.(4) - when a new envelope is received then increase the number of envelopes for specific batch.

.(5) - when a batch has been collected then reset the number of envelopes for this batch. 

The module allows transformation of the data aggregated from input streams applying the sliding window. 

A window size is equal to a number of batches. The window closes once it is full, i.e. the set number of batches is collected. The collected batches are transferred further for processing and the window slides forward for the set interval. This is the sliding window technique.

The diagram below is a simple illustration of how a sliding window operation looks like.

.. figure:: _static/BatchModule3.png
   :scale: 80 %
   :align: center

   Figure 1.8: Sliding windowing
  
As shown in the figure, every time the window slides over an input stream, the batches of events that fall within the window are combined and operated upon to produce the transformed data of the windowed stream. It is important that any window operation needs to specify the parameters:

- *batch size* — The quantity of events within a batch, or a period of time during which the events are collected in one batch.

- *window size* - The duration of the window, i.e. how many batches should be collected before sliding. 

- *sliding interval* - A step size at which the window slides forward.

In the example, the operation is applied over the last 3 events, and slides by 2 events. Thus, the window size is 3 and the sliding interval is 2.

In general, a window consists of batches, a batch consists of events (messages) that may contain data of different type depending on a data type of input. So, each event should have a type parameter that defines the type of data containing in the event unit.

The executor of the batch module provides the following methods that does not perform any work by default. So you should define their implementation by yourself.

1) ``onInit``: 
    It is invoked only once, when a module is launched. This method can be used to initialize some auxiliary variables or check the state variables on existence and if it's necessary to create them. Thus, you should do preparation of the executor before usage.

    Example of the checking a state variable::
 
     if (!state.isExist(<variable_name>)) state.set(<variable_name>, <variable_value>)
  
   ``<variable_name>`` should be of the String type

   ``<variable_value>`` can be of any type (be careful when you will cast a state variable value to a particular data type)

2) ``onWindow``: 
    It is invoked when a window for each input stream is collected (a list of input streams are defined within the instance). These collected windows are accessible via a window repository within the method. A window consists of batches, a batch consists of envelopes (messages). There are two possible data types of envelopes - that's why you should cast the envelope inside the method. Each envelope has a type parameter that defines the type of message data.

    Example of a message casting to a particular data type::

     val allWindows = windowRepository.getAll()
     allWindows.flatMap(x => x._2.batches).flatMap(x => 
     x.envelopes).foreach {
     case kafkaEnvelope: KafkaEnvelope[Integer @unchecked] => //here there is access to certain fields such as offset and data of integer type
     case tstreamEnvelope: TStreamEnvelope[Integer @unchecked] => //here there is access to certain fields such as txnUUID, consumerName and data (array of integers)
     }

    The data type of the envelope can be "KafkaEnvelope" data type or "TStreamEnvelope" data type. If you specify the inputs of the only one of this data types in an instance, you shouldn't match the envelope like in the example above and cast right the envelope to a particular data type::
          
     val tstreamEnvelope =            
     envelope.asInstanceOf[TStreamEnvelope[Integer]]

3) ``onBeforeCheckpoint``: 
    It is invoked before every checkpoint
.. 4) "onAfterCheckpoint": 
    It is invoked after every checkpoint
4) ``onTimer``: 
    It is invoked every time when a set timer expires. Inside the method there is access to a parameter that defines a delay between a real response time and an invocation of this handler
5) ``onIdle``: 
    It is invoked every time when idle timeout expires but a new message hasn't appeared. It is a moment when there is nothing to process
6) ``onBeforeStateSave``: 
    It is invoked before every saving of the state. Inside the method there is a flag denoting whether the full state (true) or partial changes of state (false) will be saved
.. 8) "onAfterStateSave": 
    It is invoked after every saving of the state. Inside the method there is a flag denoting the full state (true) or partial changes of state (false) have(s) been saved

When running a module in a parallel mode (the instance "parallelism" parameter is greater than 1), you may need to exchange data between tasks at the exact moment. You should use shared memory for it, e.g. Hazelcast or any other. In this case, the following handlers are used for synchronizing the tasks' work: 
 
1) ``onEnter``: The system awaits every task to finish the ``onWindow`` method and then the ``onEnter`` method of all tasks is invoked.

2) ``onLeaderEnter``: The system awaits every task to finish the ``onEnter`` method and then the ``onLeaderEnter`` method of a leader task is invoked.

.. 3) "onLeave": It is invoked by every task and waits for a leader-task stop processing

.. 4) "onLeaderLeave": It is invoked by a leader-task after passing an output barrier

To see a flow chart about how these methods intercommunicate see the :ref:`Batch_Streaming_Engine` section.

The Batch module can either have a state or not. A state is a sort of a key-value storage and can be used to keep some global module variables related to processing. These variables are persisted and are recovered after a fail. 

A fail means that something is going wrong in one of the methods described above. In this case a whole module will be restarted. And the work will start with the onInit method call.

Saving of the state is performed alongside with the checkpoint. At a checkpoint the data received after processing is checked for completeness. The checkpoint is an event that provides an exactly-once processing. 

There is a manager inside the module which grants access to:

- output that was defined within the instance (by calling ``getPartitionedOutput()`` or ``getRoundRobinOutput()``),
- timer (by ``calling setTimer()``)
- state (by calling ``getState()``) (only if it is a module with state)
- list of output names (by calling ``getStreamsByTags()``). Every output contains its own set of tags which are used to retrieve it.
- initiation of checkpoint (by calling ``initiateCheckpoint()``)

.. _output-module:

Modules of Output Type
~~~~~~~~~~~~~~~~~~~~~~~~

Modules of an output type are responsible for saving of output data to external data destinations (Elasticsearch, SQL database, RESTful).

.. figure:: _static/OutputModuleInP.png
  :scale: 80 %
  :align: center

  Figure 1.9: Output module direct interaction with pipline components
  
They transform the result of data processing received from T-streams and passe them to an external data storage. They allow to transform one data item from incoming streaming into one and more data output items.

The diagram below illustrates the dataflow in an output module.

.. figure:: _static/OutputModuleDataflow1.png
  :scale: 80 %
  :align: center

  Figure 1.10: Processing in the output module
  
The TaskEngine deserializes the stream of bytes from T-Streams to TStreamsEnvelope[T] (where [T] is a type of messages in the envelope) and sends it to the StreamingExecutor. The StreamingExecutor returns Entities back to the TaskEngine. 

They are then put to an external datastorage.

The output executor provides the following methods that does not perform any work by default so you should define their implementation by yourself.

1. ``onMessage``: 
    It is invoked for every received message from one of the inputs that are defined within the instance. Inside the method you have access to the message that has the TStreamEnvelope type. 

2. ``getOutputEntity``:
    It is invoked once when module running. This method returns the current working entity, i.e. fields and types. This method must be overridden. 

We should assign a type to an output envelope that corresponds to the type of an external storage (Elasticsearch, SQL database, RESTful).

To see a flow chart on how these methods intercommunicate, please, visit the :ref:`Output_Streaming_Engine` section.

A detailed manual on how to write a module you may find at the :ref:`hello-world-module` page.

Modules` performance is determined by the work of an engine. Engines of different types (Input, Regular/Batch, Output) have different structure, components and the workflow corresponding to the type of a module. 

Please, find more information about engines at the :ref:`Engines` page.

.. _Entities_Correlation:

Module Instances
-----------------------

Each type of modules described above requires an instance of a corresponding type. An instance is a set of settings determining the collaborative work of an engine and a module. These settings are specified via the UI or REST API and determine the mode of the module operation: data stream type the module is going to work with, checkpoint concept, settings of state and parallelism, etc. In the system instances can be of the following types: input, processing (regular/batch), output. 

Module's instances require the following elements to be created for their work:

- a stream

- a service

- a provider

You should create these elements before creating an instance. You need streams for instance inputs and outputs. Streams, in their turn, require specific services to be created. Each service (based on its type) requires a provider of a corresponding type. All these elements form the infrastructure for an instance.

Each instance type works with a specific type of streams, services and providers. Find below the detailed information on the types of providers, services and streams required for each instance type. Besides, we will provide you an example to explain the dependence of entity types on an instance type.

**Streams**

The Stream Juggler Platform supports *Apache Kafka* and *T-stream* types of streams. And while the Apache Kafka streams are a well-known type of streaming introduced by Apache Software Foundation, the T-streams is intentionally designed for the Stream Juggler Platform as a complement for Apache Kafka. The T-streams has more features than Kafka and makes exactly-once processing possible. Find more about T-streams at the `site <http://t-streams.com>`_ .

The following stream types can be used for output streams that export resulting data from the system to an external storage: Elasticsearch, SQL-database, a system with RESTful interface. They are determined by the type of the external data storage.

**Services and Providers**

To create streams of exact type in the system you need to create a service and a provider for this service. The types of a service and a provider are determined by the type of a stream you need for the module.

Example of Instance Infrastructure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In this section we will describe the process of determining of all the needed entities for the instance infrastructure.

For example, there is some issue for which you need to process data from Apache Kafka in a micro-batch mode. So we need to include a Batch module into our pipeline.

For the Batch module we need to create a batch instance. In the system an instance of any type requires Apache Zookeper service and Apache Zookeeper provider for it (Figure 1.11). The Apache Zookeeper service should be unique for all the instances in the system.

.. figure:: _static/zk-inst-serv-pr1.png
   :align: center
   
   Figure 1.11: Instance dependence on Apache Zookeeper 
   
The batch instance will receive data from Apache Kafka streams. Apache Kafka streams require the Apache Kafka service to exist in our system. The Apache Kafka service requires two specific providers of the following types: Apache Kafka and Apache Zookeeper (the same as in the previous step) (Figure 1.12).

.. figure:: _static/kfk-inst-serv-pr1.png
   :align: center
   
   Figure 1.12: Apache Kafka streaming infrastructure for the batch instance
   
So these are the instance and the streaming components types that we need for our example:

.. figure:: _static/inst-stream-serv-prov.png
   :align: center
   
   Figure 1.13: Batch instance infrastructure example

At this point we have determined the type of instance in the pipeline and the types of streaming components. So we can start building the infrastructure.

Firstly, create two providers - Apache Kafka and Apache Zookeeper. Secondly, create Apache Kafka service and Apache Zookeeper service (that will be unique for all instances in the system). Thirdly, create the stream of Apache Kafka. Finally, create the instance of a batch module.

General Instance Infrastructure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The schemas below may help you to understand the dependency of entities in the system for each module instance type.

Input module instance type works with the following entities types:

.. figure:: _static/InstCorrelation-Input.png
  :align: center

  Figure 1.14: Instance infrastructure for the input instance type
  
  |oneof-arrow| points to the entity type required for creation of this entity.
  
Processing module instance type (regular or batch) works with the following entities types:

.. figure:: _static/InstCorrelation-Process.png
  :align: center

  Figure 1.15: Instance infrastructure for the processing instance type
  
  |oneof-arrow| points to the entity type required for creation of this entity.
  
  |req-arrow| points to the entity which may be needed when creating a dependent entity.


Output module instance type works with the following entities types:

.. figure:: _static/InstCorrelation-Output2.png
  :align: center

  Figure 1.16: Instance infrastructure for the output instance type
  
  |oneof-arrow| points to the entity type required for creation of this entity.
  
  |oneofset-arrow| points to the set of entities, one of which shall be created before creating the dependent entity.
  
The table below explains what inputs and outputs can be used for a particular instance type:

===============  ================================================  ===============================================
Instance type    Inputs                                            Outputs
===============  ================================================  ===============================================
*Input*           TCP (provided by Input Streaming Engine)          T-streams 

                                                                      **Providers**: Apache Zookeeper
                                       
                                                                      **Services**: T-streams, Apache Zookeeper

*Regular/Batch*    T-streams                                         T-streams
               
                    **Providers**: Apache Zookeeper                   **Providers**: Apache Zookeeper

                    **Services**: T-streams, Apache Zookeeper         **Services**: T-streams, Apache Zookeeper
               
                   Apache Kafka
              
                    **Providers**: Apache Zookeeper, Apache Kafka
 
                    **Services**: Apache Zookeeper, Apache Kafka

*Output*           T-streams                                         Elasticsearch

                    **Providers**: Apache Zookeeper                     **Providers**: Elasticsearch
                 
                    **Services**: T-streams, Apache Zookeeper           **Services**:  Elasticsearch, Apache Zookeeper

                                                                     SQL database

                                                                       **Providers**:  SQL database

                                                                       **Services**: SQL database, Apache Zookeeper 
                                                                   
                                                                     RESTful
                                                                   
                                                                       **Providers**: RESTful

                                                                       **Services**: RESTful,  Apache Zookeeper 
===============  ================================================  ===============================================

We hope this information will help you to select the most appropriate types of entities in the system to build a pipeline for smooth data stream processing.

You can find the details on creating instances and their infrastructure in the :ref:`UI_Guide`.


.. |req-arrow| image:: _static/req-arrow.png

.. |oneof-arrow| image:: _static/oneof-arrow.png

.. |oneofset-arrow| image:: _static/oneofset-arrow.png
