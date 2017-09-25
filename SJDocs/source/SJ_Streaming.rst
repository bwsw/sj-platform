.. _Streaming::

Streams in SJ-Platform
=============================

SJ-Platform enables scalable, high-throughput, fault-tolerant stream processing of live data streams. 

Stream Conception in SJ-Platform 
-------------------------------------------

The streaming component is essential in SJ-Platform. The data are fed to the system, transported between modules and exported to an external storage via streams.

There are two kinds of streams in SJ-Platform:

- An input stream - a stream which provides new events. The following input stream types are supported in SJ-Platform: TCP, Apache Kafka and T-Streams.

- An output stream - a stream which is a destination for results. Within SJ-Platform the results are written into T-Streams. To export the processed data from T-streams additional output streams are required. They are created for an output module and correspond to the type of the external storage. For now, Elasticsearch, SQL database and RESTful output stream types are supported.

Input Streams
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
The data can be received from different sources. Currently, the platform supports obtaining data from Apache Kafka and TCP sockets.

SJ-Platform supports `Apache Kafka <https://kafka.apache.org/documentation/>`_ as a standard providing a common interface for integration for most applications.

Using TCP as an input source a custom protocol can be applied for receiving events, deduplicating them and putting into the processing pipeline. 

But in case TCP is used as a source, there arises a need in an input module to transform the input data into a stream to bring them to processing. At the project `repository <https://github.com/bwsw/sj-platform/tree/develop>`_ two input modules are available for users - a CSV input module and a Regex input module - that transform data flow of CSV/regex type to message format acceptable for T-streams. 

Within the platform, the data are transported to and from modules via *transactional streams* or T-streams. It is a message broker and a Scala library native to SJ-Platform and designed primarily for exactly-once processing (so it includes a transactional producer, a consumer and a subscriber). More information on T-streams can be found at the `project site <http://t-streams.com/>`_. Some general information on T-streams you can find below.
About T-Streams
""""""""""""""""""""""""

The easiest way to try T-streams and dive into basic operations with T-streams is to download `T-streams-hello <http://t-streams.com/getting-started/>`_. The demo shows the basic operation mode between a producer and a subscriber.

The T-streams architecture is not complicated. T-streams consist of partitions. Each partition holds a number of transactions with data elements inside. 

.. figure:: _static/t-streams-organization.png

Data elements are time-sorted within a transaction. 

Consumers and Producers use transactions to write or read data from T-streams. The transaction is also a basic recovery element. This means, that in case of a crash, Consumers and Producers can recover from a transaction.

Consumer iterates over transactions from earliest to the latest and reads data from every transaction. Every Consumer works in a specific T-stream and specific partitions. Consumer implements polling approach of processing.  After a transaction (or transaction set) is handled properly, the consumer does checkpoint which means that even in case of a crash or for another reason that consumer will start processing the transaction which is the next to the processed one.

Producers open transactions in a strictly ordered mode. Consumers and Subscribers always process transactions in the same order they have been opened. Producer can checkpoint or cancel transactions in an arbitrary way, but Subscriber will start handling them once the first (the earliest) one is checkpointed. 

For the strictly ordered way of transaction opening a master producer is responsible. A master is registered per each transaction in Apache Zookeeper. The master generates a new transaction, registers it in Apache Zookeeper, and returns the transaction ID to a Producer. Producer fills the transaction with data from the storage server. Then, it sends checkpoint or cancels to the server commit log and the transaction is checkpointed or canceled. 

Finally, storage server commit logs are played, and the results are stored to RocksDB. 

Checkpoint Group
"""""""""""""""""""""

**CheckpointGroup** is a special entity which allows a developer to do atomic checkpoint for a group of producers and consumers. 

Several producers and consumers can be bunched up into a group, which can do a checkpoint atomically. This means  all producers and consumers in that group fix the current state. This is the key component of exactly-once data processing in SJ-Platform. 

Output streams
~~~~~~~~~~~~~~~~~~~~~~

Output streams are streams which are a destination for results.

The processed data are put into T-streams.

The data are retrieved from T-streams with the help of an output module. The output module puts the data from T-streams to the streams of the type which is suitable for the type of the external data storage.

The following types of output streams are supported in SJ-Platform:

- Elasticsearch, to store data to Elasticsearch;
- SQL database, to store data to JDBC-compatible databases;
- RESTful, to store data to RESTful storage.

.. _Streaming_Infrastructure:

Streaming Infrastructure
-----------------------------------

Streams need infrastructure: providers and services. This is a required presetting without which streaming will not be so flexible. Streaming flexibility lies in the one-to-many connection between providers and services, services and streams. One provider works with many services (they can be of various types) as well as one service can provide several streams. These streams take necessary settings from the common infrastructure (providers and services). There is no need to duplicate the settings for each individual stream.

A **provider** is a service provider for data transformation into a stream.

A **service** is a service to transform data into a stream of an exact type.

They can be of different types. The types of instances and streams in the pipeline determine the type of providers and services that are necessary in the particular case.

The diagram of platform entities interconnections can be useful in selecting the necessary types of providers and services.

.. figure:: _static/InstanceCorrelation1.png

Firstly, decide what types of instances will perform data transformation and processing in the pipeline. 

Determined instance types will help to clarify which streams are required for these particular instances.

Secondly, find in the diagram what services are necessary for these types of streams. 

Finally, when services are determined, it is easy to see what types of providers should be created. 

The table below explains what types of streams may serve as input or output streams for particular instances:

===============  ================================================  ===============================================
Instance          Input stream                                     Output stream
===============  ================================================  ===============================================
*Input*            TCP                                               T-streams 

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

Start creating the infrastructure from providers, then proceed with services and then streams. 

Detailed instructions on stream creation can be found in the :ref:`Tutorial` (for creating infrastructure via REST API) or in the `UI Guide <http://streamjuggler.readthedocs.io/en/develop/SJ_UI_Guide.html>`_ for creating through the Web UI.



