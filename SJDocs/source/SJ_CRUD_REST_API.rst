.. _REST_API:

REST API Guide
========================================

.. Contents::

Introduction
---------------
The Stream Juggler platform provides developers with the opportunity of retrieving data by programming means. The range of REST API functions described on this page is intended for this purposes. 
Each method request will return specific data. Choose the method you need from the list, and generate a request according to the method description below. 

HTTP Methods
~~~~~~~~~~~~

.. csv-table:: 
  :header: "Method","Description"
  :widths: 25, 60

  "GET", "Used for retrieving resources/resource instance."
  "POST", "Used for creating resources and performing resource actions."
  "PUT", "Used for updating resource instance."
  "DELETE", "Used for deleting resource instance."


HTTP Status codes
~~~~~~~~~~~~~~~~~
	
Stream Jugler REST API uses HTTP status codes to indicate success or failure of an API call. In general, status codes in the 2xx range mean success, 4xx range mean there was an error in the provided information, and those in the 5xx range indicate server side errors. 

Commonly used HTTP status codes are listed below.
				
.. csv-table:: 
  :header: "Status code","Description"
  :widths: 25, 60

  "200", "OK"
  "201", "Created"
  "400", "Bad request"
  "404", "URL Not Found"
  "405", "Method Not Allowed (Method you have called is not supported for the invoked API)"
  "500", "Internal Error"

Requirements for requests and responses
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Expected URI scheme for requests should include the version number of the REST API, for example:: 
                 
 http://{domain}/{version}/ 

All text data must be encoded in UTF-8.

The data format in the body of the response is JSON.





CRUD Rest-API for Providers
-----------------------------------

.. csv-table::  Provider fields
  :header: "Field", "Format",  "Description"
  :widths: 25, 25,  50

  "name*", "String", "Provider name. Must contains only letters, digits or hyphens."
  "description", "String", "Provider description"
  "hosts*", "Array[String]", "ist of provider hosts"
  "login", "String", "Provider login"
  "password", "String", "Provider password"
  "type*", "String", "Provider type"
  "driver*", "String", "Driver name (for JDBC type)"

.. note:: `*` - a required field.

Provider type must be one of the following values: "cassandra", "aerospike", "zookeeper", "kafka", "ES", "JDBC", "REST"

Config settings must contain (<driver> is a value of the "driver" field):

- driver.<driver> - name of file with JDBC driver (must exists in files) (e.g. "mysql-connector-java-5.1.6.jar")
- driver.<driver>.class - name of class of this driver (e.g. "com.mysql.jdbc.Driver")
- driver.<driver>.prefix - prefix of server url: (prefix)://(host:port)/(database), one of [jdbc:mysql, jdbc:postgresql, jdbc:oracle:thin]

Create a new provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format::
 
 /v1/providers

.. csv-table::  Response
  :header: "Status code","Description"
  :widths: 25, 60

  "201", "Provider 'kafka-example' has been created."
  "400", "Cannot create provider. Errors: <list-of-errors>."
  "500", "Internal server error"

Request json example::

 {
     "name": "kafka-example",
     "description": "example kafka provider",
     "login": "my_login",
     "password": "my_pass",
     "type": "kafka",
     "hosts": [
        "192.168.1.133:9092",
        "192.168.1.135:9092"
      ]
 }


Success response example::

 {
  "status-code": 201,
  "entity": {
    "message": "Provider 'kafka-example' has been created."
  }
 }


Error response example::


 {
  "status-code": 400,
  "entity": {
    "message": "Cannot create provider. Errors: <creation_errors_string>."
  }
 }


Get provider by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/providers/{name}

.. csv-table::  Response
  :header: "Status code","Description"
  :widths: 25, 60

  "200", "Provider"
  "404", "Provider 'foo' has not been found."
  "500", "Internal server error"

Response json example::

 {
  "status-code": 200,
  "entity": {
    "provider": {
      "name": "kafka-example",
     "description": "example kafka provider",
     "login": "my_login",
     "password": "my_pass",
     "type": "kafka",
     "hosts": [
        "192.168.1.133:9092",
        "192.168.1.135:9092"
      ]
    }
  }
 }


Empty response example::

 {
  "status-code": 404,
  "entity": {
    "message": "Provider 'foo-prov' has not been found."
  }
 }


Get list of all providers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/providers

.. csv-table::  Response
  :header: "Status code","Description"
  :widths: 25, 60

  "200", "List of providers"
  "500", "Internal server error"

Success response example::

 {
  "status-code": 200,
  "entity": {
    "providers": [
      {
        "name": "kafka-exmpl",
        "description": "example kafka provider",
        "login": "my_login",
        "password": "my_pass",
        "type": "kafka",
        "hosts": [
           "192.168.1.133:9092",
           "192.168.1.135:9092"
         ]
     },
     {
       "name": "cass-prov",
       "description": "cassandra provider example",
       "login": "my_login",
       "password": "my_pass",
       "type": "cassandra",
       "hosts": [
           "192.168.1.133"
       ]
     }
    ]
  }
 }


Get list of provider types
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 
 
 /v1/providers/_types

.. csv-table::  Response
  :header: "Status code","Description"
  :widths: 25, 60

  "200 ",  "List of types "
  "500 ",  "Internal server error "

Success response example::

 {
  "entity": {
    "types": [
      "cassandra",
      "aerospike",
      "zookeeper",
      "kafka",
      "ES",
      "JDBC",
      "REST"
    ]
  },
  "statusCode": 200
 }


Delete provider by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: DELETE

Request format:: 

 /v1/providers/{name}

.. csv-table::  Response
  :header: "Status code","Description"
  :widths: 25, 60

  "200", "Provider"
  "404",  "Provider 'foo' has not been found."
  "422", "Cannot delete provider 'foo'. Provider is used in services."
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Provider 'kafka-example' has been deleted."
  }
 }



Test connection to provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Method: GET

Request format:: 

 /v1/providers/{name}/connection

.. csv-table::  Response
  :header: "Status code","Description"
  :widths: 25, 60

  "200", "Provider"
  "404", "Provider 'foo' has not been found."
  "409", "Can not establish connection to Kafka on '192.168.1.133:9092'; Can not establish connection to Kafka on '192.168.1.135:9092'"
  "500", "Internal server error"

Response example:

Provider available::

 {
  "status-code": 200,
  "entity": {
    "connection": true
  }
 }

Provider not available::

 {
  "entity": {
    "connection": false,
    "errors": "Can not establish connection to Kafka on '192.168.1.133:9092';Can not establish connection to Kafka on '192.168.1.135:9092'"
  },
  "statusCode": 409
 }


Unknown provider::

 {
  "status-code": 404,
  "entity": {
    "message": "Provider 'kafka' has not been found."
  }
 }

Get services related to a provider (by provider name)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/providers/{name}/related

.. csv-table::  Response
  :header: "Status code","Description"
  :widths: 25, 60

  "200", "List of services"
  "404", "Provider 'foo' has not been found."
  "500", "Internal server error"

Response example::

 {
  "entity": {
    "services": [
      "boo",
      "foo"
    ]
  },
  "statusCode": 200
 }



