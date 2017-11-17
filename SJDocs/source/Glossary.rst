Glossary
================

.. glossary::
  :sorted:
  
Batch 
  A minimum time interval during which a handler collect events in a stream.

Configurations 
  Settings required for the module performance. 

Checkpoint
  Completeness check of the data received after the stream processing.
  
Checkpoint Group
  A special entity which allows a developer to do atomic checkpoint for a group of producers and consumers. 

Custom handler 
  A handler specified by a user.

Database service 
  The database provided by an exact provider.

Engine
  A base of the system. It provides basic I/O functionality. It uses module settings to process data.

Envelope
  A specialized fundamental data structure, containing data and metadata that allow exactly-once processing.

Event 
  Minimal data unit in a stream.

Exactly-once processing 
  The system processes events in a stream only once

Executor
  A part of a module that performs data processing.

Instance 
  A set of settings determining the work of engine-module collaboration.
 
Input 
  A stream that is fed to the system input.

Life-cycle of events in the stream 
  The time period for events in a stream.

Locks service 
  The service aborting an operation till receiving necessary events. 

Metric 
  A numeric value received as a result of aggregation.

Module 
  A program module processing streams.

Output 
  A stream that goes out of the system.

Partition 
  A part of a data stream allocated for convenience in operation.

Provider 
  The service provider for input data transformation into a stream. 

Service 
  A service to perform an input data into a stream of an exact type. 

Shift 
  Window sliding in a period of time (multiple of a batch). It can be less than a window size, and in this case the data will be duplicated.

Storage state service  
  A service storing data state in a stream; is performed together with the checkpoint.

Stream  
  A sequence of events happening randomly at irregular intervals.

Task
  The actual data processing.

Transaction 
  A separate atomic operations with events.

T-streams
  (transactional streams); a Scala library and infrastructure components which implement transactional messaging.

Typed stream 
  A stream the avro schema for which is specified.

Window 
  A time period multiple of a batch during which the event collecting takes place.
  




