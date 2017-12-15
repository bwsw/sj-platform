.. _Json_schema:

.. contents::

JSON schema for module specification 
===========================================

A JSON schema for module specification should contain the following *specification fields*. These fields are described in the table below :

.. tip:: IOstream - is an input/output stream.

.. csv-table:: **Specification fields**
   :header: "Field", "Format", "Description"
   :widths: 25, 20, 40

   "name*", "String", "The unique name of the module"
   "description", "String", "The description for a module"
   "version*", "String", "The module version"
   "author", "String", "The module author"
   "license", "String", "The software license type for a module"
   "inputs*", "IOstream","The specification for the inputs of a module"
   "outputs*", "IOstream", "The specification for the outputs of a module"
   "module-type*", "String", "The type of a module. One of the following: [input-streaming, output-streaming, batch-streaming, regular-streaming]"
   "engine-name*", "String", "The name of the computing core of a module"
   "engine-version*", "String", "The version of the computing core of a module"
   "validator-class*", "String", "The absolute path to class that is responsible for a validation of launch options"
   "executor-class*", "String", "The absolute path to class that is responsible for a running of module"
   "batch-collector-class**", "String", "The absolute path to class that is responsible for a batch collecting of batch-streaming module"

.. note:: `*` - a required field, ** - a required field for a batch-streaming module.

IOstream for inputs and outputs has the following structure:

.. csv-table:: **IOstream fields**
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60

  "cardinality*", "Array[Int]", "The boundary of interval in which a number of inputs can change. Must contain 2 items."
  "types*", "Array[String]", "The enumeration of types of inputs. Can contain only [stream.t-streams, stream.apache-kafka, stream.elasticsearch, stream.sql-database, stream.restful, input]."

.. note:: `*` - a required field.

.. A Json schema for a specification file of a module looks as presented below::

 {
  "$schema": "http://json-schema.org/draft-04/schema#",
  "title": "Run Module Specification",
  "description": "Common specification for stream-juggler modules that can be run",
  "definitions": {
    "source": {
      "type": "object",
      "properties": {
        "cardinality": {
          "description": "The boundary of interval in that a number of inputs can change",
          "type": "array",
          "items": {
            "type": "number",
            "minimum": 0
          },
          "minItems": 2,
          "maxItems": 2
        },
        "types": {
          "description": "The enumeration of types of inputs",
          "type": "array",
          "items": {
            "type": "string",
            "enum": [
              "stream.t-streams",
              "stream.apache-kafka",
              "stream.elasticsearch",
              "stream.sql-database",
              "stream.restful",
              "input"
            ]
          }
        }
      },
      "required": [
        "cardinality",
        "types"
      ]
    }
  },
  "type": "object",
  "properties": {
    "name": {
      "description": "The unique name for a module",
      "type": "string"
    },
    "description": {
      "description": "The description for a module",
      "type": "string"
    },
    "version": {
      "description": "The module version",
      "type": "string"
    },
    "author": {
      "description": "The module author",
      "type": "string"
    },
    "license": {
      "description": "The software license type for a module",
      "type": "string"
    },
    "inputs": {
      "description": "The specification for the inputs of a module",
      "$ref": "#/definitions/source"
    },
    "outputs": {
      "description": "The specification for the outputs of a module",
      "$ref": "#/definitions/source"
    },
    "module-type": {
      "description": "The type of a module",
      "type": "string",
      "enum": [
        "regular-streaming",
        "batch-streaming",
        "output-streaming",
        "input-streaming"
      ]
    },
    "engine-name": {
      "description": "The name of the computing core of a module",
      "type": "string"
    },
    "engine-version": {
      "description": "The version of the computing core of a module",
      "type": "string"
    },
    "validator-class": {
      "description": "The absolute path to class that is responsible for a validation of launch options",
      "type": "string"
    },
    "executor-class": {
      "description": "The absolute path to class that is responsible for a running of module",
      "type": "string"
    },
    "batch-collector-class": {
      "description": "The absolute path to class that is responsible for a batch collecting of batch-streaming module",
      "type": "string"
    }
  },
  "required": [
    "name",
    "inputs",
    "outputs",
    "module-type",
    "engine-name",
    "engine-version",
    "validator-class",
    "executor-class"
  ]
 }
 