CRUD Rest-API for Services
--------------------------------------

.. note:: Method PUT is not available yet

Service fields
~~~~~~~~~~~~~~~~~

Each particular service has its own set of fields.

.. csv-table::  Available types and its aliases name for request.
  :header: "Service type","Alias for request"
  :widths: 25, 60  
  
  "Elasticsearch Index", "ESInd"
  "Kafka queue", "KfkQ"
  "T-streams queue", "TstrQ"
  "Zookeeper coordination", "ZKCoord" 
  "Redis coordination", "RdsCoord"
  "SQL database", "JDBC"
  "RESTful service", "REST"

Elasticsearch Index (ESInd)
""""""""""""""""""""""""""""""""""""""

.. csv-table::  
   :header: "Field", "Format", "Description"
   :widths: 20, 20, 60
  
   "type*", "String", "Service type"
   "name*", "String", "Service name"
   "description", "String", "Service description"
   "index*", "String", "Elasticsearch index"
   "provider*", "String", "provider name"
   "login", "String", "User name"
   "password", "String", "User password"

.. note:: Provider type can be 'ES' only

Kafka queue (KfkQ)
""""""""""""""""""""""""""""""

.. csv-table::  
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60  

  "type*", "String", "Service type"
  "name*", "String", "Service name"
  "description", "String", "Service description"
  "provider*", "String", "provider name"
  "zkProvider*", "String", "zk provider name"
  "zkNamespace*", "String", "namespace"

.. note:: provider type can be 'kafka' only

.. note::  zkProvider type can be 'zookeeper' only

T-streams queue (TstrQ)
""""""""""""""""""""""""""""""

.. csv-table::  
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60  

  "type*", "String", "Service type"
  "name*", "String", "Service name. Must contain only letters, digits or hyphens."
  "description", "String", "Service description"
  "provider*", "String", "provider name"
  "prefix*", "String", "Must be a valid znode path"
  "token*", "String", "(no more than 32 symbols)"

.. note:: provider type can be 'zookeeper' only

Zookeeper Coordination (ZKCoord)
""""""""""""""""""""""""""""""""""""""

.. csv-table::  
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60 

  "type*", "String", "Service type"
  "name*", "String", "Service name"
  "description", "String", "Service description"
  "namespace*", "String", "Zookeeper namespace"
  "provider*", "String", "provider name"

.. note:: provider type can be 'zookeeper' only

SQL database (JDBC)
"""""""""""""""""""""""""

.. csv-table::  
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60 

  "type*", "String", "Service type"
  "name*", "String", "Service name"
  "description", "String", "Service description"
  "provider*", "String", "provider name"
  "database*", "String", "Database name"

.. note:: provider type can be 'JDBC' only


