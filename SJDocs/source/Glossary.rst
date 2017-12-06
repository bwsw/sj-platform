Glossary
================

.. glossary::
  :sorted:
  
Batch 
  A minimum data set to collect events of a stream.

Configurations 
  Settings used for the system work. 

Checkpoint
  A saved "snapshot" of processed data at a specific time. The system saves the information about all consumed/recorded data by a module at that particular time, and in case of failure it restores the module work to the stage it was before the last checkpoint. If no checkpoint has been performed, the processing will start from the beginning.
  
.. Checkpoint Group
  A special entity which allows a developer to do atomic checkpoint for a group of producers and consumers. 

Engine
  A base of the system. It provides basic I/O functionality for a module. It uses module settings to process data.

Envelope
  A specialized fundamental data structure, containing data and metadata. The metadata is required for exactly-once processing.

Event 
  Minimal data unit of a stream.

Exactly-once processing 
  The processing of stream events only once.

Executor
  A part of a module that performs data processing.

External data source 
   An external system which provides data for processing.

External data storage 
   An external system which stores resulting data after processing.

Instance 
  A set of settings determining the collaborative work of an engine and a module.
 
Metric 
  A numeric value received as a result of aggregation.

Module 
  A key program component in the system that contains the logic of data processing and settings validation.

Partition 
  A part of a data stream allocated for convenience in stream processing.

Physical service 
  One of the following services used for SJ-Platform: Apache Kafka, Apache Zookeeper, T-streams, Elasticsearch, SQL-database, any system wich provides RESTful interface.

Provider 
  An entity which contains general information to access a physical service. 

Service 
  An entity which contains specific information to access a physical service. 

Sliding interval 
  A step size at which a window slides forward. It can be less than a window size, and in this case some data will be processed more than once.

State
  A sort of a key-value storage for user data which is used to keep some global module variables related to processing. These variables are persisted and are recovered after a failure. 

State storage service  
  A service responsible for storing data state into a specified location (determined by instance settings). It is performed together with the checkpoint.

Stream  
  A sequence of events happening randomly at irregular intervals. Streams in SJ-Platform differ by a physical service (that provides or stores data) and by purpose. According to a physical service the types of streams existing in SJ-Platform are: Apache Kafka, T-streams, Elasticsearch, SQL-database, RESTful. According to stream purpose they are: input streams, internal streams and output streams.
  
  - **Input stream** - sequence of data elements from data source. May be of types: T-streams, Apache Kafka.
  - **Internal stream** - sequence of data elements transferred between system modules. May be of type: T-streams.
  - **Output stream** - sequence of data elements which is saved in data storage. May be of types: T-streams, Elasticsearch, SQL database, RESTful.
  
Task
  It is a Mesos term and, in general, it means a piece of work to be done. Within the system, a task is a logical part of an instance. Each task does the same work that is provided by a module. They are executed separately from each other.

Transaction 
  A part of a partition consisting of events.

T-streams
  (transactional streams); a Scala library providing an infrastructure component which implements transactional messaging.

Window 
  A set of batches on an unbounded stream. Grouping batches allows processing a series of events at one time. It is important in case the processing of events in a stream depends on other events of this stream or on events in another stream.  
  




