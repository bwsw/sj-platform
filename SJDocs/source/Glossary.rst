
:Aggregation: A function applied to the grouped values.

:Batch: A minimum time interval during which a handler collect events in a stream.

:Check-every policy: The policy when every matching rule is selected for a stream (one stream has many exits)

:Check point: Completeness check of the data received after the stream processing.

:Custom handler: A handler specified by a user.

:Database service: The database provided by an exact provider.

:Demux handler: A handler that allows to divide an input stream into several output streams.

:Dimensions: Values that uniquely determine an event in the handler output stream within a window.

:Event: Minimal data unit in a stream.

:Exactly once processing: The system processes events in a stream only once

:First-match-win policy: The policy when the first matching rule is selected for a stream (one stream has one exit)

:Grouping: Event grouping by variables in transfer data stream.

:Guard: Boolean expression, defined by a user.

:Handler: A programm module processing streams.

:Instance:  A full range of settings to perfom an exact executor type.
 
:Input: A stream that is fed to the system input.

:Life-cycle of the events in the stream: The time period for events in a stream.

:Locks service: The service aborting an operation till receiving necessary events. 

:Matcher: A range of parameters for typed stream creation.

:Mapping: An input stream projecting to a transfer stream. 

:Metric: A numeric value received as a result of aggregation.

:Mux handler: A handler that allows to integrate several input stream into one output streams.

:Output: A stream that passes through the handler and goes to the system output.

:Partition: A part of a data stream allocated for convenience in operation.

:Post-guard: The stream check after transformation, or an additional checking at matcher.

:Pre-guard: A checking of a typed stream before transformation.

:Provider: The sevice provider for input data transformation into a stream. 

:Shift: Windown sliding in a period of time (multiple of a batch). It can be less than a window size, and in this case the data will be duplicated.

:Storage state service: A service storing data state in a stream; is performed together with the checkpoint.

:Stream: A sequence of events happening randomly at irregular intervals.

:Streams service: A service to perform an input data into a stream of an exact type. 

:Transaction: A separate atomic operations with events.

:Transformation: Sequence of actions including displaying, grouping and aggregation.

:Transformer handler: A handler for typed stream transformations.

:Typed stream: A stream the avro schema for which is specified.

:UDF (user-defined function): A user-defined function for a typed stream transfromation. 

:Window: A time period multiple of a batch during which the event collecting takes place.