RESTful service (REST)
"""""""""""""""""""""""""""""

.. csv-table::  
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60

  "type*", "String", "Service type"
  "name*", "String", "Service name"
  "description", "String", "Service description"
  "provider*", "String", "provider name"
  "basePath", "String", "Path to storage (/ by default)"
  "httpVersion", "String", "Version og HTTP protocol, one of (1.0, 1.1, 2); (1.1 by default)"
  "headers", "Object", "Extra HTTP headers. Values in object must be only String type. ({} by default)"

.. note:: provider type can be 'REST' only

.. important::  Note: * - required field.

Create a new service
~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format:: 
 
 /v1/services

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60


  "201", "Service 'test' has been created."
  "400", "Cannot create service. Errors: <list-of-errors>."
  "500", "Internal server error"

Request example::

 {
    "name": "test-rest-zk-service",
    "description": "ZK test service created with REST",
    "type": "ZKCoord",
    "provider": "zk-prov",
    "namespace": "namespace"
 }


Success response example::

 {
  "status-code": 201,
  "entity": {
    "message": "Service 'test-rest-zk-service' has been created."
  }
 }

Error response example::

 {
  "status-code": 400,
  "entity": {
    "message": "Cannot create service. Errors: <creation_errors_string>."
  }
 }


Get service by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/services/{name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Service"
  "404", "Service 'test' has not been found."
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "service": {
      "name": "test-rest-zk-service",
      "description": "ZK test service created with REST",
      "type": "ZKCoord",
      "provider": "zk-prov",
      "namespace": "namespace"
    }
  }
 }


Get list of all services
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/services

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "List of services"
  "500", "Internal server error"

Success response example::

 {
  "status-code": 200,
  "entity": {
    "services": [
      {
        "name": "test-rest-zk-service",
        "description": "ZK test service created with REST",
        "type": "ZKCoord",
        "provider": "zk-prov",
        "namespace": "namespace"
      },
      {
        "name": "rest-service",
        "description": "rest test service",
        "namespace": "mynamespace",
        "provider": "rest-prov",
        "type": "REST"
      },
      
    ]
  }
 }


Get list of service types
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/services/_types

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "List of types"
  "500|Internal server error"

Success response example::

 {
  "entity": {
    "types": [
      "ESInd",
      "KfkQ",
      "TstrQ",
      "ZKCoord",
      "JDBC",
      "REST"
    ]
  },
  "statusCode": 200
 }


Delete service by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: DELETE

Request format:: 

 /v1/services/{name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Service"
  "404", "Service 'test' has not been found."
  "422", "Cannot delete service 'test'. Service is used in streams."
  "422", "Cannot delete service 'test'. Service is used in instances."
  "500", "Internal server error"

Response example::


 {
  "status-code": 200,
  "entity": {
    "message": "Service 'foo' has been deleted."
  }
 }


Get streams and instances related to a service (by service name)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/services/{name}/related

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "List of streams and instances"
  "404", "Service 'test' has not been found."
  "500", "Internal server error"

Response example::

 {
  "entity": {
    "streams": [
      "new-tstr"
    ],
    "instances": [
      "new",
      "test",
      "input1",
      "input2",
      "input3",
      "output",
      "regular",
      "demo-regular",
      "rew",
      "input",
      "neew"
    ]
  },
  "statusCode": 200
 }

CRUD Rest-API for Streams
--------------------------------------

.. note::  Method PUT is not available yet

Stream fields
~~~~~~~~~~~~~~~~~
.. csv-table:: Response
   :header: "Stream type", "Field", "Format", "Description"
   :widths: 20, 20, 20, 40

   "all", "name*", "String", "Stream name. Must contains only lowercase letters, digits or hyphens."
   "all", "description", "String", "Stream description"
   "all", "service*", "String", "Service id"
   "all", "type*", "String", "Stream type [stream.t-stream, stream.kafka, jdbc-output, elasticsearch-output, rest-output]"
   "all", "tags", "Array[String]", "Tags"
   "stream.t-stream, stream.kafka", "partitions*", "Int", "partitions"
   "stream.kafka", "replicationFactor*", "Int", "Replication factor (how many zookeeper nodes to utilize)"
   "jdbc-output", "primary", "String", "Primary key field name used in sql database"
   "all", "force", "Boolean", "Indicates if a stream should be removed and re-created by force (if it exists). False by default."


.. important:: 
           - Service type for 'stream.t-stream' stream can be 'TstrQ' only. 
           - Service type for 'stream.kafka' stream can be 'KfkQ' only. 
           - Service type for 'jdbc-output' stream can be 'JDBC' only. 
           - Service type for 'elasticsearch-output' stream can be 'ESInd' only.
           - Service type for 'rest-output' stream can be 'REST' only.

.. note:: `*` - required field.

Create a new stream
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format:: 

 /v1/streams

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "201", "Stream 'kafka' has been created."
  "400", "Cannot create stream. Errors: <list-of-errors>."
  "500", "Internal server error"

Request example::

 {
      "name": "tstream-2",
      "description": "Tstream example",
      "partitions": 3,
      "service": "some-tstrq-service",
      "type": "stream.t-stream",
      "tags": ["lorem", "ipsum"]
 }

Success response example::

 {
   "status-code": 201,
   "entity": {
     "message": "Stream 'tstream-2' has been created."
   }
 }


Error response example::

 {
   "status-code": 400,
   "entity": {
     "message": "Cannot create stream. Errors: <creation_errors_string>."
   }
 }

Get list of all streams
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/streams

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "List of streams"
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "streams": [
      {
        "name": "tstream-2",
        "description": "Tstream example",
        "partitions": 3,
        "service": "some-tstrq-service",
        "type": "stream.t-stream",
        "tags": ["lorem", "ipsum"]
      },
      {
        "name": "kafka-stream",
        "description": "One of the streams",
        "partitions": 1,
        "service": "some-kfkq-service",
        "type": "stream.kafka",
        "tags": ["lorem", "ipsum"],
        "replicationFactor": 2
      }
    ]
  }
 }


Get list of streams types
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/streams/_types

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "List of types"
  "500", "Internal server error"

Success response example::

 {
  "entity": {
    "types": [
      "stream.t-stream",
      "stream.kafka",
      "jdbc-output",
      "elasticsearch-output",
      "rest-output"
    ]
  },
  "statusCode": 200
 }

Get stream by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/streams/{name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Stream"
  "404", "Stream 'kafka' has not been found."
  "500", "Internal server error"

Success response example::

 {
  "entity": {
    "stream": {
      "name": "echo-response",
      "description": "Tstream for demo",
      "service": "tstream_test_service",
      "tags": [
        "ping",
        "station"
      ],
      "force": false,
      "partitions": 1,
      "type": "stream.t-stream"
    }
  },
  "statusCode": 200
 }

Error response example::

 {
  "status-code": 404,
  "entity": {
    "message": "Stream 'Tstream-3' has not been found."
  }
 }

Delete stream by name
~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: DELETE

Request format:: 

 /v1/streams/{name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Stream 'kafka' has been deleted."
  "404", "Stream 'kafka' has not been found."
  "422", "Cannot delete stream 'kafka'. Stream is used in instances."
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Stream 'tstr-1' has been deleted."
  }
 }


Get instances related to a stream (by stream name)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/streams/{name}/related

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "List of instances"
  "404", "Stream 'kafka' has not been found."
  "500", "Internal server error"

Response example::

 {
  "entity": {
    "instances": [
      "pingstation-output",
      "ivan"
    ]
  },
  "statusCode": 200
 }


CRUD Rest-API for Config Settings
-----------------------------------

Config setting fields
~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60

  "name*", "String", "Name of setting (key)"
  "value*", "String", "Value of setting"
  "domain*", "String", "Name of config-domain"

.. note:: `*` - required field.

Config setting name should contain digits, lowercase letters, hyphens or periods and start with a letter.

{config-domain} should be one of the following values: 'system', 't-streams', 'kafka', 'es', 'zk', 'jdbc'

Create a new config setting
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format:: 
 
 /v1/config/settings

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "201", "{config-domain} config setting {name} has been created."
  "400", "Cannot create {config-domain} config setting. Errors: {list-of-errors}."
  "500", "Internal server error"


Request json example::

 {
  "name": "crud-rest-host",
  "value": "localhost",
  "domain": "system"
 }


Response example::


 {
  "status-code": 400,
  "entity": {
    "message": "Cannot create system config setting. Errors: <creation_errors_string>."
  }
 }


Get a config setting by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/config/settings/{config-domain}/{name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Json with requested config setting for specific config domain"
  "400",  "Cannot recognize config setting domain '{config-domain}'. Domain must be one of the following values: 'system, t-streams, kafka, es, zk, jdbc, rest'."
  "404", "{config-domain} сonfig setting {name} has not been found."
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "configSetting": {
      "name": "crud-rest-host",
      "value": "localhost",
      "domain": "system"
    }
  }
 }

Get all config settings for specific config domain
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/config/settings/{config-domain}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Json of set of config settings for specific config domain"
  "400", "Cannot recognize config setting domain '{config-domain}'. Domain must be one of the following values: 'system, t-streams, kafka, es, zk, jdbc, rest'."
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "configSettings": [
      {
        "name": "crud-rest-host",
        "value": "localhost",
        "domain": {config-domain}
     },
     {
       "name": "crud-rest-port",
       "value": "8000",
       "domain": {config-domain}
     }
    ]
  }
 }

Delete a config setting by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: DELETE

Request format:: 

 /v1/config/settings/{config-domain}/{name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "config-domain} config setting {name} has been deleted."
  "400", "Cannot recognize config setting domain '{config-domain}'. Domain must be one of the following values: 'system, t-streams, kafka, es, zk, jdbc, rest'."
  "404", "{config-domain} сonfig setting {name} has not been found."
  "500", "Internal server error"

Response example::

 {
  "status-code" : 200,
  "entity" : {
     "message" : "System config setting 'crud-rest-host' has been deleted."
  }
 }


Get all config settings
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/config/settings

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Json of set of config settings"
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "configSettings": [
      {
          "name": "crud-rest-host",
          "value": "localhost",
          "domain": "system"
      },
      {
          "name": "crud-rest-port",
          "value": "8000",
          "domain": "system"
      },
      {
          "name": "session.timeout",
          "value": "7000",
          "domain": "zk"
      }
    ]
  }
 }


Get list of domains
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/config/settings/domains

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Set of domains"
  "500", "Internal server error"

Response example::

 {
  "entity": {
    "domains": [
      "system",
      "t-streams",
      "kafka",
      "es",
      "zk",
      "jdbc"
    ]
  },
  "statusCode": 200
 }

CRUD Rest-API for Custom Files
----------------------------------------

Custom jars
~~~~~~~~~~~~~~~~~~~~

Upload custom jar
"""""""""""""""""""""""""""""

Request method: POST

Request format::

 /v1/custom/jars

Content-type: `multipart/form-data`

Attachment: java-archive as field 'jar'

Example of source message::

 POST /v1/modules HTTP/1.1
 HOST: 192.168.1.174:18080
 content-type: multipart/form-data; boundary=----WebKitFormBoundaryPaRdSyADUNG08o8p
 content-length: 1093

 ------WebKitFormBoundaryPaRdSyADUNG08o8p
 Content-Disposition: form-data; name="jar"; filename="file.jar"
 Content-Type: application/x-java-archive
 ..... //file content
 ------WebKitFormBoundaryPaRdSyADUNG08o8p--


