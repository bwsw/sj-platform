Custom Module Development Guide
======================================

.. warning:: *The section is under development!*

Under this section how to write your own module for the Stream Juggler will be described.

Stream Juggler is a platform for your custom module implementation. It allows to adjust the system to your custom aims. Creation of a custom module will not become a challenge for a practicing developer or a programmer as no special tools or services are necessary.

Processing modules in Stream Juggler can be of the following types:

1. Input module - It handles external inputs, does data deduplication, transforms raw data to objects.
2. Processing module:

- Regular-streaming - A generic module which receives an event, does some data transformation and sends transformation to the next processing step.
- Batch-streaming - It organizes incoming data into batches and processing is performed with sliding window. Batch module may be used to implement streaming joins and processing where algorithm must observe range of input messages rather than current one.

3. Output module - It handles the data outcoming from event processing pipeline to external data destinations (Elasticsearch, JDBC, etc.).

The workflow of the platform implies the structure:

1. As incoming information can be fed to a processing module in T-streams or Kafka, the input module is necessary at the first step of ingesting data to transform it from TCP into T-streams. If you want to process the data from Kafka, the input module is not required.
2. A processing module performs the main transformation and calculation of data. It accepts data via T-streams and Kafka. The processed data is put into T-streams only. So an output module is required in the next step.
3. An output module is necessary to transform the data from T-streams into the result data of the type appropriate for the external storage.

.. figure:: _static/ModulePipeline.png

Below you will find the instructions on custom module creation in Scala.

Before Starting With Modules
--------------------------------------------------
The instructions below are given for assembling a .jar file via sbt in Scala.

It is meant that all necessary services are deployed for the module and you know for sure:

- what type of module you want to create;
- what type of inputs/outputs are necessary for it;
- what engine will be used (it should exist in the database);
- what type of external storage you are going to use for result data.


Input Streaming Custom Module
---------------------------------
- Create an executor class inheriting InputStreamingExecutor class and override some of methods if necessary (:ref:`input-module`)
- Create a validator class inheriting StreamingValidator class and override the validate method if necessary (:ref:`validator`)

1) Create a new project depending on sj-engine-core library, i.e. use the latest version from https://mvnrepository.com/artifact/com.bwsw in your `build.sbt` file.
2) Create `specification.json` in a resources folder and fill it in as shown in the example (:ref:`Json_schema`).
3) Assembly a jar of your module by calling sbt instruction from project folder, e.g. 'sbt my-input-module/assembly'
4) Upload the module (via UI or REST)
5) Create an instance of the module (via UI or REST)
6) Launch InputTaskRunner.scala with the following environment variables:

- `INSTANCE_NAME`
- `TASK_NAME` - name of the task from 'tasks' field of instance
- `MONGO_HOSTS` - comma separated list of Mongo hosts
- `MONGO_USER` - Mongo readWrite role user for stream_juggler db
- `MONGO_PASSWORD` - Mongo readWrite role password for stream_juggler db user
- `AGENTS_HOST` - host for holding input TCP connection (you should use 'localhost')
- `ENTRY_PORT` - port for holding input TCP connection (you should use any numbers from 8000 to 9000)


Regular Streaming Custom Module
---------------------------------
- Create an executor class inheriting RegularStreamingExecutor class and override some of methods if necessary (:ref:`regular-module`)
- Create a validator class inheriting StreamingValidator class and override the validate method if necessary (:ref:`validator`)
1) Create a new project depending on sj-engine-core library, i.e. use the latest version from https://mvnrepository.com/artifact/com.bwsw in your build.sbt file.
2) Create `specification.json` in a resources folder and fill it in as shown in the example (:ref:`Json_schema`). 

3) Assembly a jar of your module by calling sbt instruction from project folder, e.g. 'sbt my-regular-module/assembly' 
4) Upload the module (via REST or UI)
5) Create an instance of the module (via REST or UI)
6) Launch `RegularTaskRunner.scala` with the following environment variables:

* `INSTANCE_NAME`
* `TASK_NAME` - name of the task from execution plan of instance
* `MONGO_HOSTS` - comma separated list of Mongo hosts
* `MONGO_USER` - mongo readWrite role user for stream_juggler db
* `MONGO_PASSWORD` - mongo readWrite role password for stream_juggler db user
* `AGENTS_HOST` - host for T-stream agents (you should use 'localhost')
* `AGENTS_PORTS`- set of ports for T-stream agents (you should use any numbers from 8000 to 9000 in accordance with the number of instance T-stream inputs)

Batch Streaming Custom Module
------------------------------------

- Create an executor class inheriting BatchStreamingExecutor class and override some of methods if necessary (:ref:`batch-module`)
- Create a batch collector inheriting BatchCollector class and override the required methods (:ref:`Batch-Collector)
- Create a validator class inheriting StreamingValidator class and override the validate method if necessary (:ref:`validator`)

1) Create a new project depending on sj-engine-core library, i.e. use the latest version from https://mvnrepository.com/artifact/com.bwsw in your build.sbt file.
2) Create `specification.json` in a resources folder and fill it in as shown in the example (:ref:`Json_schema`).
3) Assembly a jar of your module by calling sbt instruction from project folder, e.g. 'sbt my-batch-module/assembly' 
4) Upload the module (via REST or UI)
5) Create an instance of the module (via REST or UI)
6) Launch BatchTaskRunner.scala with the following environment variables:

* `INSTANCE_NAME`
* `TASK_NAME`- name of the task from execution plan of instance
* `MONGO_HOSTS` - comma separated list of hosts
* `MONGO_USER` - mongo readWrite role user for stream_juggler db
* `MONGO_PASSWORD` - mongo readWrite role password for stream_juggler db user
* `AGENTS_HOST` - host for t-stream agents (you should use 'localhost')
* `AGENTS_PORTS` - set of ports for t-stream agents (you should use any numbers from 8000 to 9000 in accordance with the number of instance t-stream inputs)

Output Streaming Custom Module
-----------------------------------------------
- Create an executor class inheriting OutputStreamingExecutor class and override some of methods if necessary (:ref:`output-module`)
- Create a validator class inheriting StreamingValidator class and override the validate method if necessary (:ref:`validator`)

1) Create a new project depending on sj-engine-core library, i.e. use the latest version from https://mvnrepository.com/artifact/com.bwsw in your build.sbt file.
2) Create `specification.json` in a resources folder and fill it in as shown in the example (:ref:`Json_schema`).

.. note: Stream types for output-streaming module:
* stream.t-stream (only for incoming streams)
* elasticsearch-output (output stream)
* jdbc-output (output stream)
* rest-output (output stream)

3) Create class of entity extended on OutputEnvelope. Override method "getFieldsValue".
4) Assembly a jar of your module by calling sbt instruction from project folder, e.g. 'sbt my-output-module/assembly' 
5) Create an index in Elasticsearch and the index mapping, or a table in a database, or deploy some REST service. Name of index is provided in Elasticsearch service. A table name and a document type is a stream name. A full URL to entities of the REST service is "http://<host>:<port><basePath>/<stream-name>"
6) Upload the module (via Rest API or UI)
7) Create an instance of the module  (via Rest API or UI)
8) Launch `InputTaskRunner.scala` with the following environment variables:
   
* `INSTANCE_NAME`
* `TASK_NAME`- name of task from execution plan of instance.
* `MONGO_HOSTS`- comma separated list of mongo hosts.
* `MONGO_USER` - mongo readWrite role user for stream_juggler db
* `MONGO_PASSWORD` - mongo readWrite role password for stream_juggler db user
* `AGENTS_HOST` - host for t-stream agents (you should use 'localhost')
* `AGENTS_PORTS` - set of ports for t-stream agents (you should use any numbers from 8000 to 9000 in accordance with the number of instance t-stream inputs)





Hello World Custom Module
------------------------------

.. warning:: The section is under development!

This tutorial explains how to write a module using a simple Hello World example.
