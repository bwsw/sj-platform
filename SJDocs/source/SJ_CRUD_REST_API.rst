.. _REST_API:

REST API Guide
========================================

.. Contents::

Introduction
---------------
The Stream Juggler platform provides developers with the opportunity of retrieving data by programming means. The range of REST API functions described on this page is intended for this purposes. 

Each method request will return specific data. Choose the method you need from the list, and generate a request according to the method description below. 

REST API provides access to resources (data entities) via URI paths. To use the REST API, the application will make an HTTP request and parse the response. The response format is JSON. The methods are standard HTTP methods: GET, POST and DELETE.

HTTP Methods
~~~~~~~~~~~~

.. csv-table:: 
  :header: "Method","Description"
  :widths: 25, 60

  "GET", "Used for retrieving resources/resource instance."
  "POST", "Used for creating resources and performing resource actions."
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

The range of REST API methods described below allows to create or delete a provider, get the information on the provider, get the list of providers in the system, test connection to a provider.

.. csv-table::  Provider fields
  :header: "Field", "Format",  "Description", "Requirements"
  :widths: 15, 15, 25, 25

  "name*", "String", "Provider name.", "Name must be unique and contain only letters, digits or hyphens."
  "description", "String", "Provider description.", ""
  "hosts*", "Array[String]", "List of provider hosts.", ""
  "login", "String", "Provider login.", ""
  "password", "String", "Provider password.", ""
  "type*", "String", "Provider type.", "One of the following values are possible: 'provider.elasticsearch', 'provider.apache-kafka', 'provider.apache-zookeeper', 'provider.sql-database', 'provider.restful'."
  "driver*", "String", "Driver name.", "For 'provider.sql-database' provider type only."

.. important:: 
   - Configurations must contain the following settings (<driver> is a value of the "driver" field) of sql database domain:
      
     - driver.<driver> - name of file with JDBC driver (must exists in files) (e.g. "mysql-connector-java-5.1.6.jar")
     - driver.<driver>.class - name of class of this driver (e.g. "com.mysql.jdbc.Driver")
     - driver.<driver>.prefix - prefix of server url: (prefix)://(host:port)/(database), one of [jdbc:mysql, jdbc:postgresql, jdbc:oracle:thin]

.. note:: `*` - a required field.

Create a new provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format::
 
 /v1/providers

.. csv-table::  Response
  :header: "Status code","Description"
  :widths: 25, 60

  "201", "Provider <provider name> has been created."
  "400", "Cannot create provider. Errors: <list-of-errors>."
  "500", "Internal server error."

Request json example::

 {
     "name": "kafka-provider",
     "description": "example of kafka provider",
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
    "message": "Provider 'kafka-provider' has been created."
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

  "200", "Provider."
  "404", "Provider <provider name> has not been found."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "provider": {
      "name": "kafka-provider",
     "description": "example kafka provider",
     "login": "my_login",
     "password": "my_pass",
     "type": "kafka",
     "hosts": [
        "192.168.1.133:9092",
        "192.168.1.135:9092"
      ]
      "creationDate": "Thu Jul 20 08:32:51 NOVT 2017" 
    }
  }
 }


