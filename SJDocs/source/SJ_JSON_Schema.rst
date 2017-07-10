.. _Json_schema:

Json schema for specification of module
===========================================

Below you will find a Json schema for specification file of a module::

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
              "stream.t-stream",
              "stream.kafka",
              "elasticsearch-output",
              "jdbc-output",
              "rest-output",
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

An example of valid specification for a **regular** module::

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
      "stream.kafka",
      "stream.t-stream"
    ]
  },
  "outputs": {
    "cardinality": [
      1,
      10
    ],
    "types": [
      "stream.kafka",
      "stream.t-stream"
    ]
  },
  "module-type": "regular-streaming",
  "engine-name": "regular-streaming-engine",
  "engine-version": "0.1",
  "validator-class": "com.bw-sw.sj.Validator",
  "executor-class": "com.bw-sw.sj.Executor" 
 }


An example of valid specification for a **batch** module::

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
      "stream.kafka",
      "stream.t-stream"
    ]
  },
  "outputs": {
    "cardinality": [
      1,
      1
    ],
    "types": [
      "stream.t-stream"
    ]
  },
  "module-type": "batch-streaming",
  "engine-name": "com.bwsw.batch.streaming.engine",
  "engine-version": "1.0",
  
  "validator-class": "com.bwsw.sj.stubs.module.windowed_streaming.Validator",
  "executor-class": "com.bwsw.sj.stubs.module.windowed_streaming.Executor",
  "batch-collector-class": "com.bwsw.sj.stubs.module.windowed_streaming.NumericalBatchCollector"
 }


An example of valid specification for an **input** module::

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
      "stream.t-stream"
    ]
  },
  "module-type": "input-streaming",
  "engine-name": "com.bwsw.input.streaming.engine",
  "engine-version": "1.0",
  
  "validator-class": "com.bwsw.sj.stubs.module.input_streaming.Validator",
  "executor-class": "com.bwsw.sj.stubs.module.input_streaming.Executor"

 }


An example of valid specification for an **output** module::

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
      "stream.t-stream"
    ]
  },
  "outputs": {
    "cardinality": [
      1,
      1
    ],
    "types": [
      "elasticsearch-output"
    ]
  },
  "module-type": "output-streaming",
  "engine-name": "com.bwsw.output.streaming.engine",
  "engine-version": "1.0",
  
  "validator-class": "com.bwsw.sj.stubs.module.output.StubOutputValidator",
  "executor-class": "com.bwsw.sj.stubs.module.output.StubOutputExecutor",
  "entity-class" : "com.bwsw.sj.stubs.module.output.data.StubEsData"
 }