.. csv-table:: Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Custom jar '<file_name>' has been uploaded."
  "400", "Cannot upload custom jar. Errors: {list-of-errors}. ('Specification.json is not found or invalid.'; 'Custom jar '<file_name>' already exists.'; 'Cannot upload custom jar '<file_name>'. Custom jar with name <name_from_specification> and version <version_from_specification> already exists.')"
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Custom jar is uploaded."
  }
 }


Download a custom jar by file name
""""""""""""""""""""""""""""""""""""""""""

Request method: GET

Request format:: 

 /v1/custom/jars/{custom-jar-file-name}

Response headers example::

 Access-Control-Allow-Credentials : true
 Access-Control-Allow-Headers : Token, Content-Type, X-Requested-With
 Access-Control-Allow-Origin : *
 Content-Disposition : attachment; filename=sj-transaction-generator-1.0-SNAPSHOT.jar
 Content-Type : application/java-archive
 Date : Wed, 07 Dec 2016 08:33:54 GMT
 Server : akka-http/2.4.11
 Transfer-Encoding : chunked


.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Jar-file for download"
  "404", "Jar '<custom-jar-file-name>' has not been found."
  "500", "Internal server error"

Download a custom jar by name and version
""""""""""""""""""""""""""""""""""""""""""""""""

Request method: GET

Request format:: 

 /v1/custom/jars/{custom-jar-name}/{custom-jar-version}/

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Jar-file for download"
  "404", "Jar '<custom-jar-name>-<custom-jar-version>' has not been found."
  "500", "Internal server error"

Delete a custom jar by file name
"""""""""""""""""""""""""""""""""""

Request method: DELETE

Request format:: 

 /v1/custom/jars/{custom-jar-file-name}/

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Jar named '<custom-jar-file-name>' has been deleted."
  "404", "Jar '<custom-jar-file-name>' has not been found."
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Jar named 'regular-streaming-engine-1.0.jar' has been deleted"
  }
 }
 
Delete a custom jar by name and version (from specification)
"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

Request method: DELETE

Request format:: 

 /v1/custom/jars/{custom-jar-name}/{custom-jar-version}/

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Jar named '<custom-jar-name>' of the version '<custom-jar-version>' has been deleted."
  "404", "Jar '<custom-jar-name>-<custom-jar-version>' has not been found."
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Jar named 'com.bwsw.regular.streaming.engine' of the version '0.1' has been deleted"
  }
 }


Get list of all uploaded custom jars
"""""""""""""""""""""""""""""""""""""""""""""

Request method: GET

Request format:: 

 /v1/custom/jars

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "List of uploaded custom jars"
  "500", "Internal server error"

Response example::

 {
  "entity": {
    "customJars": [
      {
        "name": "com.bwsw.fw",
        "version": "1.0",
        "size": "98060032"
      },
      {
        "name": "com.bwsw.tg",
        "version": "1.0",
        "size": "97810217"
      }
    ]
  },
  "status-code": 200
 }

Custom files
~~~~~~~~~~~~~~~~~~

Upload a custom file
""""""""""""""""""""""""""""""""

Request method: POST

Request format:: 
  
 /v1/custom/files

Content-type: `multipart/form-data`

Attachment: any file as field 'file', text field "description"

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Custom file '<custom-jar-file-name>' has been uploaded."
  "400", "Request is missing required form field 'file'."
  "409", "Custom file '<custom-jar-file-name>' already exists."
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Custom file '<custom-jar-file-name>' has been uploaded."
  }
 }


Download a custom file by file name
"""""""""""""""""""""""""""""""""""""""""""""""

Request method: GET

Request format:: 

 /v1/custom/files/{custom-jar-file-name}

Response format for file download::

 Access-Control-Allow-Origin: *
 Access-Control-Allow-Credentials: true
 Access-Control-Allow-Headers: Token, Content-Type, X-Requested-With
 Content-Disposition: attachment; filename=GeoIPASNum.dat
 Server: akka-http/2.4.11
 Date: Wed, 07 Dec 2016 09:16:22 GMT
 Transfer-Encoding: chunked
 Content-Type: application/octet-stream


.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "File for download"
  "404", "Custom file '<custom-jar-file-name>' has not been found."
  "500", "Internal server error"

Delete a custom file
""""""""""""""""""""""""""""""""""""

Request method: DELETE

Request format:: 

 /v1/custom/files/{custom-jar-file-name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Custom file '<custom-jar-file-name>' has been deleted."
  "404", "Custom file '<custom-jar-file-name>' has not been found."
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Custom file 'text.txt' has been deleted."
  }
 }


Get list of all uploaded custom files
""""""""""""""""""""""""""""""""""""""""

Request method: GET

Request format:: 

 /v1/custom/files

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "List of uploaded custom files"
  "500", "Internal server error"

Response example::

 {
  "entity": {
    "customFiles": [
      {
        "name": "GeoIPASNum.dat",
        "description": "",
        "upload-date": "Mon Jul 04 10:42:03 NOVT 2016",
        "size": "46850"
      },
      {
        "name": "GeoIPASNumv6.dat",
        "description": "",
        "upload-date": "Mon Jul 04 10:42:58 NOVT 2016",
        "size": "52168"
      }
    ]
  },
  "status-code": 200
 }


CRUD Rest-API for Modules 
------------------------------

Introduction
~~~~~~~~~~~~~~~~~~~

This is the CRUD Rest-API for modules uploaded as jar files, instantiated and running modules as well as  for custom jar files.

The following types of modules are supported in the system:
* regular-streaming (base type)
* batch-streaming
* output-streaming
* input-streaming


.. csv-table::  **Specification fields**
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60

  "name*", "String", "The unique name for a module"
  "description", "String", "The description for a module"
  "version*", "String", "The module version"
  "author", "String", "The module author"
  "license", "String", "The software license type for a module"
  "inputs*", "Iostream", "The specification for the inputs of a module"
  "outputs*", "Iostream", "The specification for the outputs of a module"
  "module-type*", "String", "The type of a module. One of [input-streaming, output-streaming, batch-streaming, regular-streaming]."
  "engine-name*", "String", "The name of the computing core of a module"
  "engine-version*", "String", "The version of the computing core of a module"
  "validator-class*", "String", "The absolute path to class that is responsible for a validation of launch options"
  "executor-class*", "String", "The absolute path to class that is responsible for a running of module"
  "batch-collector-class**", "String", "The absolute path to class that is responsible for a batch collecting of batch-streaming module"


.. csv-table:: **IOstream fields**
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60

  "cardinality*", "Array[Int]", "The boundary of interval in that a number of inputs can change. Must contain 2 items."
  "types*", "Array[String]", "The enumeration of types of inputs. Can contain only [stream.t-stream, stream.kafka, elasticsearch-output, jdbc-output, rest-output, input]"

.. note:: `*` - required field, `**` - required for batch-streaming field

Upload module
~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format:: 

 /v1/modules