Error response example::

 {
  "status-code": 404,
  "entity": {
    "message": "Provider 'kafka-provider' has not been found."
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

  "200", "List of providers."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "providers": [
      {
        "name": "kafka-provider",
        "description": "example kafka provider",
        "login": "my_login",
        "password": "my_pass",
        "type": "kafka",
        "hosts": [
           "192.168.1.133:9092",
           "192.168.1.135:9092"
         ]
	 "creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
     },
     {
       "name": "es-provider",
       "description": "elasticsearch provider example",
       "login": "my_login",
       "password": "my_pass",
       "type": "ES",
       "hosts": [
           "192.168.1.133"
       ],
       "creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
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

  "200 ",  "List of types. "
  "500 ",  "Internal server error. "

Success response example::

 {
  entity: {
    types: [
      {
        id: "provider.elasticsearch",
        name: "Elasticsearch" 
      },
      {
        id: "provider.apache-zookeeper",
        name: "Apache Zookeeper" 
      },
      {
        id: "provider.apache-kafka",
        name: "Apache Kafka" 
      },
      {
        id: "provider.restful",
        name: "RESTful" 
      },
      {
        id: "provider.sql-database",
        name: "SQL database" 
      }
    ]
  },
  status-code: 200
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
  "404", "Provider <provider name> has not been found."
  "422", "Cannot delete provider <provider name>. Provider is used in services."
  "500", "Internal server error"

Success response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Provider 'kafka-provider' has been deleted."
  }
 }

Error response example::

 {
    "entity": {
        "message": "Cannot delete provider 'provider-name'. Provider is used in services."
    },
    "status-code": 422
 }

Test connection to provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Method: GET

Request format:: 

 /v1/providers/{name}/connection

.. csv-table::  Response
  :header: "Status code","Description"
  :widths: 25, 60

  "200", "Provider."
  "404", "Provider <provider name> has not been found."
  "409", "Provider is not available."
  "500", "Internal server error."

Success response example (provider is available)::

 {
  "status-code": 200,
  "entity": {
    "connection": true
  }
 }

Error response example:

Provider is not available::

 {
  "entity": {
    "connection": false,
    "errors": "Can not establish connection to Kafka on '192.168.1.133:9092'"
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

  "200", "List of services."
  "404", "Provider <provider name> has not been found."
  "500", "Internal server error."

Success response example::

 {
  "entity": {
    "services": [
      "abc",
      "def"
    ]
  },
  "statusCode": 200
 }

Error response example::

 {
    "entity": {
        "message": "Provider 'kafka-provider' has not been found."
    },
    "status-code": 404
 }
 
.. tip:: A full range of error responses can be found at :ref:`Provider_Errors`

CRUD Rest-API for Services
--------------------------------------

The range of REST API methods described below allows to create or delete a service, get the information on the service, get the list of services and service types in the system, get streams and instances related to a service.

Service fields
~~~~~~~~~~~~~~~~~

Each particular service has its own set of fields.

.. csv-table::  Service type names for request.
 :header: "Service Types"
 :widths: 25
  
 "service.elasticsearch"
 "service.apache-kafka"
 "service.t-streams"
 "service.apache-zookeeper"
 "service.sql-database"
 "service.restful"

Elasticsearch 
""""""""""""""""""""""""""""""""""""""

.. csv-table::  
   :header: "Field", "Format", "Description", "Requirements"
   :widths: 15, 15, 20, 20
  
   "type*", "String", "Service type.", ""
   "name*", "String", "Service name.", "Must be unique and contain only letters, digits or hyphens."
   "description", "String", "Service description.", ""
   "index*", "String", "Elasticsearch index.", ""
   "provider*", "String", "Provider name.", "Provider can be of 'provider.elasticsearch' type only."
   "login", "String", "User name.", ""
   "password", "String", "User password.", ""
   "creationDate", "String", "The time when a service has been created.", ""

Apache Kafka 
""""""""""""""""""""""""""""""

.. csv-table::  
  :header: "Field", "Format",  "Description", "Requirements"
  :widths: 15, 15, 20, 20   

  "type*", "String", "Service type", ""
  "name*", "String", "Service name", "Must be unique and contain only letters, digits or hyphens."
  "description", "String", "Service description", ""
  "provider*", "String", "Provider name.", "Provider can be of 'provider.apache-kafka' type only."
  "zkProvider*", "String", "zk provider name.", "zkProvider can be of 'provider.apache-zookeeper' type only."
  "zkNamespace*", "String", "Namespace.", ""
  "creationDate", "String", "The time when a service has been created.", ""

T-streams 
""""""""""""""""""""""""""""""

.. csv-table::  
  :header: "Field", "Format",  "Description", "Requirements"
  :widths: 15, 15, 20, 20  

  "type*", "String", "Service type.", ""
  "name*", "String", "Service name.", "Must be unique and contain only letters, digits or hyphens."
  "description", "String", "Service description.", ""
  "provider*", "String", "Provider name.", "Provider can be of 'provider.apache-zookeeper' type only."
  "prefix*", "String", "A znode path", "Must be a valid znode path."
  "token*", "String", "A token", "Should not contain more than 32 symbols."
  "creationDate", "String", "The time when a service has been created.", ""


Apache Zookeeper 
""""""""""""""""""""""""""""""""""""""

.. csv-table::  
  :header: "Field", "Format",  "Description", "Requirements"
  :widths: 15, 15, 20, 20 

  "type*", "String", "Service type.", ""
  "name*", "String", "Service name.", "Must be unique and contain only letters, digits or hyphens."
  "description", "String", "Service description.", ""
  "namespace*", "String", "Zookeeper namespace.", ""
  "provider*", "String", "Provider name.", "Provider can be of 'provide.apache-zookeeper' type only."
  "creationDate", "String", "The time when a service has been created.", ""

SQL database 
"""""""""""""""""""""""""

.. csv-table::  
  :header: "Field", "Format",  "Description", "Requirements"
  :widths: 15, 15, 20, 20 

  "type*", "String", "Service type.", ""
  "name*", "String", "Service name.", "Must be unique and contain only letters, digits or hyphens."
  "description", "String", "Service description.", ""
  "provider*", "String", "Provider name.", "Provider can be of 'JDBC' type only."
  "database*", "String", "Database name.", ""
  "creationDate", "String", "The time when a service has been created.", ""

RESTful 
"""""""""""""""""""""""""""""

.. csv-table::  
  :header: "Field", "Format",  "Description", "Requirements"
  :widths: 15, 15, 20, 20

  "type*", "String", "Service type.", ""
  "name*", "String", "Service name.", "Must be unique and contain only letters, digits or hyphens."
  "description", "String", "Service description.", ""
  "provider*", "String", "Provider name.", "Provider can be  of 'provider.restful' type only."
  "basePath", "String", "Path to storage (/ by default)", ""
  "httpScheme", "String", "The time when a service has been created.", "Scheme of HTTP protocol, one of ('http', 'https'); ('http' by default)"
  "httpVersion", "String", "Version og HTTP protocol", "One of (1.0, 1.1, 2); (1.1 by default)"
  "headers", "Object", "Extra HTTP headers.", "Values in object must be only String type. ({} by default)"
  "creationDate", "String", "The time when a service has been created.", ""
  
.. note:: `*` - required fields.

Create a new service
~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format:: 
 
 /v1/services

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60


  "201", "Service <service name> has been created."
  "400", "Cannot create service. Errors: <list-of-errors>."
  "500", "Internal server error."

Request json example::

 {
    "name": "test-rest-zk-service",
    "description": "ZK test service created with REST",
    "type": "service.apache-zookeeper",
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

  "200", "Service."
  "404", "Service <service name> has not been found."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "service": {
      "name": "test-rest-zk-service",
      "description": "ZK test service created with REST",
      "type": "service.apache-zookeeper",
      "provider": "zk-prov",
      "namespace": "namespace"
    }
  }
 }

Error response example::

 {
   "status-code": 404,
   "entity": {
     "message": "Service <service name> has not been found."
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

  "200", "List of services."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "services": [
      {
        "name": "test-rest-zk-service",
        "description": "ZK test service created with REST",
        "type": "service.apache-zookeeper",
        "provider": "zk-prov",
        "namespace": "namespace"
	"creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
      },
      {
        "name": "rest-service",
        "description": "rest test service",
        "namespace": "mynamespace",
        "provider": "rest-prov",
        "type": "service.restful"
	"creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
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

  "200", "List of types."
  "500", "Internal server error."

Success response example::

 {
  entity: {
    types: [
      {
        id: "service.elasticsearch",
        name: "Elasticsearch" 
      },
      {
        id: "service.sql-database",
        name: "SQL database" 
      },
      {
        id: "service.t-streams",
        name: "T-streams" 
      },
      {
        id: "service.apache-kafka",
        name: "Apache Kafka" 
      },
      {
        id: "service.apache-zookeeper",
        name: "Apache Zookeeper" 
      },
      {
        id: "service.restful",
        name: "RESTful" 
      }
    ]
  },
  status-code: 200
 }


Delete service by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: DELETE

Request format:: 

 /v1/services/{name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Service."
  "404", "Service <service name> has not been found."
  "422", "Cannot delete service <service name>. Service is used in streams."
  "422", "Cannot delete service <service name>. Service is used in instances."
  "500", "Internal server error."

Success response example::


 {
  "status-code": 200,
  "entity": {
    "message": "Service 'abc' has been deleted."
  }
 }


Error response example::

 {
   "status-code": 404,
   "entity": {
     "message": "Service <service name> has not been found."
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

  "200", "List of streams and instances."
  "404", "Service <service name> has not been found."
  "500", "Internal server error."

Success response example::

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

Error response example::

 {
   "status-code": 404,
   "entity": {
     "message": "Service <service name> has not been found."
   }
 }

.. tip:: A full range of error responses can be found at :ref:`Services_Errors`

CRUD Rest-API for Streams
--------------------------------------

The range of REST API methods described below allows to create or delete a stream, get the information on the stream, get the list of streams and stream types in the system, get instances related to a stream.

Stream fields
~~~~~~~~~~~~~~~~~

.. csv-table::  Stream types for requests
 :header: "Types"
 :widths: 25
  
 "stream.elasticsearch"
 "stream.apache-kafka"
 "stream.t-streams"
 "stream.sql-database"
 "stream.restful"

.. csv-table:: Response
   :header: "Field", "Format", "Description", "Requirements"
   :widths: 10, 10, 20, 20
 
   "name*", "String", "Stream name.", "Must be unique and contain only lowercase letters, digits or hyphens."
   "description", "String", "Stream description", ""
   "service*", "String", "Service id", ""
   "type*", "String", "Stream type", "One of the following values : stream.t-streams, stream.apache-kafka, stream.sql-database, stream.elasticsearch, stream.restful."
   "tags", "Array[String]", "Tags.", ""
   "partitions*", "Int", "Partitions.", "For stream.t-streams, stream.apache-kafka types"
   "replicationFactor*", "Int", "Replication factor (how many zookeeper nodes to utilize).", "For stream.apache-kafka stream type only."
   "primary", "String", "Primary key field name used in sql database.", "For stream.sql-database stream type only."
   "force", "Boolean", "Indicates if a stream should be removed and re-created by force (if it exists). False by default.", ""


.. important:: 
           - Service type for 'stream.t-streams' stream can be 'service.t-streams' only. 
           - Service type for 'stream.apache-kafka' stream can be 'service.apache-kafka' only. 
           - Service type for 'stream.sql-database' stream can be 'service.sql-database' only. 
           - Service type for 'stream.elasticsearch' stream can be 'service.elasticsearch' only.
           - Service type for 'stream.restful' stream can be 'service.restful' only.

.. note:: `*` - required field.

Create a new stream
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format:: 

 /v1/streams

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "201", "Stream <stream name> has been created."
  "400", "Cannot create stream. Errors: <list-of-errors>."
  "500", "Internal server error."

Request example::

 {
      "name": "tstream-2",
      "description": "Tstream example",
      "partitions": 3,
      "service": "some-tstrq-service",
      "type": "stream.t-streams",
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

  "200", "List of streams."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "streams": [
      {
        "name": "tstream-2",
        "description": "Tstream example",
        "partitions": 3,
        "service": "some-tstrq-service",
        "type": "stream.t-streams",
        "tags": ["lorem", "ipsum"]
	"creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
      },
      {
        "name": "kafka-stream",
        "description": "One of the streams",
        "partitions": 1,
        "service": "some-kfkq-service",
        "type": "stream.apache-kafka",
        "tags": ["lorem", "ipsum"],
        "replicationFactor": 2
	"creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
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

  "200", "List of types."
  "500", "Internal server error."

Success response example::

 {
  entity: {
    types: [
      {
        id: "stream.restful",
        name: "RESTful" 
      },
      {
        id: "stream.elasticsearch",
        name: "Elasticsearch" 
      },
      {
        id: "stream.t-streams",
        name: "T-streams" 
      },
      {
        id: "stream.apache-kafka",
        name: "Apache Kafka" 
      },
      {
        id: "stream.sql-database",
        name: "SQL database" 
      }
    ]
  },
  status-code: 200
 }

Get stream by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/streams/{name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Stream."
  "404", "Stream <stream name> has not been found."
  "500", "Internal server error."

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
      "type": "stream.t-streams"
      "creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
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

  "200", "Stream <stream name> has been deleted."
  "404", "Stream <stream name> has not been found."
  "422", "Cannot delete stream <stream name>. Stream is used in instances."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Stream 'tstr-1' has been deleted."
  }
 }


Error response example::

 {
    "entity": {
        "message": "Stream 'output-stream' has not been found."
    },
    "status-code": 404
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
  "404", "Stream <stream name> has not been found."
  "500", "Internal server error"

Success response example::

 {
  "entity": {
    "instances": [
      "pingstation-output",
      "john"
    ]
  },
  "statusCode": 200
 }

Error response example::

 {
    "entity": {
        "message": "Stream 'output-stream' has not been found."
    },
    "status-code": 404
 }

.. tip:: A full range of error responses can be found at :ref:`Streams_Errors`

CRUD Rest-API for Configurations
-----------------------------------

The range of REST API methods described below allows to create or delete configuration, get the information on the configuration, get the list of configurations existing in the system, get list of domains.

Configuration fields
~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
  :header: "Field", "Format",  "Description", "Requirements"
  :widths: 15, 15, 20, 20

  "name*", "String", "Name of the configuration (key).", "Should be unique and contain digits, lowercase letters, hyphens or periods and start with a letter."
  "value*", "String", "Value of configuration.", ""
  "domain*", "String", "Name of config-domain.", "(see the table below)"

.. note:: `*` - required field.

{config-domain} must be one of the following values:

.. csv-table::  
  :header: "Types"
  :widths: 25 
  
  "configuration.system"
  "configuration.t-streams"
  "configuration.apache-kafka"
  "configuration.elasticsearch"
  "configuration.apache-zookeeper"
  "configuration.sql-database"

Create a new configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format:: 
 
 /v1/config/settings

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "201", "<config-domain> configuration <name> has been created."
  "400", "Cannot create <config-domain> configuration. Errors: <list-of-errors>."
  "500", "Internal server error."


Request json example::

 {
  "name": "crud-rest-host",
  "value": "localhost",
  "domain": "system"
 }


Error response example::


 {
  "status-code": 400,
  "entity": {
    "message": "Cannot create system config setting. Errors: <creation_errors_string>."
  }
 }


Get a configuration by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/config/settings/{config-domain}/{name}

.. csv-table::  Response
  :header: "Status code", "Description"
  :widths: 25, 60

  "200", "Json with requested configuration for specific config domain."
  "400",  "Cannot recognize configuration domain <config-domain>. Domain must be one of the following values: 'configuration.system, configuration.t-streams, configuration.apache-kafka, configuration.elasticsearch, configuration.apache-zookeeper, configuration.sql-database'."
  "404", "<config-domain> сonfiguration <name> has not been found."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "configSetting": {
      "name": "crud-rest-host",
      "value": "localhost",
      "domain": "configuration.system"
      "creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
    }
  }
 }

Error response example::

 {
    "entity": {
        "message": "Cannot recognize configuration domain 'elasticsearch'. Domain must be one of the following values: 'configuration.system, configuration.t-streams, configuration.apache-kafka, configuration.elasticsearch, configuration.apache-zookeeper, configuration.sql-database'."
    },
    "status-code": 400
 }


Get all configurations for specific config domain
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: http://streamjuggler.readthedocs.io/en/develop/SJ_CRUD_REST_API.html#stream-juggler-mesos-framework-rest

 /v1/config/settings/{config-domain}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Json of set of configurations for specific config domain."
  "400", "Cannot recognize configuration domain <config-domain>. Domain must be one of the following values: 'configuration.system, configuration.t-streams, configuration.apache-kafka, configuration.elasticsearch, configuration.apache-zookeeper, configuration.sql-database'."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "configSettings": [
      {
        "name": "crud-rest-host",
        "value": "localhost",
        "domain": {config-domain}
	"creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
     },
     {
       "name": "crud-rest-port",
       "value": "8000",
       "domain": {config-domain}
       "creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
     }
    ]
  }
 }

Error response example::

 {
    "entity": {
        "message": "Cannot recognize configuration domain 'elasticsearch'. Domain must be one of the following values: 'configuration.system, configuration.t-streams, configuration.apache-kafka, configuration.elasticsearch, configuration.apache-zookeeper, configuration.sql-database'."
    },
    "status-code": 400
 }


Delete a configuration by name
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: DELETE

Request format:: 

 /v1/config/settings/{config-domain}/{name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "<config-domain> configuration <name> has been deleted."
  "400", "Cannot recognize configuration domain <config-domain>. Domain must be one of the following values: 'configuration.system, configuration.t-streams, configuration.apache-kafka, configuration.elasticsearch, configuration.apache-zookeeper, configuration.sql-database'."
  "404", "<config-domain> configuration <name> has not been found."
  "500", "Internal server error."

Success response example::

 {
    "entity": {
        "message": "Configuration 'system.crud-rest-host' has been deleted."
    },
    "status-code": 200
 }



Get all configurations
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/config/settings

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Json of set of configurations"
  "500", "Internal server error"

Success response example::

 {
  "status-code": 200,
  "entity": {
    "configSettings": [
      {
          "name": "crud-rest-host",
          "value": "localhost",
          "domain": "configuration.system"
	  "creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
      },
      {
          "name": "crud-rest-port",
          "value": "8000",
          "domain": "configuration.system"
	  "creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
      },
      {
          "name": "session.timeout",
          "value": "7000",
          "domain": "configuration.apache-zookeeper"
	  "creationDate": "Thu Jul 20 08:32:51 NOVT 2017"
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

  "200", "Set of domains."
  "500", "Internal server error."

Success response example::

 {
  entity: {
    domains: [
      {
        id: "configuration.sql-database",
        name: "SQL database" 
      },
      {
        id: "configuration.elasticsearch",
        name: "Elasticsearch" 
      },
      {
        id: "configuration.apache-zookeeper",
        name: "Apache Zookeeper" 
      },
      {
        id: "configuration.system",
        name: "System" 
      },
      {
        id: "configuration.t-streams",
        name: "T-streams" 
      },
      {
        id: "configuration.apache-kafka",
        name: "Apache Kafka" 
      }
    ]
  },
  status-code: 200
 }

.. tip:: A full range of error responses can be found at :ref:`Config_Setting_Errors`

CRUD Rest-API for Custom Files
----------------------------------------

The range of REST API methods described below allows to upload a custom jar or file, download it to your computer, get list of custom jars or files in the system and delete a custom jar or file.

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

  "200", "Custom jar <file_name> has been uploaded."
  "400", "Cannot upload custom jar. Errors: <list-of-errors>. ('Specification.json is not found or invalid.'; 'Custom jar <file_name> already exists.'; 'Cannot upload custom jar <file_name>'. Custom jar with name <name_from_specification> and version <version_from_specification> already exists.')"
  "500", "Internal server error."

Success response example::

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

  "200", "Jar-file for download."
  "404", "Jar <custom-jar-file-name> has not been found."
  "500", "Internal server error."

Error response example::

 {
    "entity": {
        "message": "Jar 'sj-transaction-generator-1.0-SNAPSHOT.jar' has not been found."
    },
    "status-code": 404
 }

Download a custom jar by name and version
""""""""""""""""""""""""""""""""""""""""""""""""

Request method: GET

Request format:: 

 /v1/custom/jars/{custom-jar-name}/{custom-jar-version}/

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Jar-file for download."
  "404", "Jar <custom-jar-name>-<custom-jar-version> has not been found."
  "500", "Internal server error."

Error response example::

 {
    "entity": {
        "message": "Internal server error: Prematurely reached end of stream."
    },
    "status-code": 500
 }


Delete a custom jar by file name
"""""""""""""""""""""""""""""""""""

Request method: DELETE

Request format:: 

 /v1/custom/jars/{custom-jar-file-name}/

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Jar named <custom-jar-file-name> has been deleted."
  "404", "Jar <custom-jar-file-name> has not been found."
  "500", "Internal server error"

Success response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Jar named 'regular-streaming-engine-1.0.jar' has been deleted"
  }
 }
 
Error response example::

 {
    "entity": {
        "message": "Jar 'com.bwsw.batch.stream.engine' has not been found."
    },
    "status-code": 404
 }

Delete a custom jar by name and version (from specification)
"""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""""

Request method: DELETE

Request format:: 

 /v1/custom/jars/{custom-jar-name}/{custom-jar-version}/

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Jar named <custom-jar-name> of the version <custom-jar-version> has been deleted."
  "404", "Jar <custom-jar-name>-<custom-jar-version> has not been found."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Jar named 'com.bwsw.regular.streaming.engine' of the version '0.1' has been deleted"
  }
 }

Error response example::

 {
    "entity": {
        "message": "Jar 'com.bwsw.batch.streaming.engine-2.0' has not been found."
    },
    "status-code": 404
 }

Get list of all uploaded custom jars
"""""""""""""""""""""""""""""""""""""""""""""

Request method: GET

Request format:: 

 /v1/custom/jars

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "List of uploaded custom jars."
  "500", "Internal server error."

Success response example::

 {
  "entity": {
    "customJars": [
      {
        "name": "com.bwsw.fw",
        "version": "1.0",
        "size": "98060032"
	"uploadDate": "Wed Jul 19 13:46:31 NOVT 2017"
      },
      {
        "name": "com.bwsw.tg",
        "version": "1.0",
        "size": "97810217"
	"uploadDate": "Wed Jul 19 13:46:31 NOVT 2017"
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

  "200", "Custom file <custom-jar-file-name> has been uploaded."
  "400", "Request is missing required form field 'file'."
  "409", "Custom file <custom-jar-file-name> already exists."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Custom file <custom-jar-file-name> has been uploaded."
  }
 }

Error response example::

 {
    "entity": {
        "message": "Request is missing required form field 'file'."
    },
    "status-code": 400
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

  "200", "File for download."
  "404", "Custom file <custom-jar-file-name> has not been found."
  "500", "Internal server error."

Success response format: 

File for download is returned.

Error response example::

 {
    "entity": {
        "message": "Custom file 'Custom_jar.jar' has not been found."
    },
    "status-code": 404
 }

Delete a custom file
""""""""""""""""""""""""""""""""""""

Request method: DELETE

Request format:: 

 /v1/custom/files/{custom-jar-file-name}

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Custom file <custom-jar-file-name> has been deleted."
  "404", "Custom file <custom-jar-file-name> has not been found."
  "500", "Internal server error"

Success response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Custom file 'text.txt' has been deleted."
  }
 }

Error response example::

 {
    "entity": {
        "message": "Custom file 'customfile.txt' has not been found."
    },
    "status-code": 404
 }

Get list of all uploaded custom files
""""""""""""""""""""""""""""""""""""""""

Request method: GET

Request format:: 

 /v1/custom/files

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "List of uploaded custom files."
  "500", "Internal server error."

Success response example::

 {
  "entity": {
    "customFiles": [
      {
        "name": "GeoIPASNum.dat",
        "description": "",
        "uploadDate": "Mon Jul 04 10:42:03 NOVT 2016",
        "size": "46850"
      },
      {
        "name": "GeoIPASNumv6.dat",
        "description": "",
        "uploadDate": "Mon Jul 04 10:42:58 NOVT 2016",
        "size": "52168"
      }
    ]
  },
  "status-code": 200
 }

.. _Modules_REST_API:
CRUD Rest-API for Modules 
------------------------------

This is the CRUD Rest-API for modules uploaded as jar files, instantiated and running modules as well as for custom jar files.

.. csv-table::  Module types
 :header: "Types"
 :widths: 25 
  
 "input-streaming"
 "regular-streaming"
 "batch-streaming"
 "output-streaming"

.. csv-table::  **Specification fields**
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60

  "name*", "String", "The unique name for a module."
  "description", "String", "The description for a module."
  "version*", "String", "The module version."
  "author", "String", "The module author."
  "license", "String", "The software license type for a module."
  "inputs*", "IOstream", "The specification for the inputs of a module."
  "outputs*", "IOstream", "The specification for the outputs of a module."
  "module-type*", "String", "The type of a module. One of [input-streaming, output-streaming, batch-streaming, regular-streaming]."
  "engine-name*", "String", "The name of the computing core of a module."
  "engine-version*", "String", "The version of the computing core of a module."
  "validator-class*", "String", "The absolute path to class that is responsible for a validation of launch options."
  "executor-class*", "String", "The absolute path to class that is responsible for a running of module."
  "batch-collector-class**", "String", "The absolute path to class that is responsible for a batch collecting of batch-streaming module."

IOstream for inputs and outputs has the following structure:

.. csv-table:: **IOstream fields**
  :header: "Field", "Format",  "Description"
  :widths: 20, 20, 60

  "cardinality*", "Array[Int]", "The boundary of interval in that a number of inputs can change. Must contain 2 items."
  "types*", "Array[String]", "The enumeration of types of inputs. Can contain only [stream.t-streams, stream.apache-kafka, stream.elasticsearch, stream.sql-database, stream.restful, input]"

.. note:: `*` - required field, `**` - required for batch-streaming field

Upload a module
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

.. csv-table:: Response
  :header: "Status code",  "Description"
  :widths: 10, 60

  "200", "Jar file <file_name> of module has been uploaded."
  "400", "1. Cannot upload jar file <file_name> of module. Errors: file <file_name> does not have the .jar extension. 
  2. Cannot upload jar file <file_name> of module. Errors: module <module-type>-<module-name>-<module-version> already exists.
  3. Cannot upload jar file <file_name> of module. Errors: file <file_name> already exists.
  4. Other errors."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Jar file 'regular-module.jar' of module has been uploaded."
  }
 }

