Custom Module Development Guide
======================================

.. warning:: *The section is under development!*

Under this section how to write your own module for the Stream Juggler will be described.

Stream Juggler is a platform for your custom module implementation. It allows to adjust the system to your custom aims. Creation of a custom module will not become a challenge for a practicing developer or a programmer as no special tools or services are necessary.

Prior to the module development, please, take a look at the platform :ref:`Architecture` and :ref:`Modules`.

As a simple refresher, processing modules in Stream Juggler can be of the following types:

1. Input module - It handles external inputs, does data deduplication, transforms raw data to objects.
2. Processing module:

- Regular-streaming - A generic module which receives an event, does some data transformation and sends it to the next processing step.
- Batch-streaming - It organizes incoming data into batches and processing is performed with sliding window. Batch module may be used to implement streaming joins and processing where algorithm must observe the range of input messages rather than a current one.

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
1) Create a new sbt project depending on sj-engine-core library, i.e. use the latest version from https://mvnrepository.com/artifact/com.bwsw in your `build.sbt` file.
2) Create an executor class inheriting InputStreamingExecutor class and override some methods if necessary (:ref:`input-module`)
3) Create a validator class inheriting StreamingValidator class and override the validate method if necessary (:ref:`validator`)
4) Create `specification.json` in a resources folder and fill it in as shown in the example (:ref:`Json_schema`).
5) Assemble a jar of your module by calling sbt instruction from the project folder, e.g. 'sbt my-input-module/assembly'
6) Upload the module (via UI or REST)
7) Create an instance of the module (via UI or REST)
8) Launch the instance.
.. note:: You can use a module simulator for preliminary testing of executor work (:ref:`Input_Engine_Simulator`).

Regular Streaming Custom Module
---------------------------------
1) Create a new sbt project depending on sj-engine-core library, i.e. use the latest version from https://mvnrepository.com/artifact/com.bwsw in your build.sbt file.
2) Create an executor class inheriting RegularStreamingExecutor class and override some methods if necessary (:ref:`regular-module`)
3) Create a validator class inheriting StreamingValidator class and override the validate method if necessary (:ref:`validator`)
4) Create `specification.json` in a resources folder and fill it in as shown in the example (:ref:`Json_schema`). 
5) Assemble a jar of your module by calling sbt instruction from project folder, e.g. 'sbt my-regular-module/assembly' 
6) Upload the module (via REST or UI)
7) Create an instance of the module (via REST or UI)
8) Launch the instance.
.. note:: You can use a module simulator for preliminary testing of executor work (:ref:`Regular_Engine_Simulator`).

Batch Streaming Custom Module
------------------------------------
1) Create a new sbt project depending on sj-engine-core library, i.e. use the latest version from https://mvnrepository.com/artifact/com.bwsw in your build.sbt file.
2) Create an executor class inheriting BatchStreamingExecutor class and override some methods if necessary (:ref:`batch-module`)
3) Create a batch collector inheriting BatchCollector class and override the required methods (:ref:`Batch-Collector`)
4) Create a validator class inheriting StreamingValidator class and override the validate method if necessary (:ref:`validator`)
5) Create `specification.json` in a resources folder and fill it in as shown in the example (:ref:`Json_schema`).
6) Assemble a jar of your module by calling sbt instruction from project folder, e.g. 'sbt my-batch-module/assembly' 
7) Upload the module (via REST or UI)
8) Create an instance of the module (via REST or UI)
9) Launch the instance.
.. note:: You can use a module simulator for preliminary testing of executor work (:ref:`Batch_Engine_Simulator`).

Output Streaming Custom Module
-----------------------------------------------
1) Create a new sbt project depending on sj-engine-core library, i.e. use the latest version from https://mvnrepository.com/artifact/com.bwsw in your build.sbt file.
2) Create an executor class inheriting OutputStreamingExecutor class and override some of methods if necessary (:ref:`output-module`)
3) Create a validator class inheriting StreamingValidator class and override the validate method if necessary (:ref:`validator`)
4) Create `specification.json` in a resources folder and fill it in as shown in the example (:ref:`Json_schema`).

.. note:: Stream types for output-streaming module:
* stream.t-stream (only for incoming streams)
* elasticsearch-output (output stream)
* jdbc-output (output stream)
* rest-output (output stream)

5) Create class of entity that extends OutputEnvelope. Override method "getFieldsValue".
6) Assemble a jar of your module by calling sbt instruction from the project folder, e.g. 'sbt my-output-module/assembly' 
7) Create an index in Elasticsearch and the index mapping, or a table in a database, or deploy some REST service. Name of index is provided in Elasticsearch service. A table name and a document type is a stream name. A full URL to entities of the REST service is "http://<host>:<port><basePath>/<stream-name>"
8) Upload the module (via Rest API or UI)
9) Create an instance of the module  (via Rest API or UI)
10) Launch the instance.
.. note:: You can use a module simulator for preliminary testing of executor work (:ref:`Output_Engine_Simulator`).

Hello World Custom Module
------------------------------

.. warning:: The section is under development!

This tutorial explains how to write a module using a simple Hello World example.