Content-type: `multipart/form-data`

Attachment: java-archive as field 'jar'

Example of source message::

 POST /v1/modules HTTP/1.1
 HOST: 192.168.1.174:18080
 content-type: multipart/form-data; boundary=----WebKitFormBoundaryPaRdSyADUNG08o8p
 content-length: 109355206

 ------WebKitFormBoundaryPaRdSyADUNG08o8p
 Content-Disposition: form-data; name="jar"; filename="sj-stub-batch-streaming-1.0-     SNAPSHOT.jar"
 Content-Type: application/x-java-archive
 ..... //file content
 ------WebKitFormBoundaryPaRdSyADUNG08o8p--

.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 10, 60

  "200", "Jar file '<file_name>' of module has been uploaded."
  "400", "1. Cannot upload jar file '<file_name>' of module. Errors: file '<file_name>' does not have the .jar extension. 
  2. Cannot upload jar file '<file_name>' of module. Errors: module '<module-type>-<module-name>-<module-version>' already exists.
  3. Cannot upload jar file '<file_name>' of module. Errors: file '<file_name>' already exists.
  4. Other errors"
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Jar file '<file_name>' of module has been uploaded."
  }
 }


Download jar of uploaded module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/

Response headers example::

 Access-Control-Allow-Origin: *
 Access-Control-Allow-Credentials: true
 Access-Control-Allow-Headers: Token, Content-Type, X-Requested-With
 Content-Disposition: attachment; filename=sj-stub-batch-streaming-1.0-SNAPSHOT.jar
 Server: akka-http/2.4.11
 Date: Wed, 07 Dec 2016 05:45:45 GMT
 Transfer-Encoding: chunked
 Content-Type: application/java-archive


.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 10, 60

  "200", "Jar-file for download"
  "404", "1. Module '<module_type>-<module_name>-<module_version>' has not been found.
  2. Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage."
  "500", "Internal server error"

Delete uploaded module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: DELETE

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/

.. csv-table::  **Response**
  :header: "Status code",  "Description"
  :widths: 10, 60

  "200", "Module {module-name} for type {module-type} has been deleted"
  "404", "1. Module '<module_type>-<module_name>-<module_version>' has not been found.
  2. Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage."
  "422", "1. It's impossible to delete module '<module_type>-<module_name>-<module_version>'. Module has instances.
  2. Cannot delete file '<module-filename>'"
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Module 'regular-streaming-com.bwsw.sj.stub-1.0' has been deleted."
  }
 }


Get list of all uploaded modules
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules

.. csv-table::  **Response**
  :header: "Status code",  "Description"
  :widths: 15, 60

  "200", "List of uploaded modules"
  "500","Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "modules": [
      {
        "moduleType": "regular-streaming",
        "moduleName": "com.bwsw.sj.stub",
        "moduleVersion": "0.1",
        "size": "68954210"
      },
      {
        "moduleType": "batch-streaming",
        "moduleName": "com.bwsw.sj.stub-win",
        "moduleVersion": "0.1",
        "size": "69258954"
      }
    ]
  }
 }


Get list of types of modules
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/_types

.. csv-table::  **Response**
  :header: "Status code",  "Description"
  :widths: 15, 60

  "200", "List of types"
  "500", "Internal server error"

Response example::

 {
  "entity": {
    "types": [
      "batch-streaming",
      "regular-streaming",
      "output-streaming",
      "input-streaming"
    ]
  },
  "statusCode": 200
 }


Get list of all uploaded module for such type
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/{module-type}

.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 15, 60

  "200", "Uploaded modules for type {module-type} + {list-modules-for-type}"
  "400", "Module type '{module-type}' does not exist."
  "500", "Internal server error"

Response example::

 {
  "status-code": 200,
  "entity": {
    "modules": [
      {
        "moduleType": "regular-streaming",
        "moduleName": "com.bwsw.sj.stub",
        "moduleVersion": "0.1",
        "size": 106959926
      }
    ]
  }
 }


Get specification for uploaded module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/specification

.. csv-table::  **Response**
  :header: "Status code",  "Description"
  :widths: 15, 60

  "200", "specification json (see [[Json_schema_for_specification_of_module]])"
  "404", "1. Module '<module_type>-<module_name>-<module_version>' has not been found.
  2. Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage."
  "500", "Internal server error (including erorrs related to incorrect module type or nonexistent module)"

Response example::

 {
  "entity": {
    "specification": {
      "name": "batch-streaming-stub",
      "description": "Stub module by BW",
      "version": "1.0",
      "author": "John Smith",
      "license": "Apache 2.0",
      "inputs": {
        "cardinality": [
          1,
          10
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
          "stream.t-stream"
        ]
      },
      "moduleType": "batch-streaming",
      "engineName": "com.bwsw.batch.streaming.engine",
      "engineVersion": "1.0",
      "options": {
        "opt": 1
      },
      "validatorClass": "com.bwsw.sj.stubs.module.batch_streaming.Validator",
      "executorClass": "com.bwsw.sj.stubs.module.batch_streaming.Executor"
    }
  },
  "statusCode": 200
 }


CRUD Rest-API for Instances
-----------------------------------

Get all instances
~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 
 
 /v1/modules/instances

.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Json set of instances (in short format)"
  "500", "Internal server error"

Response entity: json example::

 {
  "status-code" : 200,
  "entity" : {[
    {
       "name": "instance-test"
       "moduleType": "batch-streaming"
       "moduleName": "com.bw.sw.sj.stub.win"
       "moduleVersion": "0.1"
       "description": ""
       "status" : "started"
       "restAddress" : "12.1.1.1:12:2900"
     },
     {
       "name": "reg-instance-test"
       "moduleType": "regular-streaming"
       "moduleName": "com.bw.sw.sj.stub.reg"
       "moduleVersion": "0.1"
       "description": ""
       "status" : "ready"
       "restAddress" : ""
     }
  ]}
 }

.. note:: Instance may have one of the following statuses:

 * ready - a newly created instance and not started yet;
 * starting - a recently launched instance but not started yet (right after the "Start" button is pushed);
 * started - the launched instance started to work;
 * stopping - a started instance in the process of stopping (right after the "Stop" button is pushed);
 * stopped - an instance that has been stopped;
 * deleting - an instance in the process of deleting (right after the "Delete" button is pressed);
 * failed - an instance that has been launched but in view of some errors is not started;
 * error - an error is detected at stopping or deleting an instance.

.. figure:: _static/InstanceStatuses.png


Stream Juggler Mesos Framework Rest
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 http://{rest-address}

.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Json set of instances (in short format)"
  "500", "Internal server error"