Error response example::

 {
    "entity": {
        "message": "Cannot upload jar file 'regular-module.jar' of module. Errors: file 'regular-module.jar' already exists."
    },
    "status-code": 400
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


.. csv-table:: Response
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

.. csv-table:: Response
  :header: "Status code",  "Description"
  :widths: 10, 60

  "200", "Module {module-name} for type {module-type} has been deleted"
  "404", "1. Module '<module_type>-<module_name>-<module_version>' has not been found.
  2. Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage."
  "422", "1. It's impossible to delete module '<module_type>-<module_name>-<module_version>'. Module has instances.
  2. Cannot delete file '<module-filename>'"
  "500", "Internal server error"

Success response example::

 {
  "status-code": 200,
  "entity": {
    "message": "Module 'regular-streaming-com.bwsw.sj.stub-1.0' has been deleted."
  }
 }


Error response example::

 {
    "entity": {
        "message": "Module 'regular-streaming-RegularModule-1.0' has not been found."
    },
    "status-code": 404
 }


Get list of all uploaded modules
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules

.. csv-table:: Response
  :header: "Status code",  "Description"
  :widths: 15, 60

  "200", "List of uploaded modules"
  "500","Internal server error"

Success response example::

 {
  "status-code": 200,
  "entity": {
    "modules": [
      {
        "moduleType": "regular-streaming",
        "moduleName": "com.bwsw.sj.stub",
        "moduleVersion": "0.1",
        "size": "68954210"
	"uploadDate": "Wed Jul 19 13:46:31 NOVT 2017"
      },
      {
        "moduleType": "batch-streaming",
        "moduleName": "com.bwsw.sj.stub-win",
        "moduleVersion": "0.1",
        "size": "69258954"
	"uploadDate": "Wed Jul 19 13:46:31 NOVT 2017"
      }
    ]
  }
 }


Get list of types of modules
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/_types

.. csv-table::  Response
  :header: "Status code",  "Description"
  :widths: 15, 60

  "200", "List of types"
  "500", "Internal server error"

Success response example::

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


Get list of all uploaded modules for such type
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/{module-type}

.. csv-table:: **Response**
  :header: "Status code",  "Description"
  :widths: 15, 60

  "200", "Uploaded modules for type <module-type> + <list-modules-for-type>."
  "400", "Module type <module-type> does not exist."
  "500", "Internal server error."

Success response example::

 {
  "status-code": 200,
  "entity": {
    "modules": [
      {
        "moduleType": "regular-streaming",
        "moduleName": "com.bwsw.sj.stub",
        "moduleVersion": "0.1",
        "size": 106959926
	"uploadDate": "Wed Jul 19 13:46:31 NOVT 2017"
      }
    ]
  }
 }

Error response example::

 {
    "entity": {
        "message": "Module type 'output-stream' does not exist."
    },
    "status-code": 400
 }

Get specification for uploaded module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/specification

.. csv-table::  **Response**
  :header: "Status code",  "Description"
  :widths: 15, 60

  "200", "Specification json (see :ref:`Json_schema`)."
  "404", "1. Module '<module_type>-<module_name>-<module_version>' has not been found.
  2. Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage."
  "500", "Internal server error (including erorrs related to incorrect module type or nonexistent module)."

Success response example::

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
          "stream.t-streams"
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

Error response example::

 {
    "entity": {
        "message": "Module 'regular-streaming-RegularModule-1.0' has not been found."
    },
    "status-code": 400
 }

.. tip:: A full range of error responses can be found at :ref:`Modules_Errors`

.. _REST_API_Instance:

CRUD Rest-API for Instances
-----------------------------------

The range of REST API methods described below allows to create an instance of a module, get the list of existing instances, get the settings of a specific instance, start and stop an instance and get the instance tasks information as well as delete an instance of a specific module. 


.. _REST_API_Instance_Create:

Create an instance of a module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: POST

Request format:: 

 /v1/modules/{module-type}/{module-name}/{module-version}/instance/

.. note:: The name of an input stream should contain the  "/split" suffix (if stream's partitions should be distributed between the tasks) or "/full" (if each task should process all partitions of the stream). The stream has a 'split' mode as default. (see `Execution plan <Execution plan>`_)

Instance fields
"""""""""""""""""""

**General instance fields**

.. csv-table:: 
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

**Input-streaming instance fields**

.. csv-table:: 
  :header: "Field name", "Format",  "Description", "Example"
  :widths: 15, 10, 60, 20

  "checkpointMode*", "String", "Value must be time-interval or every-nth",  "every-nth" 
  "checkpointInterval*", "Int ", "Interval for creating checkpoint",  "100 "
  "outputs*", "List[String] ", "Names of output streams (must be stream.t-stream only)", "[s3, s4] "
  "duplicateCheck",  "Boolean", "The flag points  if every envelope (an envelope key) has to be checked on duplication or not. (false by default) **Note**: You can indicate the 'duplicateCheck' field in the instance to set up a default policy for message checking on duplication. Use the 'InputEnvelope' flag in the :ref:`input-module`  for special cases* ", "true "
  "lookupHistory*", "Int", "How long an unique key of an envelope will stay in a queue for checking envelopes on duplication (in seconds). If it is not 0, entries that are older than this time and not updated for this time are evicted automatically accordingly to an eviction-policy. Valid values are integers between 0 and Integer.MAX VALUE. Default value is 0, which means infinite.", "1000"
  "queueMaxSize*", "Int", "Maximum size of the queue that contains the unique keys of envelopes. When maximum size is reached, the queue is evicted based on the policy defined at default-eviction-policy (should be greater than 271)", "500"
  "defaultEvictionPolicy", "String", "Must be only 'LRU' (Least Recently Used), 'LFU' (Least Frequently Used) or 'NONE' (NONE by default)", "LRU" 
  "evictionPolicy", "String",  "An eviction policy of duplicates for an incoming envelope. Must be only 'fix-time' (default) or 'expanded-time'. If it is 'fix-time', a key of the envelope will be contained only {lookup-history} seconds. The 'expanded-time' option means, if a duplicate of the envelope appears, the key presence time will be updated", "fix-time" 
  "backupCount", "Int", "The number of backup copies you want to have (0 by default, maximum 6). Sync backup operations have a blocking cost which may lead to latency issues. You can skip this field if you do not want your entries to be backed up, e.g. if performance is more important than backing up.",  2 
  "asyncBackupCount", "Int", "Flag points an every envelope (an envelope key) has to be checked on duplication or not (0 by default). The backup operations are performed at some point in time (non-blocking operation). You can skip this field if you do not want your entries to be backed up, e.g. if performance is more important than backing up.", 3 

**Regular-streaming instance fields**

.. csv-table::  
  :header: "Field name", "Format",  "Description", "Example"
  :widths: 15, 10, 60, 20

  "checkpointMode*", "String", "Value must be 'time-interval' or 'every-nth'", "every-nth" 
  "checkpointInterval*", "Int", "Interval for creating checkpoint", 100 
  "inputs*", "List[String]", "Names of input streams. Name format must be <stream-name>/<'full' or 'split'> ('split' by default). Stream must exist in database (must be stream.t-streams or stream.apache-kafka)", "[s1/full, s2/split]" 
  "outputs*", "List[String]", "Names of output streams (must be stream.t-streams only)", "[s3, s4]" 
  "startFrom", "String or Datetime", "Value must be 'newest', 'oldest' or datetime. If instance have kafka input streams, then 'start-from' must be only 'oldest' or 'newest' (newest by default)", "newest" 
  "stateManagement", "String", "Must be 'ram' or 'none' (none by default)", "ram" 
  "stateFullCheckpoint", "Int", "Interval for full checkpoint (100 by default)", "5"
  "eventWaitTime", "Long", "Idle timeout, when not messages (1000 by default)", 10000

**Batch-streaming instance fields**

.. csv-table:: 
  :header: "Field name", "Format",  "Description", "Example"
  :widths: 15, 10, 60, 20

  "outputs*", "List[String]", "Names of output streams (must be stream.t-stream only)", "[s3, s4]"
  "window", "Int", "Count of batches that will be contained into a window (1 by default). Must be greater than zero",  3 
  "slidingInterval", "Int", "The interval at which a window will be shifted (сount of batches that will be removed from the window after its processing). Must be greater than zero and less or equal than window (1 by default)", 3
  "inputs*", "String", "Names of input streams. Name format must be <stream-name>/<'full' or 'split'> ('split' by default).
 Stream must exist in database (must be stream.t-streams or stream.apache-kafka)", "[s1/full]"
  "startFrom", "String or Datetime", "Value must be 'newest', 'oldest' or datetime. If instance have kafka input streams, then 'start-from' must be only 'oldest' or 'newest' (newest by default)", "newest" 
  "stateManagement", "String", "Must be 'ram' or 'none' (none by default)",  "ram" 
  "stateFullCheckpoint", "Int", "Interval for full checkpoint (100 by default)", 5 
  "eventWaitTime", "Long", "Idle timeout, when not messages (1000 by default)", 10000 

**Output-streaming instance fields**

.. csv-table:: 
  :header: "Field name", "Format",  "Description", "Example"
  :widths: 15, 10, 60, 20

  "checkpointMode*", "String",  "Value must be 'time-interval'", "time-interval" 
  "checkpointInterval*", "Int", "Interval for creating checkpoint", 100 
  "input*", "String", "Names of input stream. Must be only 'stream.t-streams' type. Stream for this type of module is 'split' only.  Stream must be exists in database.", "s1" 
  "output*", "String", "Names of output stream (must be stream.elasticsearch, stream.sql-database or stream.restful)", "es1" 
  "startFrom", "String or Datetime", "Value must be 'newest', 'oldest' or datetime (newest by default)", "newest" 


.. note:: `*` - required fields.

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



**Request** json example for creating a batch-streaming instance::

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


**Response**

.. csv-table:: 
  :header: "Status code",  "Description"
  :widths: 10, 60
  
  "201", "Instance <instance_name> for module <module_type>-<module_name>-<module_version> has been created."
  "400", "1. Cannot create instance of module. The instance parameter 'options' haven't passed validation, which is declared in a method, called 'validate'. This method is owned by a validator class that implements StreamingValidator interface. Errors: {list-of-errors}.
  2. Cannot create instance of module. Errors: {list-of-errors}."
  "404", "1. Module <module_type>-<module_name>-<module_version> has not been found.
  2. Jar of module <module_type>-<module_name>-<module_version> has not been found in the storage."
  "500", "Internal server error (including erorrs related to incorrect module type or nonexistent module)."


Success response json example of a created instance::

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
 
Error response example::

 {
    "entity": {
        "message": "Module 'input-streaming-Instance1-2.0' has not been found."
    },
    "status-code": 404
 }

Instance statuses
"""""""""""""""""""

Instance may have one of the following statuses:

 * ready - a newly created instance and not started yet;
 * starting - a recently launched instance but not started yet (right after the "Start" button is pushed);
 * started - the launched instance started to work;
 * stopping - a started instance in the process of stopping (right after the "Stop" button is pushed);
 * stopped - an instance that has been stopped;
 * deleting - an instance in the process of deleting (right after the "Delete" button is pressed);
 * failed - an instance that has been launched but in view of some errors is not started;
 * error - an error is detected at stopping or deleting an instance.

.. figure:: _static/InstanceStatuses.png

Execution plan
"""""""""""""""""""

A created instance contains an execution plan that is provided by the system. 

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

Each task has a name, host and port. A host and a port define an address to which the data should be sent for the input module to process them.

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

A created instance contains a stage that is provided by the system.

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



Get all instances
~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 
 
 /v1/modules/instances

.. csv-table:: Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Json set of instances (in short format)."
  "500", "Internal server error."

Success response json example::

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

Success response json example::

 {
  "status-code": 200,
  "entity": {
    "instances": [
      "test-instance",
      "abc"
    ]
  }
 }

Error response example::

 {
    "entity": {
        "message": "Module 'output-streaming-OutputModule-1.0' has not been found."
    },
    "status-code": 400
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

Success response json example::

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

Error response example::

 {
    "entity": {
        "message": "Module 'output-streaming-OutputModule-1.0' has not been found."
    },
    "status-code": 400
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


Error response example::

 {
    "entity": {
        "message": "Instance 'batch-streaming' has not been found."
    },
    "status-code": 404
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

Success response example::

 {
  "status-code" : 200,
  "entity" : {
     "message" : "Instance '<instance_name>' is being launched."
  }
 }


Error response example::

 {
    "entity": {
        "message": "Cannot start of instance. Instance has already launched."
    },
    "status-code": 422
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

Success response example::

 {
  "status-code": 200,
  "entity": {
    "tasks": [
      {
        "state": "TASK_RUNNING",
        "directories": [
          {
            "name": "Mon Dec 05 11:33:47 NOVT 2016",
            "path": "http://stream-juggler.z1.netpoint-dc.com:5050/#/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/browse?path=/var/lib/mesos/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/frameworks/c69ce526-c420-44f4-a401-                       6b566b1a0823-0003/executors/pingstation-process-task0/runs/d9748d7a-3d0e-4bb6-88eb-3a3340d133d8" 
          },
          {
            "name": "Mon Dec 05 11:56:47 NOVT 2016",
            "path": "http://stream-juggler.z1.netpoint-dc.com:5050/#/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/browse?path=/var/lib/mesos/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/frameworks/c69ce526-c420-44f4-a401-                       6b566b1a0823-0003/executors/pingstation-process-task0/runs/8a62f2a4-6f3c-412f-9d17-4f63e9052868" 
          }
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


Error response example::

 {
    "entity": {
        "message": "Cannot get instance framework tasks info. The instance framework has not been launched."
    },
    "status-code": 422
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

.. note:: An instance with the "started" status only can be stopped. 

When the instance stops, the framework suspends on Mesos.


Success response example::

 {
  "status-code" : 200,
  "entity" : {
     "message" : "Instance '<instance_name>' is being stopped."
  }
 }

Error response example::

 {
    "entity": {
        "message": "Cannot stop instance. Instance has not been started."
    },
    "status-code": 422
 }

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

.. note:: This process includes destruction of the framework on Mesos.

Success response example::


 {
  "status-code" : 200,
  "entity" : {
     "message" : "Instance 'stub-instance-1' has been deleted."
  }
 }

Error response example::

 {
    "entity": {
        "message": "Instance 'output instance' has not been found."
    },
    "status-code": 404
 }

Stream Juggler Mesos Framework Rest
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Request method: GET

Request format:: 

 http://{rest-address}

.. csv-table:: Response
  :header: "Status code",  "Description"
  :widths: 25, 60

  "200", "Json set of instances (in short format)."
  "500", "Internal server error"

Success response json example::

 entity: {
 "tasks": [
 {
 "state": "TASK_RUNNING",
 "directories": [
 "http://stream-juggler.z1.netpoint-dc.com:5050/#/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/browse?path=/var/lib/mesos/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/frameworks/c69ce526-c420-44f4-a401-6b566b1a0823-0003/executors/pingstation- process-task0/runs/d9748d7a-3d0e-4bb6-88eb-3a3340d133d8",
 "http://stream-juggler.z1.netpoint-dc.com:5050/#/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/browse?path=/var/lib/mesos/slaves/3599865a-47b1-4a17-9381-b708d42eb0fc-S0/frameworks/c69ce526-c420-44f4-a401-6b566b1a0823-0003/executors/pingstation-process-task0/runs/8a62f2a4-6f3c-412f-9d17-4f63e9052868" 
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

.. tip:: A full range of error responses can be found at :ref:`Instances_Errors`