Json Example For Each Module Type 
=====================================

Below you will find an example for the JSON schema for each module type.

.. _Json_example_input:

Input Module
----------------------

An example of a valid specification for an **input** module::

 {
  "name": "InputModule",
  "description": "Universal demux module by BW",
  "version": "1.0",
  "author": "John Smith",
  "license": "Apache 1.0",
  "inputs": {
    "cardinality": [
      0,
      0
    ],
    "types": [
      "input"
    ]
  },
  "outputs": {
    "cardinality": [
      1,
      1
    ],
    "types": [
      "stream.t-streams"
    ]
  },
  "module-type": "input-streaming",
  "engine-name": "com.bwsw.input.streaming.engine",
  "engine-version": "1.0",
  
  "validator-class": "com.bwsw.sj.stubs.module.input_streaming.Validator",
  "executor-class": "com.bwsw.sj.stubs.module.input_streaming.Executor"

 }
 
.. _Json_example_regular:

Regular Module
----------------------

An example of a valid specification for a **regular** module::

 {
  "name": "com.bw-sw.sj.demux",
  "description": "Universal demux module by BW",
  "version": "0.1",
  "author": "John Smith",
  "license": "Apache 2.0",
  "inputs": {
    "cardinality": [
      1,
      5
    ],
    "types": [
      "stream.apache-kafka",
      "stream.t-streams"
    ]
  },
  "outputs": {
    "cardinality": [
      1,
      10
    ],
    "types": [
      "stream.apache-kafka",
      "stream.t-streams"
    ]
  },
  "module-type": "regular-streaming",
  "engine-name": "regular-streaming-engine",
  "engine-version": "0.1",
  "validator-class": "com.bw-sw.sj.Validator",
  "executor-class": "com.bw-sw.sj.Executor" 
 }

.. _Json_example_batch:

Batch Module
----------------------

An example of a valid specification for a **batch** module::

 {
  "name": "BatchModule",
  "description": "Universal demux module by BW",
  "version": "1.1",
  "author": "John Smith",
  "license": "Apache 2.0",
  "inputs": {
    "cardinality": [
      1,
      1
    ],
    "types": [
      "stream.apache-kafka",
      "stream.t-streams"
    ]
  },
  "outputs": {
    "cardinality": [
      1,
      1
    ],
    "types": [
      "stream.t-streams"
    ]
  },
  "module-type": "batch-streaming",
  "engine-name": "com.bwsw.batch.streaming.engine",
  "engine-version": "1.0",
  
  "validator-class": "com.bwsw.sj.stubs.module.windowed_streaming.Validator",
  "executor-class": "com.bwsw.sj.stubs.module.windowed_streaming.Executor",
  "batch-collector-class": "com.bwsw.sj.stubs.module.windowed_streaming.NumericalBatchCollector"
 }

.. _Json_example_output:

Output Module
----------------------

An example of a valid specification for an **output** module::

 {
  "name": "OutputModule",
  "description": "Universal demux module by BW",
  "version": "1.0",
  "author": "John Smith",
  "license": "Apache 2.0",
  "inputs": {
    "cardinality": [
      1,
      1
    ],
    "types": [
      "stream.t-streams"
    ]
  },
  "outputs": {
    "cardinality": [
      1,
      1
    ],
    "types": [
      "stream.elasticsearch-"
    ]
  },
  "module-type": "output-streaming",
  "engine-name": "com.bwsw.output.streaming.engine",
  "engine-version": "1.0",
  
  "validator-class": "com.bwsw.sj.stubs.module.output.StubOutputValidator",
  "executor-class": "com.bwsw.sj.stubs.module.output.StubOutputExecutor"
 }
