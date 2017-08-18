Streaming
=============================

SJ Platform enables scalable, high-throughput, fault-tolerant stream processing of live data streams. 

Stream Conception in SJ-Platform 
-------------------------------------------

The Streaming component is essential in SJ-Platform. The data is fed to the system, transported between modules and exported to an external storage via streams. It is streaming that makes possible such platform features as exactly-once processing, parallelism, fault-tolerance, horizontal scalability.

There are two kinds of streams in SJ-Platform:

- An input stream - a stream which provides new events. There are two different input stream types in SJ-Platform: Kafka & T-Stream

- An output stream - a stream which is a destination for results. There is one output stream type in SJ-Platform: T-Stream


The data can be received from different sources. Currently, the platform supports obtaining data from Kafka and TCP sockets.

SJ-Platform supports Apache Kafka as a standard providing a common interface for integration for most applications.

Using TCP as an input source a custom protocol can be applied for receiving events, deduplicating them and putting into the processing pipeline. 

At the project `repository <https://github.com/bwsw/sj-platform/tree/develop>`_ two input modules are available for users - CSV input module and Regex input module - that trnasform data flow of csv/regex type to envelopes for T-streams.

Within the platform, the data is transported to and from modules via *transactional streams* or T-streams. It is a message broker and a Scala library native to SJ-Platform and designed primarily for exactly-once processing (so it includes a transactional producer, a consumer and a subscriber). More information on T-streams can be found at the `project site <http://t-streams.com/>`_.

The easiest way to try T-streams and dive into basic operation with t-streams is to download `T-streams-hello <http://t-streams.com/getting-started/>`_ . The demo shows the basic operation mode between producer and subscriber.

T-streams architecture is not complicated. T-streams consist of partitions. Each partition holds a number of transactions with data elements inside. 

.. figure:: _static/t-streams-organization.png

Data elements are time-sorted in a transaction. 

Consumers and Producers use transactions to write or read data from T-streams.  Transaction is also a basic recovery element. This means, that in a case of a crash, Consumers and Producers can recover from a transaction.

Consumer iterates over transactions from earliest to the latest and reads data from every transaction. Every Consumer works in a specific T-stream and specific partitions. Consumer implements polling approach of processing.  After a transaction (or transaction set) was handled properly, the consumer does checkpoint which means that even in a cause of a crash or for another reason that consumer will start processing the transaction which is the next to the processed one.

Producers open transactions in a strictly ordered mode. Consumers and Subscribers always process transactions in the same order they have been opened. Producer can checkpoint or cancel transactions in an arbitrary way, but Subscriber will start handling them once the first (the eraliest) one is checkpointed. 

For the strictly ordered way of transaction opening a master producer is responsible. A master is registered in Apache Zookeeper per each transaction. The master generates a new transaction, registers it in Zookeeper, and returns the transaction ID to a Producer. Producer fills the transaction with data from the storage server. Then, it sends checkpoint or cancel to the server commit log and the transaction is checkpointed or canceled. 

Finally, storage server commit logs are played and results are stored to RocksDB. 

Checkpoint Group
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**CheckpointGroup** is the special entity which allows a developer to do atomic checkpoint for a group of producers and consumers. CheckpointGroup is the core component for exactly-once data exchange.

Streaming Infrastructure
-----------------------------------

Streams need infrastructure: Providers and Services. This is a required presetting without which streaming will not be so flexible. Streaming flexibility lies in one-to-many connection between providers and services, services and streams. One provider works with many services (they can be of various types) as well as one service can provide several streams. These streams take necessary settings from the common infrastructure (providers and services). There is no need to duplicate the settings for each individual stream.

Provider is the service provider for data transformation into a stream.

Service is a service to transform data into a stream of an exact type.

They can be of different types. The types of platform entities in the pipeline determine the type of providers and services that are necessary in the particular case.

The diagram of platform entities interconnections can be useful in selecting the necessary types of providers and services.

.. figure:: _static/InstanceCorrelation1.png

Firstly, decide what type of modules will be included into the pipline.

That will help to clarify which streams are required for these particular modules.

Secondly, find in the diagram what services are necessary for these types of streams. 

Finally, when services are determined, it is easy to see what types of providers should be created. 

Start creating the infrastructure from providers, then proceed with services and then streams. Detailed instructions can be found in the `Tutorial <>`_ (for creating infrastructure via REST API) or in the `UI Guide <http://streamjuggler.readthedocs.io/en/develop/SJ_UI_Guide.html>`_ for creating through the Web UI.