Response entity: json example::

 entity: {
 "tasks": [
 {
 "state": "TASK_RUNNING",
 "directories": [
 "http://stream-juggler.z1.netpoint-dc.com:5050/#/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc- S0/browse?path=/var/lib/mesos/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc- S0/frameworks/c69ce526-c420-44f4-a401-6b566b1a0823-0003/executors/pingstation-process- task0/runs/d9748d7a-3d0e-4bb6-88eb-3a3340d133d8",
 "http://stream-juggler.z1.netpoint-dc.com:5050/#/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc- S0/browse?path=/var/lib/mesos/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc- S0/frameworks/c69ce526-c420-44f4-a401-6b566b1a0823-0003/executors/pingstation-process- task0/runs/8a62f2a4-6f3c-412f-9d17-4f63e9052868"
 ],
 "state-change": "Mon Dec 05 11:56:47 NOVT 2016",
 "reason": "Executor terminated",
 "id": "pingstation-process-task0",
 "node": "3599865a-47b1-4a17-9381-b708d42eb0fc-S0",
 "last-node": "3599865a-47b1-4a17-9381-b708d42eb0fc-S0"
 }
 ],
 "message": "Tasks launched"
 }


Create an instance of a module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/instance/

.. note:: The name of an input stream should contain the  "/split" suffix (if stream's partitions should be distributed between the tasks) or "/full" (if each task should process all partitions of the stream). The stream has a 'split' mode as default. (see `SJ_CRUD_REST_API.rst#execution-plan <Execution plan>`_)

Instance fields
"""""""""""""""""""

.. csv-table::  **General instance fields**
  :header: "Field name", "Format",  "Description", "Example"
  :widths: 15, 10, 60, 20

  "name*", "String", "Required field, uniq name of creating instance. Must contain only letters, digits or hyphens.", "stub-reg-instance-1"
  "description", "String", "Description of instance", "Test instance for regular module" 
  "parallelism", "Int or String", "Value may be integer or 'max' string. If 'max', then parallelims equals minimum count of partitions of streams (1 by default)", "max" 
  "options", "Jobject", "Json with options for module", "{'opt1' : 10 }"
  "perTaskCores", "Double", "Quantity of cores for task (1 by default)", "0.5"
  "perTaskRam", "Long", "Amount of RAM for task (1024 by default)", "256"
  "jvmOptions", "Jobject", "Json with jvm-options. It is important to emphasize that mesos kill a task if it uses more memory than 'perTaskRam' parameter. There is no options by default. Defined options in the example fit the perTaskRam=192 and it's recommended to laucnh modules. In general, the sum of the following parameters: Xmx, XX:MaxDirectMemorySize and XX:MaxMetaspaceSize, should be less than perTaskRam; XX:MaxMetaspaceSize must be grater than Xmx by 32m or larger.",  "{'-Xmx': '32m', '-XX:MaxDirectMemorySize=': '4m', '-XX:MaxMetaspaceSize=': '96m' }"
  "nodeAttributes", "Jobject", "Json with map attributes for framework", "{ '+tag1' : 'val1', '-tag2' : 'val2'}"
  "coordinationService*", "String", "Service name of zookeeper service", "zk_service" 
  "environmentVariables", "Jobject", "Using in framework", "{ 'LIBPROCESS_IP' : '176.1.0.17' }"
  "performanceReportingInterval", "Long", "Interval for creating report of performance metrics of module in ms (60000 by default)",  "5000696"

.. csv-table::   **Input-streaming instance fields**
  :header: "Field name", "Format",  "Description", "Example"
  :widths: 15, 10, 60, 20

  "checkpointMode*", "String", "Value must be time-interval or every-nth",  "every-nth" 
  "checkpointInterval*", "Int ", "Interval for creating checkpoint",  "100 "
  "outputs*", "List[String] ", "Names of output streams (must be stream.t-stream only)", "[s3, s4] "
  "duplicateCheck",  "Boolean", "The flag points  if every envelope (an envelope key) has to be checked on duplication or not. (false by default) **Note**: You can indicate the 'duplicateCheck' field in the instance to set up a default policy for message checking on duplication. Use the 'InputEnvelope' flag in the :ref:`input-module`  for special cases* ", "true "
  "lookupHistory*", "Int", "How long an unique key of envelope will stay in a queue for checking envelopes on duplication (in seconds). If it is not 0, entries that are older than this time and not updated for this time are evicted automatically accordingly to an eviction-policy. Valid values are integers between 0 and Integer.MAX VALUE. Default value is 0, which means infinite.", "1000"
  "queueMaxSize*", "Int", "Maximum size of the queue that contains the unique keys of envelopes. When maximum size is reached, the queue is evicted based on the policy defined at default-eviction-policy (should be greater than 271)", "500"
  "defaultEvictionPolicy", "String", "Must be only 'LRU' (Least Recently Used), 'LFU' (Least Frequently Used) or 'NONE' (NONE by default)", "LRU" 
  "evictionPolicy", "String",  "An eviction policy of duplicates of incoming envelope. Must be only 'fix-time' or 'expanded-time'. The first means that a key of envelope will be contained only {lookup-history} seconds. The second means that if a duplicate of the envelope appears, the key presence time will be updated ('fix-time' by default)", "fix-time" 
  "backupCount", "Int", "The number of backup copies you want to have (0 by default, maximum 6). Sync backup operations have a blocking cost which may lead to latency issues. You can skip this field if you do not want your entries to be backed up, e.g. if performance is more important than backing up.",  2 
  "asyncBackupCount", "Int", "Flag points an every envelope (an envelope key) has to be checked on duplication or not (0 by default). The backup operations are performed at some point in time (non-blocking operation). You can skip this field if you do not want your entries to be backed up, e.g. if performance is more important than backing up.", 3 

.. csv-table::  **Regular-streaming instance fields**
  :header: "Field name", "Format",  "Description", "Example"
  :widths: 15, 10, 60, 20

  "checkpointMode*", "String", "Value must be 'time-interval' or 'every-nth'", "every-nth" 
  "checkpointInterval*", "Int", "Interval for creating checkpoint", 100 
  "inputs*", "List[String]", "Names of input streams. Name format must be <stream-name>/<'full' or 'split'> ('split' by default). Stream must exist in database (must be stream.t-stream or stream.kafka)", "[s1/full, s2/split]" 
  "outputs*", "List[String]", "Names of output streams (must be stream.t-stream only)", "[s3, s4]" 
  "startFrom", "String or Datetime", "Value must be 'newest', 'oldest' or datetime. If instance have kafka input streams, then 'start-from' must be only 'oldest' or 'newest' (newest by default)", "newest" 
  "stateManagement", "String", "Must be 'ram' or 'none' (none by default)", "ram" 
  "stateFullCheckpoint", "Int", "Interval for full checkpoint (100 by default)", "5"
  "eventWaitTime", "Long", "Idle timeout, when not messages (1000 by default)", 10000


.. csv-table:: **Output-streaming instance fields**
  :header: "Field name", "Format",  "Description", "Example"
  :widths: 15, 10, 60, 20

  "checkpointMode*", "String",  "Value must be 'time-interval'", "time-interval" 
  "checkpointInterval*", "Int", "Interval for creating checkpoint", 100 
  "input*", "String", "Names of input stream. Must be only 't-stream' type. Stream for this type of module is 'split' only.  Stream must be exists in database.", "s1" 
  "output*", "String", "Names of output stream (must be elasticsearch-output, jdbc-ouptut or rest-output)", "es1" 
  "startFrom", "String or Datetime", "Value must be 'newest', 'oldest' or datetime (newest by default)", "newest" 

.. csv-table:: **Batch-streaming instance fields**
  :header: "Field name", "Format",  "Description", "Example"
  :widths: 15, 10, 60, 20

  "outputs*", "List[String]", "Names of output streams (must be stream.t-stream only)", "[s3, s4]"
  "window", "Int", "Count of batches that will be contained into a window (1 by default). Must be greater than zero",  3 
  "slidingInterval", "Int", "The interval at which a window will be shifted (сount of batches that will be removed from the window after its processing). Must be greater than zero and less or equal than window (1 by default)", 3
  "inputs*", "String", "Names of input streams. Name format must be <stream-name>/<'full' or 'split'> ('split' by default).
 Stream must exist in database (must be stream.t-stream or stream.kafka)", "[s1/full]"
  "startFrom", "String or Datetime", "Value must be 'newest', 'oldest' or datetime. If instance have kafka input streams, then 'start-from' must be only 'oldest' or 'newest' (newest by default)", "newest" 
  "stateManagement", "String", "Must be 'ram' or 'none' (none by default)",  "ram" 
  "stateFullCheckpoint", "Int", "Interval for full checkpoint (100 by default)", 5 
  "eventWaitTime", "Long", "Idle timeout, when not messages (1000 by default)", 10000 

.. note:: `*` - required field.


Regular-streaming module json format::

 {
  "name" : String,
  "description" : String,
  "inputs" : List[String],
  "outputs" : List[String],
  "checkpointMode" : "time-interval" | "every-nth",
  "checkpointInterval" : Int,
  "stateManagement" : "none" | "ram",
  "stateFullCheckpoint" : Int,
  "parallelism" : Int,
  "options" : {},
  "startFrom" : "oldest" | "newest" | datetime (as timestamp),
  "perTaskCores" : Double,
  "perTaskRam" : Int,
  "jvmOptions" : {"-Xmx": "32m", "-XX:MaxDirectMemorySize=": "4m", "-XX:MaxMetaspaceSize=": "96m" },
  "nodeAttributes" : {},
  "eventWaitTime" : Int,
  "coordinationService" : String,
  "performanceReportingInterval" : Int
 }


Batch-streaming module json format::

 {
  "name" : String,
  "description" : String,
  "inputs" : [String],
  "stateManagement" : "none" | "ram",
  "stateFullCheckpoint" : Int,
  "parallelism" : Int,
  "options" : {},
  "startFrom" : "newest" | "oldest",
  "perTaskCores" : Double,
  "perTaskRam" : Int,
  "jvmOptions" : {"-Xmx": "32m", "-XX:MaxDirectMemorySize=": "4m", "-XX:MaxMetaspaceSize=": "96m" },
  "nodeAttributes" : {},
  "eventWaitTime" : Int,
  "coordinationService" : String,
  "performanceReportingInterval" : Int
 }


Output-streaming module json format::

 {
  "name" : String,
  "description" : String,
  "input" : String,
  "output" : String,
  "checkpointMode" : "time-interval",
  "checkpointInterval" : Int,
  "parallelism" : Int,
  "options" : {},
  "startFrom" : "oldest" | "newest" | datetime (as timestamp),
  "perTaskCores" : Double,
  "perTaskRam" : Int,
  "jvmOptions" : {"-Xmx": "32m", "-XX:MaxDirectMemorySize=": "4m", "-XX:MaxMetaspaceSize=": "96m" },
  "nodeAttributes" : {},
  "coordinationService" : String,
  "performanceReportingInterval" : Int
 }


Input-streaming module json format::

 {
  "name" : String,
  "description" : String,
  "outputs" : List[String],
  "checkpointMode" : "time-interval" | "every-nth",
  "checkpointInterval" : Int,
  "parallelism" : Int,
  "options" : {},
  "perTaskCores" : Double,
  "perTaskRam" : Int,
  "jvmOptions" : {"-Xmx": "32m", "-XX:MaxDirectMemorySize=": "4m", "-XX:MaxMetaspaceSize=": "96m" },
  "nodeAttributes" : {},
  "coordinationService" : String,
  "performanceReportingInterval" : Int,
  "lookupHistory" : Int,
  "queueMaxSize" : Int,
  "defaultEvictionPolicy" : "LRU" | "LFU",
  "evictionPolicy" : "fix-time" | "expanded-time",
  "duplicateCheck" : true | false,
  "backupCount" : Int,
  "asyncBackupCount" : Int
 }


Request json example for creating batch-streaming instance::

 {
  "name" : "stub-instance-win",
  "description" : "",
  "mainStream" : "ubatch-stream",
  "batchFillType": {
    "typeName" : "every-nth",
    "value" : 100
  },
  "outputs" : ["ubatch-stream2"],
  "stateManagement" : "ram",
  "stateFullCheckpoint" : 1,
  "parallelism" : 1,
  "options" : {},
  "startFrom" : "oldest",
  "perTaskCores" : 2,
  "perTaskRam" : 192,
  "jvmOptions" : {
    "-Xmx": "32m",
    "-XX:MaxDirectMemorySize=": "4m",
    "-XX:MaxMetaspaceSize=": "96m"
  },
  "nodeAttributes" : {},
  "eventWaitTime" : 10000,
  "coordinationService" : "a-zoo",
  "performanceReportingInterval" : 50054585 
 }


.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 10, 60
  
  "201", "Instance '<instance_name>' for module '<module_type>-<module_name>-<module_version>' has been created."
  "400", "1. Cannot create instance of module. The instance parameter 'options' haven't passed validation, which is declared in a method, called 'validate'. This method is owned by a validator class that implements StreamingValidator interface. Errors: {list-of-errors}.
  2. Cannot create instance of module. Errors: {list-of-errors}."
  "404", "1. Module '<module_type>-<module_name>-<module_version>' has not been found.
  2. Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage."
  "500", "Internal server error (including erorrs related to incorrect module type or nonexistent module)"


Json-example of a created instance::

 "instance": {
  "stage": {
      "state": "to-handle",
      "datetime": 1481092354533,
      "duration": 0
    }
  },
  "status": "ready",
  "name": "stub-instance-win",
  "description": "",
  "parallelism": 1,
  "options": {
    
  },
  "engine": "com.bwsw.batch.streaming.engine-1.0",
  "window": 1,
  "outputs": [
    "ubatch-stream2"
  ],
  "perTaskCores": 2.0,
  "perTaskRam": 128,
  "jvmOptions" : {
    "-Xmx": "32m",
    "-XX:MaxDirectMemorySize=": "4m",
    "-XX:MaxMetaspaceSize=": "96m"
  },
  "nodeAttributes": {
    
  },
  "coordinationService": "a-zoo",
  "environmentVariables": {
    
  },
  "performanceReportingInterval": 50054585,
  "inputs": [
    "ubatch-stream"
  ],
  "slidingInterval": 1,
  "executionPlan": {
    "tasks": {
      "stub-instance-win-task0": {
        "inputs": {
          "ubatch-stream": [
            0,
            2
          ]
        }
      }
    }
  },
  "startFrom": "oldest",
  "stateManagement": "ram",
  "stateFullCheckpoint": 1,
  "eventWaitTime": 10000,
  "restAddress" : ""
 }
 }


Execution plan
"""""""""""""""""""

A created instance contains an execution plan that you don't provide. 

Execution plan consists of tasks. The number of tasks equals to a parallelism parameter.

Each task has a unique name within execution plan. Also the task has a set of input stream names and their intervals of partitions.

Altogether it provides the information of the sources from which the data will be consumed.

Execution plan example::

 "executionPlan": {
    "tasks": {
      "stub-instance-win-task0": {
        "inputs": {
          "ubatch-stream": [
            0,
            2
          ]
        }
      }
    }
  }


.. note:: The execution plan doesn't exist in instances of an input module. An instance of an input-module contains a 'tasks' field.

Each task has a name, host and port. The host and port defines an address to which the data should be sent for the input module to process them.

Json format of 'tasks' field for instance of input module::

 {
  "instance-name-task0" : {
    "host" : String,
    "port" : Int
  },
  "instance-name-task1" : {
    "host" : String,
    "port" : Int
  },
  "instance-name-taskN" : {
    "host" : String,
    "port" : Int
  }
 }


Stage
"""""""""""""""""

A created instance contains a stage that you don't provide.

First of all it should be noted that a framework is responsible for launching instance.

The stage is used to display information about current status of framework. It allows you to follow  start or stop processes of instance.

The stage consists of state, datetime and duration. Let's look at every parameter in detail.

1. *State* can have one of the following values. The value corresponds to an instance status:

* to-handle - a newly created instance and not started yet;
* starting -  a recently launched instance but not started yet (right after the "Start" button is pushed);
* started - the launched instance started to work;
* stopping - a started instance that has been stopped (right after the "Stop" button is pushed);
* stopped - an instance that has been stopped;
* deleting - an instance in the process of deleting (right after the "Delete" button is pressed);
* failed - an instance that has been launched but in view of some errors is not started;
* error - an error is detected when stopping the instance.

2. *Datetime* defines the time when a state has been changed

3. *Duration* means how long a stage has got a current state. This field makes sense if a state field is in a 'starting', a 'stopping' or a 'deleting' status.

Json example of this field::

 "stage": {
    "state": "started",
    "datetime": 1481092354533,
    "duration": 0
  }
 }


Get instances related to a specific module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/related

.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 10, 60

  "200", "List of instances"
  "404", "1. Module '<module_type>-<module_name>-<module_version>' has not been found.
  2. Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage."
  "500", "Internal server error (including erorrs related to incorrect module type or nonexistent module)"

Response entity json example::

 {
  "status-code": 200,
  "entity": {
    "instances": [
      "test-instance",
      "boo"
    ]
  }
 }


Get all instances of a specific module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 
 
 /v1/modules/{module-type}/{module-name}/{module-version}/instance/

.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 15, 60

  "200", "List of instances of module"
  "404", "1. Module '<module_type>-<module_name>-<module_version>' has not been found.
  2. Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage."
  "500", "Internal server error (including erorrs related to incorrect module type or nonexistent module)"

Response entity: json example::

 {
  "status-code": 200,
  "entity": {
    "instances": [
      {
        
      },
      {
        
      },
      ...,
      {
        
      }
    ]
  }
 }


Get an instance of a specific module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/instance/{instance-name}/

.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Instance"
  "404", "Instance '<instance_name>' has not been found."
  "500", "Internal server error"

Delete an instance of a specific module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: DELETE

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/instance/{instance-name}/

.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 10, 60

  "200", "1. Instance '<instance_name>' is being deleted.
  2. Instance '<instance_name>' has been deleted."
  "404", "Instance '<instance_name>' has not been found."
  "422", "Cannot delete of instance '<instance_name>'. Instance is not been stopped, failed or ready."
  "500", "Internal server error"

.. note:: This process includes a destruction of framework on mesos.

Response example::


 {
  "status-code" : 200,
  "entity" : {
     "message" : "Instance 'stub-instance-1' has been deleted."
  }
 }


Start an instance
~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/instance/{instance-name}/start/

.. csv-table::  **Response**
  :header: "Status code",  "Description"
  :widths: 15, 60

  "200", "Instance '<instance_name>' is being launched."
  "404", "Instance '<instance_name>' has not been found."
  "422", "Cannot start of instance. Instance has already launched."
  "500", "Internal server error"

.. note:: To start an instance it should have a status: "failed", "stopped" or "ready". 

When instance is starting, framework starts on Mesos.

Response example::

 {
  "status-code" : 200,
  "entity" : {
     "message" : "Instance '<instance_name>' is being launched."
  }
 }


Get the information about instance tasks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/instance/{instance-name}/tasks/

.. csv-table::  
  :header: "Status code",  "Description"
  :widths: 10, 60

  "200", "Instance framework tasks info."
  "404", "Instance '<instance_name>' has not been found."
  "422", "Cannot get instance framework tasks info. The instance framework has not been launched."
  "500", "Internal server error (including erorrs related to incorrect module type or nonexistent module and «Instance '<instance_name>' has not been found.»)"

Response example::

 {
  "status-code": 200,
  "entity": {
    "tasks": [
      {
        "state": "TASK_RUNNING",
        "directories": [
          "http://stream-juggler.z1.netpoint-dc.com:5050/#/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/browse?path=/var/lib/mesos/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/frameworks/c69ce526-c420-44f4-a401-6b566b1a0823-0003/executors/pingstation-process-task0/runs/d9748d7a-3d0e-4bb6-88eb-3a3340d133d8",
          "http://stream-juggler.z1.netpoint-dc.com:5050/#/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/browse?path=/var/lib/mesos/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/frameworks/c69ce526-c420-44f4-a401-6b566b1a0823-0003/executors/pingstation-process-task0/runs/8a62f2a4-6f3c-412f-9d17-4f63e9052868"
        ],
        "state-change": "Mon Dec 05 11:56:47 NOVT 2016",
        "reason": "Executor terminated",
        "id": "pingstation-process-task0",
        "node": "3599865a-47b1-4a17-9381-b708d42eb0fc-S0",
        "last-node": "3599865a-47b1-4a17-9381-b708d42eb0fc-S0"
      }
    ]
  }
 }


Stop an instance
~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 
 
 /v1/modules/{module-type}/{module-name}/{module-version}/instance/{instance-name}/stop/

.. csv-table::  
  :header: "Status code",  "Description"
  :widths: 10, 60

  "200", "Instance '<instance_name>' is being stopped."
  "404", "Instance '<instance_name>' has not been found."
  "422", "Cannot stop instance. Instance has not been started."
  "500", "Internal server error (including erorrs related to incorrect module type or nonexistent module and «Instance '<instance_name>' has not been found.»)"

.. note:: To stop an instance its status should be "started". 

When instance stops, framework suspends on mesos.


Response example::

 {
  "status-code" : 200,
  "entity" : {
     "message" : "Instance '<instance_name>' is being stopped."
  }
 }
