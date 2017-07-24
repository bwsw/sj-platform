.. _API_Error_Responses:

API Error Responses
=========================

At this page all possible API error responses are described. It will allow you to discern the causes of error responses and quickly correct them for smoothing the system workflow. 

.. Contents::

.. _Incorrect_Json_Api_Responses:

Incorrect Json API Responses
--------------------------------------

Types of error responses at incorrect JSON uploading: 

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 "1. Cannot parse json: '<json-part>'", "Incorrect JSON format. An error in <json-part>."
 "2. Json deserialization error: 'Empty JSON'", "No JSON in the entity."
 "3. Json contains unrecognized property '<property>'", "JSON contains the <property> field that is not described in the object. The error does not occur, if unknown fields are ignored."
 "4. Json contains incorrect value in property '<property>'", "Incorrect <property> field type."

.. _Provider_Errors:

Providers API Error Responses
------------------------------------------

The responses description for CRUD REST-API for Providers methods are presented below.

Create Provider
~~~~~~~~~~~~~~~~~~~~

*'Cannot create provider' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60  

 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "Provider with name '<provider_name>' already exists.", "All fields are filled in following the requirements, but the provider with this name already exists in the system. 
 
 In this case it does not matter that all other fields differ from the fields of the existing provider. "
 "Provider has incorrect name: '<provider_name>'. A name of the provider must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are filled following the requirements except the 'Name' field."
 "Unknown type '<provider_type>' provided. Must be one of: [provider.elasticsearch, provider.apache-kafka, provider.apache-zookeeper, provider.sql-database, provider.restful].", "All fields are filled following the requirements except the 'Type' field. This error is not possible in UI as the Type field offers the dropdown list of types."
 "Wrong host provided: '<provider_host>'.", "All fields are filled following the requirements except the 'Hosts' field."
 "Host cannot contain any URI path ('<uri_path>').", "All fields are completed following the requirements except the 'Host' field that contains the id of some resource (defining the file location, e.g. an image is stored at the desk: /home/smith_j/desk/imgpsh_fullsize.jpg)."
 "Host '<provider_host>' must contain port", "All fields are filled following the requirements except the 'Hosts' field where no port is defined in one or several elements."
 "'Name' attribute is required.", "The Name field is not completed."
 "'Type' attribute is required.", "The Type field is not completed."
 "'Hosts' attribute is required.", "The Hosts field is not completed."
 "'Hosts' must contain at least one host.", "The Hosts field is empty."
 "Configuration 'jdbc.driver.<driver-name>' is required.", "Configuration jdbc.driver.<driver-name> is not completed (required for sql-database type only)."
 "Configuration 'jdbc.driver.<driver-name>.class' is required.", "Configuration jdbc.driver.<driver-name>.class  is not completed (required for sql-database type only)."
 "Configuration 'jdbc.driver.<driver-name>.prefix' is required.", "Configuration jdbc.driver.<driver-name>.prefix  is not completed (required for sql-database type only)."
 "Prefix '<prefix>' in configuration 'jdbc.driver.<driver-name>.prefix' is incorrect.", "Incorrect jdbc.driver.<driver-name>.prefix value. The following options are allowed: jdbc:mysql, jdbc:postgresql, jdbc:oracle:thin (required for sql-database type only)."

Test Connection To Provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 "Host checking for provider type '<provider_type>' is not implemented.", "A provider of incorrect type have been posted. This error is not possible in UI as the Type field offers the dropdown list of types."
 "Cannot gain an access to Zookeeper on '<provider_host>'.", "Cannot connect Zookeeper to the specified address."
 "Can not establish connection to Kafka on '<provider_host>'.", "Cannot connect Kafka to the specified address."
 "Can not establish connection to ElasticSearch on '<provider_host>'.", "Cannot connect Elasticsearch to the specified address."
 "Can not establish connection to REST on '<provider_host>'.", "Cannot connect REST to the specified address."


Provider Deleting
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 "Provider '<provider_name>' has not been found.", "The provider is not existing. This error is not possible in UI as the Type field offers the dropdown list of types."
 "Cannot delete provider '<provider_name>'. Provider is used in services.", "The provider is used in one (or several) services, so it cannot be deleted. Firstly, all the services using the provider should be deleted, and then the provider can be deleted."


Get Provider by Name
~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 "Provider '<provider_name>' has not been found.", "The provider does not exist in the system."

Get services related to a provider (by provider name)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 "Provider '<provider_name>' has not been found.", "The provider does not exist in the system."


.. _Services_Errors:

Services API Error Responses
------------------------------------

The responses description for CRUD REST-API for Services methods are presented below.

Create Service
~~~~~~~~~~~~~~~~~~~
Possible error responses for **Elasticsearch** service creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<service_type>' of service provided. Must be one of: [service.t-streams, service.apache-kafka, service.elasticsearch, service.apache-zookeeper, service.sql-database].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the 'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'ES' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (a provider of incorrect type is specified)."
 "'Index' attribute is required.", "The Keyspace field is not completed."
 "Service has incorrect 'index': '<service_index>'. Name must contain digits, lowercase letters or underscore. First symbol must be a letter.", "All fields are completed following the requirements except the 'Index' field."


Possible error responses for **Apache Kafka** service creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements buta service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<service_type>' of service provided. Must be one of: [service.t-streams, service.apache-kafka, service.elasticsearch, service.apache-zookeeper, service.sql-database].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the 'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'kafka' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (a provider of incorrect type is specified."
 "'zkNamespace' attribute is required.", "The zkNamespace field is not completed."
 "Service has incorrect 'zkNamespace': '<service_zk-namespace>'. A name must contain digits, lowercase letters or underscore. The first symbol must be a letter.", "All fields are completed following the requirements except the 'zkNamespace' field."
 "'zkProvider' attribute is required.", "The zkProvider field is not completed."
 "Zookeeper provider '<service_zk-provider>' does not exist.", "All fields are completed following the requirements except the 'zkProvider' field (the zk provider specified in the service does not exist.)"
 "'zkProvider' must be of type: 'zookeeper' ('<service_zk-provider_type>' is given instead).", "All fields are completed following the requirements except the 'zkProvider' field (the specified provider is not of a zookeeper type)."


Possible error responses for **T-streams** service creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<service_type>' of service provided. Must be one of: [service.t-streams, service.apache-kafka, service.elasticsearch, service.apache-zookeeper, service.sql-database].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the 'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'zookeeper' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (the specified provider is not of a zookeeper type)."
 "'Prefix' attribute is required.", "The Prefix field is not completed."
 "Service has incorrect 'prefix': '<service_prefix>'. Prefix must be a valid znode path.", "All fields are completed following the requirements except the 'Prefix' field."
 "'Token' attribute is required.", "The Token field is not completed."
 "Service has incorrect 'token': '<service_token>'. Token must contain no more than 32 symbols", "All fields are completed following the requirements except the 'Token' field."


Possible error responses for **Apache Zookeeper** service creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 
 
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type is not completed."
 "Unknown type '<service_type>' of service  provided. Must be one of: [service.t-streams, service.apache-kafka, service.elasticsearch, service.apache-zookeeper, service.sql-database].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the 'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'provider.apache-zookeeper' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (the specified provider is of a wrong type)."
 "'Namespace' attribute is required.", "The Namespace field is not completed."
 "Service has incorrect 'namespace': '<service_namespace>'. A name must contain digits, lowercase letters or underscore. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Namespace' field."


Possible error responses for **SQL database** service creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 
 
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type is not completed."
 "Unknown type '<service_type>' of service provided. Must be one of: [service.t-streams, service.apache-kafka, service.elasticsearch, service.apache-zookeeper, service.sql-database].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the 'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'provider.sql-database' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (the specified provider is of a wrong type)."
 "'Database' attribute is required.", "The Database field is not completed."
 "'Driver' attribute is required.", "The Driver field is not completed."
 "Custom file '<driver-file>' is required. ", "There is no JDBC-driver file <driver-file>."
 "Database '<database_name>' does not exist.", "The Database field points to the database that does not exist."
 "Can not create client: '<reason>'.", "The client is not created for the reason that is specified after colon."


Possible error responses for **RESTful** service creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 
 
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<service_type>' of service provided. Must be one of: [service.t-streams, service.apache-kafka, service.elasticsearch, service.apache-zookeeper, service.sql-database].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the 'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'REST' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (the specified provider is of a wrong type)."
 "Attribute 'basePath'  must starts with '/'. ", "The BasePath field contains an empty string or does not start with the '/' symbol."
 "Attribute 'httpVersion' must be one of: [1.0, 1.1, 2].", "Incorrect HTTP version is specified."

Delete Services
~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 "Service '<service_name>' has not been found.", "The service does not exist in the system. This error is not possible in UI as it offers a dropdown list."
 "Cannot delete service '<service_name>'. Service is used in streams.", "The service is used in one (or several) streams, so it cannot be deleted. Firstly, all the streams using the service should be deleted, and then the service will be available for deleting."
 "Cannot delete service '<service_name>'. Service is used in instances.", "The service is used in one (or several) instances, so it cannot be deleted. Firstly, all the instances using the service should be deleted, and then the service will be available for deleting."

Get a Service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 "Service '<service_name>' has not been found.", "The service does not exist in the system."

Get streams and instances related to a service (by service name)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 "Service '<service_name>' has not been found.", "The service does not exist in the system."

.. _Streams_Errors:

Streams API Error Responses
--------------------------------
The responses description for CRUD REST-API for Streams methods are presented below.

Create Stream
~~~~~~~~~~~~~~~~~~~~~~~
Possible error responses for **T-streams** streams creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60


 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Stream with name '<stream_name>' already exists", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Stream has incorrect name: '<stream_name>'. A name of stream must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<stream_type>' of stream provided. Must be one of: [stream.t-streams, stream.apache-kafka, streams.sql-database, streams.elasticsearch, streams.restful].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Service' attribute is required.", "The Service field is not completed."
 "Service '<stream_service>' does not exist.", "All fields are completed following the requirements except the 'Service' field (the specified service does not exist)."
 "Service for '<stream_type>' stream must be of type: 'service.t-streams' ('<stream_service_type>' is given instead).", "All fields are completed following the requirements except the 'Service' field (the specified service is of an incorrect type)."
 "'Partitions' attribute is required. 'Partitions' must be a positive integer.", "All fields are completed following the requirements except the 'Partitions' field."
 "'Partitions' attribute is required.", "The Partitions field is not completed."


Possible error responses for **Apache Kafka** streams creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60


 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Stream with name '<stream_name>' already exists", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Stream has incorrect name: '<stream_name>'. A name of stream must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<stream_type>' of stream provided. Must be one of: [stream.t-streams, stream.apache-kafka, streams.sql-database, streams.elasticsearch, streams.restful].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Service' attribute is required.", "The Service field is not completed."
 "Service '<stream_service>' does not exist.", "All fields are completed following the requirements except the 'Service' field (the specified service does not exist)."
 "Service for '<stream_type>' stream must be of type: 'service.apache-kafka' ('<stream_service_type>' is given instead).", "All fields are completed following the requirements except the 'Service' field (the specified service is of an incorrect type)."
 "'Partitions' must be a positive integer.", "All fields are completed following the requirements except the 'Partitions' field."
 "'replicationFactor' must be a positive integer.", "All fields are completed following the requirements except the 'replicationFactor' field."
 "'Partitions' attribute is required.'Partitions' must be a positive integer.", "The Partitions field is not completed."
 "'replicationFactor' attribute is required. 'replicationFactor' must be a positive integer.", "The replicationFactor field is not completed."

Possible error responses for **Elasticsearch** streams creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60


 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Stream with name '<stream_name>' already exists", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Stream has incorrect name: '<stream_name>'. A name of stream must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<stream_type>' of stream provided. Must be one of: [stream.t-streams, stream.apache-kafka, streams.sql-database, streams.elasticsearch, streams.restful].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Service' attribute is required.", "The Service field is not completed."
 "Service '<stream_service>' does not exist.", "All fields are completed following the requirements except the 'Service' field (the specified service does not exist)."
 "Service for '<stream_type>' stream must be of type: 'service.elasticsearch' ('<stream_service_type>' is given instead).", "All fields are completed following the requirements except the 'Service' field (the specified service is of an incorrect type)."

Possible error responses for **SQL-database** streams creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60


 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Stream with name '<stream_name>' already exists", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Stream has incorrect name: '<stream_name>'. A name of stream must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<stream_type>' of stream provided. Must be one of: [stream.t-streams, stream.apache-kafka, streams.sql-database, streams.elasticsearch, streams.restful].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Service' attribute is required.", "The Service field is not completed."
 "Service '<stream_service>' does not exist.", "All fields are completed following the requirements except the 'Service' field (the specified service does not exist)."
 "Service for '<stream_type>' stream must be of type: 'service.sql-database' ('<stream_service_type>' is given instead).", "All fields are completed following the requirements except the 'Service' field (the specified service is of an incorrect type)."

Possible error responses for **RESTful** streams creation:

*'Cannot create service' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60


 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Stream with name '<stream_name>' already exists", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Stream has incorrect name: '<stream_name>'. A name of stream must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<stream_type>' of stream provided. Must be one of: [stream.t-streams, stream.apache-kafka, streams.sql-database, streams.elasticsearch, streams.restful].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Service' attribute is required.", "The Service field is not completed."
 "Service '<stream_service>' does not exist.", "All fields are completed following the requirements except the 'Service' field (the specified service does not exist)."
 "Service for '<stream_type>' stream must be of type: 'service.restful' ('<stream_service_type>' is given instead).", "All fields are completed following the requirements except the 'Service' field (the specified service is of an incorrect type)."

Delete Stream
~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 "Stream '<stream_name>' has not been found.", "The stream does not exist in the system. This error is not possible in UI as it offers a dropdown list."
 "Cannot delete stream '<stream_name>'. Stream is used in instances.", "The stream is used in one (or several) instances, so it cannot be deleted. Firstly, all the instances using the stream should be deleted, and then the stream will be available for deleting."

Get Stream
~~~~~~~~~~~~~~~~~~~~~~~
.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 "Stream '<stream_name>' has not been found.", "The stream does not exist in the system. "

Get Instances Related to a Stream
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 "Stream '<stream_name>' has not been found.", "The stream does not exist in the system. "

.. _Modules_Errors:

Modules API Error Responses
-------------------------------------

The responses description for CRUD REST-API for Module methods are presented below.

Module is a file with .jar extention that contains module classes and specification (see :ref:`Modules_REST_API`).

Upload a module
~~~~~~~~~~~~~~~~~~~

Possible error responces at **module uploading**:

*'Cannot upload jar file '<file_name>' of module' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "Module '<module_type>-<module_name>-<module_version>' already exists.", "The file being uploaded has correct extention and specification, but the module with the specified name, version and type already exists in the system (field names in specification: 'name', 'version', 'module-type')."
 "File '<file_name>' does not have the .jar extension.", "The file being uploaded has an extention deffernet from .jar, thus the file can not be uploaded as a module (file name has to have '.jar' suffix)."
 "File '<file_name>' not a jar archive.", "The uploaded file with .jar extention is not a jar-archive."
 "File '<file_name>' already exists.", "The file being uploaded has correct extention and specification, but a file with the same name already exists (file name should be changed)."
 "'engine-name' and 'engine-version' attributes in specification.json is invalid.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but the specified engine (that starts a module) does not exist, or more precisely there is no information that such an engine exists, but the engine does exist, so it is probably a bug that should be fixed."
 "'<attribute>' attribute in specification.json is required.", "A required <attribute> parameter is missed in the specification."
 "'module-type' attribute in specification.json must be one of [batch-streaming, regular-streaming, output-streaming, input-streaming].", "Incorrect module-type value in the specification."
 
*Input-streaming specification*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Both of cardinality of 'inputs' in the specification.json must to be equal 0.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but both values of the 'input.cardinality' field in the specification should be equal to 0."
 "'inputs' attribute in specification.json must contain only one string: 'input'.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but the 'input.types' field value in the specificaation should be equal to 'input'."
 "Cardinality of 'outputs' in the specification.json has to be an interval with the left bound that is greater than zero. ", "The file being uploaded has correct extention, the specification corresponds to the json schema, but the 'output.cardinality' field is completed incorrectly in the specification. Either the first value is less than 1, or the second value is less than the first one."
 "'outputs' attribute in the specification.json must have the streams of t-stream and kafka type.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but the 'output.types' field in the specification should contain the only value equal to 'stream.t-streams'."

*Regular-streaming specification* and *batch-streaming specification*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Cardinality of 'inputs' in the specification.json has to be an interval with the left bound that is greater than zero.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but the 'input.cardinality' field in the specification is incorrect. Either the first value is less than 1, or the second value is less than the first one."
 "'inputs' attribute in the specification.json must have the streams of t-stream and kafka type.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but in the 'input.types' field of the specification the following values are allowed: 'stream.t-streams', 'stream.apache-kafka'."
 "Cardinality of 'outputs' in the specification.json has to be an interval with the left bound that is greater than zero.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but the 'output.cardinality' field of the specification is incorrect. Either the first value is less than 1, or the second value is less than the first one."
 "'outputs' attribute in the specification.json must have the streams of t-stream and kafka type.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but in the 'output.types' field of the specification the following values are allowed: 'stream.t-streams', 'stream.apache-kafka'."

*Output-streaming specification*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Both of cardinality of 'inputs' in the specification.json must to be equal to 1.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but in the 'input.cardinality' field of the specification both values should be grater than 1."
 "'inputs' attribute in the specification.json must have the streams of t-stream and kafka type.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but in the 'input.types' field of the specification the following values are allowed: 'stream.t-streams', 'stream.apache-kafka'."
 "Both of cardinality of 'outputs' in the specification.json must to be equal 1.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but in the 'output.cardinality' field of the specification both values should be equal to 1."
 "'outputs' attribute in the specification.json must have the streams of Elasticsearch, SQL-daatbase or RESTful type.", "The file being uploaded has correct extention, the specification corresponds to the json schema, but in the 'output.types' field of the specification the following values are allowed: 'stream.elasticsearch', 'stream.sql-database', 'stream.restful'."
 "Class '<class_name>' indicated in '<param_class_name>' attribute of the specification.json isn't found.", " In jar file no <class_name> class specified in the <param_class_name> field is found. It is required for validator-class, executor-class, batch-collector-class fields only."
 "'validator-class' attribute in specification.json is invalid - a '<class_name>' should implement 'com.bwsw.sj.common.engine.StreamingValidator'", "The <class_name> class specified in the validator-class field should carry out the com.bwsw.sj.common.engine.StreamingValidator class."

Get list of modules for such type
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Module type '<module-type>' does not exist.", "Incorrect module type is specified."

Get specification for uploaded module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are available: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just methadata record can be missed for some reasons."
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."

Download jar of uploaded module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are available: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just methadata record can be missed for some reasons." 
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."

Delete a module
~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are available: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just a methadata record can be missed for some reasons." 
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."
 "It's impossible to delete module '<module_type>-<module_name>-<module_version>'. Module has instances.", "While a module has at least one instance it can not be deleted. Firstly, all module's instances should be deleted, then the module will become available for deleting."

.. _Instances_Errors:

Instances API Error Responses
--------------------------------------

The responses description for CRUD REST-API for Instances methods are presented below.

Create Instance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Input Streaming Module
"""""""""""""""""""""""""""""""""""

Possible error responces at **creating an instance** for Input Streaming Module:

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are allowed: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just a methadata record can be missed for some reasons." 
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."
 "Cannot create an instance of a module. The instance parameters haven't passed validation, which are declared in the methods called 'validate' (with different arguments). These methods are owned by a validator class that implements StreamingValidator interface. Errors: <list_of_errors>.", "All fields are completed correctly according to the requirements, but the 'options' field or other instance fields did not pass validation with the special function (that by default always returns the response the validation is successfully passed, so no such error should occur for now)."

*'Cannot create instance of module' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'<attribute>' attribute is required.", "The <attribute> field is not completed."
 "Instance '<instance_name>' already exists.", "All fields are completed correctly according to the requirements, but an instance with the same name already exists in the system.  In this case it does not matter that all other fields differ from the fields of the existing instance. "
 "Instance has incorrect name: '<instance_name>'. 

 Name of instance must contain digits, lowercase letters or hyphens. 
 First symbol must be a letter.", "All fields are completed correctly according to the requirements except the 'Name' field."
 "'checkpointInterval' attribute is required. 'checkpointInterval' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'checkpointInterval' field."
 "'perTaskCores' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'perTaskCores' field."
 "'perTaskRam' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'perTaskRam' field."
 "'performanceReportingInterval' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'performanceReportingInterval' field."
 "'coordinationService' attribute is required.", "The coordinationService field is not completed."
 "'coordinationService' attribute is not 'service.apache-zookeeper'.", "All fields are completed correctly according to the requirements except the 'coordinationService' field (service type is not Apache Zookeeper)."
 "'coordinationService' <instance_coordination-service> does not exist.", "All fields are completed correctly according to the requirements except the 'coordinationService' field (the service with the specified name does not exist.)"
 "'checkpointMode' attribute is required.", "The checkpointMode field is not completed."
 "Unknown value of 'checkpointMode' attribute: '<instance_checpoint-mode>'. 'checkpointMode' must be one of: [every-nth, time-interval].", "All fields are completed correctly according to the requirements except the 'checkpointMode' field."
 "'lookupHistory' attribute is required. 'lookupHistory' attribute must be greater than zero or equal to zero.", "All fields are completed correctly according to the requirements except the 'lookupHistory' field."
 "'queueMaxSize' attribute is required. 'queueMaxSize' attribute must be greater or equal than 271.", "All fields are completed correctly according to the requirements except the 'queueMaxSize' field."
 "Unknown value of 'defaultEvictionPolicy' attribute: '<instance_default-eviction-policy>'. 'defaultEvictionPolicy' must be one of: [LRU, LFU, NONE].", "All fields are completed correctly according to the requirements except the  'defaultEvictionPolicy' field."
 "Unknown value of 'evictionPolicy' attribute: '<instance_eviction-policy>'. 'evictionPolicy' must be one of: [fix-time, expanded-time].", "All fields are completed correctly according to the requirements except the 'evictionPolicy' field."
 "'backupCount' must be in the interval from 0 to 6.", "All fields are completed correctly according to the requirements except the 'backupCount' field."
 "Count of outputs cannot be less than <lower_bound_of_output_cardinality>.", "All fields are completed correctly according to the requirements except the  'Outputs' field (the number of outputs is less than the number in the module specification defining a minimum number of outputs)." 
 "Count of outputs cannot be more than <upper_bound_of_output_cardinality>.", "All fields are completed correctly according to the requirements except the 'Outputs' field (the number of outputs is larger than the number in the module specification defining a maximum number of outputs)."
 "'Outputs' contain the non-unique streams.", "All fields are completed correctly according to the requirements except the 'Outputs' field (Outputs names are not unique)."
 "Output stream '<output_stream_name>' does not exist.", "All fields are completed correctly according to the requirements except the 'Outputs' field (one or several specified outputs do not exist)."
 "Output streams must be one of the following type: <list_of_valid_types>.", "All fields are completed correctly according to the requirements except the 'Outputs' field (one or several  specified outputs have a type that is not specified for the module)."
 "All t-streams should have the same service.", "All fields are completed correctly according to the requirements except the 'Outputs' field (one or several outputs have a service different from the others."
 "Service for t-streams must be 'service.t-streams'.", "All fields are completed correctly according to the requirements except the 'Outputs' field (specified outputs have one and the same service, but this service is not of the T-streams type)."
 "'Parallelism' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the  'Parallelism' field."
 'Parallelism' must be greater than the total number of backups.", "All fields are completed correctly according to the requirements except the 'Parallelism' field."
 "Unknown type of 'parallelism' parameter. Must be a digit.", "All fields are completed correctly according to the requirements except the 'Parallelism' field. In this case it can be a numeric value only."
 "'asyncBackupCount' attribute must be greater than zero or equal to zero.числовым значением 'asyncBackupCount' field."

Regular Streaming Module
"""""""""""""""""""""""""""""""""

Possible error responces at **creating an instance** for Regular Streaming Module:

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are allowed: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just a methadata record can be missed for some reasons." 
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."
 "Cannot create an instance of a module. The instance parameters haven't passed validation, which are declared in the methods called 'validate' (with different arguments). These methods are owned by a validator class that implements StreamingValidator interface. Errors: <list_of_errors>.", "All fields are completed correctly according to the requirements, but the 'options' field or other instance fields did not pass validation with the special function (that by default always returns the response the validation is successfully passed, so no such error should occur for now)."

*'Cannot create instance of module' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "The 'Name' attribute is required.", "The Name field is not completed."
 "Instance '<instance_name>' already exists.", "All fields are completed correctly according to the requirements, but an instance with the same name already exists in the system.  In this case it does not matter that all other fields differ from the fields of the existing instance."
 "Instance has incorrect name: '<instance_name>'. A name of instance must contain digits, lowercase letters or hyphens. 
  The first symbol must be a letter.", "All fields are completed correctly according to the requirements except the 'Name' field."
 "'checkpointInterval' attribute is required. 'checkpointInterval' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'checkpointInterval' field."
 "'perTaskCores' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'perTaskCores' field."
 "'perTaskRam' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'perTaskRam' field."
 "'performanceReportingInterval' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'performanceReportingInterval' field."
 "'coordinationService' attribute is required.", "The coordinationService field is not completed."
 "'coordinationService' attribute is not 'service.apache-zookeeper'.", "All fields are completed correctly according to the requirements except the 'coordinationService' field (the service type is not Apache Zookeeper)."
 "'coordinationService' <instance_coordination-service> does not exist.", "All fields are completed correctly according to the requirements except the 'coordinationService' (a service with the specified name does not exist)."
 "'checkpointMode' attribute is required.", "The checkpointMode is not completed."
 "Unknown value of 'checkpointMode' attribute: '<instance_checpoint-mode>'. 'checkpointMode' must be one of: [every-nth, time-interval].", "All fields are completed correctly according to the requirements except the 'checkpointMode' field."
 "'eventWaitTime' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'eventWaitTime' field."
 "Unknown value of 'stateManagement' attribute: '<instance_state-management>'. 'stateManagement' must be one of: [none, ram, rocks].", "All fields are completed correctly according to the requirements except the 'stateManagement' field."
 "'stateFullCheckpoint' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'stateFullCheckpoint' field that should be grater than 0, if stateManagement does not equa to 'none, that is the module has state that is stored and called out via t-streams."
 "Unknown stream mode. Input streams must have one of mode: [split, full].", "All fields are completed correctly according to the requirements except the 'Inputs' field (one or several inputs have incorrect type that denotes how the stream should be used for composing an exection plan)."
 "Count of inputs cannot be less than <lower_bound_of_input_cardinality>.", "All fields are completed correctly according to the requirements except the 'Inputs' field (the number of inputs is less than the number denoting a minimum inputs number specified for the module)."
 "Count of inputs cannot be more than <upper_bound_of_input_cardinality>.", "All fields are completed correctly according to the requirements except the 'Inputs' field (the number of inputs is grater than the number denoting a maximum inputs number specified for the module)."
 "'Inputs' contain the non-unique streams.", "All fields are completed correctly according to the requirements except the 'Inputs' field (inputs names are not unique)."
 "Input stream '<input_stream_name>' does not exist.", "All fields are completed correctly according to the requirements except the 'Inputs' field (one or several inputs does not exist)."
 "Input streams must be one of the following type: <list_of_valid_types>.", "All fields are completed correctly according to the requirements except the 'Inputs' (one or several inputs have a type that is not specified for the module)."
 "'startFrom' attribute must be one of: [oldest, newest], if instance inputs have the kafka-streams.", "All fields are completed correctly according to the requirements except the 'startFrom' field (in case at least one Kafka input stream exist, the reading mode for inputs can be of two types: oldest (read all input messages) or newest (read nothing and wait till new messages come)."
 "'startFrom' must be one of: [oldest, newest] or timestamp.", "All fields are completed correctly according to the requirements except the 'startFrom' field (the mode for reading messages from inputs can be of three types: oldest (read all input messages), newest (read nothing and wait till new messages come), timestamp (read messages from the stream since the specified time moment))."
 "Count of outputs cannot be less than <lower_bound_of_output_cardinality>.", "All fields are completed correctly according to the requirements except the 'Outputs' field (the number of outputs is less than the number of minimum outputs specified for the module)."
 "Count of outputs cannot be more than <upper_bound_of_output_cardinality>.", "All fields are completed correctly according to the requirements except the 'Outputs'field (the number of outputs is larger than the number in the module specification defining a maximum number of outputs)."
 "'Outputs' contain the non-unique streams.", "All fields are completed correctly according to the requirements except the 'Outputs' field (output names are not unique)."
 "Output stream '<output_stream_name>' does not exist.", "All fields are completed correctly according to the requirements except the 'Outputs' field (one or several specified outputs do not exist)."
 "Output streams must be one of the following type: <list_of_valid_types>.", "All fields are completed correctly according to the requirements except the 'Outputs' field (one or several specified outputs have a type that is not specified for the module)."
 "All t-streams should have the same service.", "All fields are completed correctly according to the requirements except the 'Outputs' and/or 'Inputs' field(-s) (if inputs contain T-streams). One or several specified inputs/outputs of T-streams type have a service different from other T-streams." 
 "Service for t-streams must be 'service.t-streams'.", "All fields are completed correctly according to the requirements except the 'Outputs' and/or 'Inputs' fields (if inputs contain T-streams). The specified inputs/outputs have the same service, but it is not a service of a T-streams type."
 "Service for kafka streams must be 'service-apache-kafka'.", "All fields are completed correctly according to the requirements except the 'Inputs' field (if inputs have streams of the Apache Kafka type). One or several inputs have a service other than Kafka)."
 "'Parallelism' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'Parallelism' field."
 "'Parallelism' (<instance_parallelism>) is greater than minimum of partitions count (<minimum_count_of_partitions>) of input streams.", "All fields are completed correctly according to the requirements except the 'Parallelism' field, that exceeds a minimum number of input stream partitions (in this case the module performance can not be paralleled)."
 "Unknown type of 'parallelism' parameter. Must be a digit or 'max'.", "All fields are completed correctly according to the requirements except the 'Parallelism' field. In ths case it can be a numeric value or a 'max' word."

Batch Streaming Module
""""""""""""""""""""""""""""""""""""

Possible error responces at **creating an instance** for Batch Streaming Module:

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are allowed: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just a methadata record can be missed for some reasons." 
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."
 "Cannot create an instance of a module. The instance parameters haven't passed validation, which are declared in the methods called 'validate' (with different arguments). These methods are owned by a validator class that implements StreamingValidator interface. Errors: <list_of_errors>.", "All fields are completed correctly according to the requirements, but the 'options' field or other instance fields did not pass validation with the special function (that by default always returns the response the validation is successfully passed, so no such error should occur for now)."

*'Cannot create instance of module' Errors:*

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "The 'Name' attribute is required.", "The Name field is not completed."
 "Instance '<instance_name>' already exists.", "All fields are completed correctly according to the requirements, but an instance with the same name already exists in the system.  In this case it does not matter that all other fields differ from the fields of the existing instance. "
 "Instance has incorrect name: '<instance_name>'. A name of instance must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed correctly according to the requirements except the 'Name' field."
 "'perTaskCores' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'perTaskCores' field."
 "'perTaskRam' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'perTaskRam' field."
 "'performanceReportingInterval' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'performanceReportingInterval' field."
 "'coordinationService' attribute is required.", "The coordinationService field is not completed."
 "'coordinationService' attribute is not 'service.apache-zookeeper'.", "All fields are completed correctly according to the requirements except the 'coordinationService' field (the service type is not Apache Zookeeper)."
 "'coordinationService' <instance_coordination-service> does not exist.", "All fields are completed correctly according to the requirements except the 'coordinationService' (a service with the specified name does not exist)."
 "Unknown value of 'checkpointMode' attribute: '<instance_checpoint-mode>'. 'checkpointMode' must be one of: [every-nth, time-interval].", "All fields are completed correctly according to the requirements except the 'checkpointMode' field."
 "'eventWaitTime' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'eventWaitTime' field."
 "Unknown value of 'stateManagement' attribute: '<instance_state-management>'. 'stateManagement' must be one of: [none, ram, rocks].", "All fields are completed correctly according to the requirements except the 'stateManagement' field."
 "'stateFullCheckpoint' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'stateFullCheckpoint' field that should be grater than 0, if stateManagement does not equa to 'none', that is the module has state that is stored and called out via t-streams."
 "'Window' must be greater than zero.", "All fields are completed correctly according to the requirements except the 'Window' field, that should be grater than 0."
 "'slidingInterval' must be greater than zero.", "All fields are completed correctly according to the requirements except the 'slidingInterval' field, that should be grater than 0."
 "'Window' must be greater or equal than 'slidingInterval'.", "All fields are completed correctly according to the requirements except the 'slidingInterval' field, that should be less than or equal to 'Window'."
 "Unknown stream mode. Input streams must have one of mode: [split, full].", "All fields are completed correctly according to the requirements except the 'Inputs' field (one or several inputs have incorrect type that denotes how the stream should be used for composing an execution-plan)."
 "Count of inputs cannot be less than <lower_bound_of_input_cardinality>.", "All fields are completed correctly according to the requirements except the 'Inputs' field (the number of inputs is less than the number denoting a minimum inputs number specified for the module)."
 "Count of inputs cannot be more than <upper_bound_of_input_cardinality>.", "All fields are completed correctly according to the requirements except the 'Inputs' field (the number of inputs is grater than the number denoting a maximum inputs number specified for the module)."
 "'Inputs' contain the non-unique streams.", "All fields are completed correctly according to the requirements except the 'Inputs' field (inputs names are not unique)."
 "Input stream '<input_stream_name>' does not exist.", "All fields are completed correctly according to the requirements except the 'Inputs' field (one or several inputs does not exist)."
 "Input streams must be one of the following type: <list_of_valid_types>.", "All fields are completed correctly according to the requirements except the 'Inputs' (one or several inputs have a type that is not specified for the module)."
 "'startFrom' attribute must be one of: [oldest, newest], if instance inputs have the kafka-streams.", "All fields are completed correctly according to the requirements except the 'startFrom' field (in case at least one Kafka input stream exist, the reading mode for inputs can be of two types: oldest (read all input messages) or newest (read nothing and wait till new messages come)."
 "'startFrom' must be one of: [oldest, newest] or timestamp.", "All fields are completed correctly according to the requirements except the 'startFrom' field (the mode for reading messages from inputs can be of three types: oldest (read all input messages), newest (read nothing and wait till new messages come), timestamp (read messages from the stream since the specified time moment))."
 "Count of outputs cannot be less than <lower_bound_of_output_cardinality>.", "All fields are completed correctly according to the requirements except the 'Outputs' field (the number of outputs is less than the number of minimum outputs specified for the module)."
 "Count of outputs cannot be more than <upper_bound_of_output_cardinality>.", "All fields are completed correctly according to the requirements except the 'Outputs' field (the number of outputs is larger than the number in the module specification defining a maximum number of outputs)."
 "'Outputs' contain the non-unique streams.", "All fields are completed correctly according to the requirements except the 'Outputs' field (output names are not unique)."
 "Output stream '<output_stream_name>' does not exist.", "All fields are completed correctly according to the requirements except the 'Outputs' field (one or several specified outputs do not exist)."
 "Output streams must be one of the following type: <list_of_valid_types>.", "All fields are completed correctly according to the requirements except the 'Outputs' field (one or several specified outputs have a type that is not specified for the module)."
 "All t-streams should have the same service.", "All fields are completed correctly according to the requirements except the 'Outputs' and/or 'Inputs' field(-s) (if inputs contain T-streams). One or several specified inputs/outputs of T-streams type have a service different from other T-streams." 
 "Service for t-streams must be 'service.t-streams'.", "All fields are completed correctly according to the requirements except the 'Outputs' and/or 'Inputs' fields (if inputs contain T-streams). The specified inputs/outputs have the same service, but it is not a service of a T-streams type."
 "Service for kafka streams must be 'service.apache-kafka'.", "All fields are completed correctly according to the requirements except the 'Inputs' field (if inputs have streams of the Apache Kafka type). One or several inputs have a service other than Kafka)."
 "'Parallelism' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'Parallelism' field."
 "'Parallelism' (<instance_parallelism>) is greater than minimum of partitions count (<minimum_count_of_partitions>) of input streams.", "All fields are completed correctly according to the requirements except the 'Parallelism' field, that exceeds a minimum number of input stream partitions (in this case the module performance can not be paralleled)."
 "Unknown type of 'parallelism' parameter. Must be a digit or 'max'.", "All fields are completed correctly according to the requirements except the 'Parallelism' field. In ths case it can be a numeric value or a 'max' word."

Output Streaming Module
""""""""""""""""""""""""""""""""

Possible error responces at **creating an instance** for Output Streaming Module:

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are allowed: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just a methadata record can be missed for some reasons." 
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."
 "Cannot create an instance of a module. The instance parameters haven't passed validation, which are declared in the methods called 'validate' (with different arguments). These methods are owned by a validator class that implements StreamingValidator interface. Errors: <list_of_errors>.", "All fields are completed correctly according to the requirements, but the 'options' field or other instance fields did not pass validation with the special function (that by default always returns the response the validation is successfully passed, so no such error should occur for now)."

*'Cannot create instance of module' Errors:* 

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'<attribute>' attribute is required.", "The <attribute> field is not completed."
 "Instance '<instance_name>' already exists.", "All fields are completed correctly according to the requirements, but an instance with the same name already exists in the system.  In this case it does not matter that all other fields differ from the fields of the existing instance. "
 "Instance has incorrect name: '<instance_name>'. Name of instance must contain digits, lowercase letters or hyphens. First symbol must be a letter.", "All fields are completed correctly according to the requirements except the 'Name' field."
 "'checkpointInterval' attribute is required. 'checkpointInterval' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'checkpointInterval' field."
 "'perTaskCores' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'perTaskCores' field."
 "'perTaskRam' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'perTaskRam' field."
 "'performanceReportingInterval' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'performanceReportingInterval' field."
 "'coordinationService' attribute is required.", "The coordinationService field is not completed."
 "'coordinationService' attribute is not 'service.apache-zookeeper'.", "All fields are completed correctly according to the requirements except the 'coordinationService' field (service types is not Apache Zookeeper)."
 "'coordinationService' <instance_coordination-service> does not exist.", "All fields are completed correctly according to the requirements except the 'coordinationService' field (the service with the specified name does not exist.)"
 "'checkpointMode' attribute is required.", "The checkpointMode field is not completed."
 "Unknown value of 'checkpointMode' attribute: '<instance_checpoint-mode>'. 'checkpointMode' must be one of: [every-nth, time-interval].", "All fields are completed correctly according to the requirements except the 'checkpointMode' field."
 "'Input' attribute is required.", "The Input field is not completed."
 "Unknown value of 'stream-mode' attribute: 'split'.", "All fields are completed correctly according to the requirements except the 'Input' field (the input has incorrect typethat denotes how the stream should be used for composing an execution-plan)."
 "Input stream '<instance_input>' does not exist.", "All fields are completed correctly according to the requirements except the 'Input' field (the input does not exist)."
 "'Input stream' must be one of: <list_of_valid_types>.", "All fields are completed correctly according to the requirements except the 'Input' field (the input have a type that is not specified for the module)."
 "'Output' attribute is required.", "The Output field is not completed."
 "Output stream '<instance_output>' does not exist.", "All fields are completed correctly according to the requirements except the 'Output' field (the output does not exist)."
 "Output streams must be one of: <list_of_valid_types>.", "All fields are completed correctly according to the requirements except the 'Outputs' field (one or several  specified outputs have a type that is not specified for the module)."
 "'startFrom' must be one of: [oldest, newest] or timestamp.", "All fields are completed correctly according to the requirements except the 'startFrom' field (the mode for reading messages from inputs can be of three types: oldest (read all input messages), newest (read nothing and wait till new messages come), timestamp (read messages from the stream since the specified time moment))."
 "Service for t-streams must be 'service.t-streams'.", "All fields are completed correctly according to the requirements except the 'Input' field (specified inputs have one and the same service, but this service is not of the T-streams type)."
 "'Parallelism' attribute must be greater than zero.", "All fields are completed correctly according to the requirements except the 'Parallelism' field."
 "'Parallelism' (<instance_parallelism>) is greater than minimum of partitions count (<minimum_count_of_partitions>) of input streams.", "All fields are completed correctly according to the requirements except the 'Parallelism' field that exceeds the number of minimum input partitions (in this case the module performance can not be paralleled)."
 "Unknown type of 'parallelism' parameter. Must be a digit or 'max'.", "All fields are completed correctly according to the requirements except the 'Parallelism' field. In this case it can be a numeric value or a 'max' word."


Get list of instances for a module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are available: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just methadata record can be missed for some reasons."
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."

Get all instances of a specific module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are available: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just methadata record can be missed for some reasons."
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."

Get an instance of a specific module
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are available: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just methadata record can be missed for some reasons."
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."
 "Instance '<instance_name>' has not been found.", "The instance does not exist."

Delete an instance
~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Instance '<instance_name>' has not been found.", "The instance does not exist."
 "Cannot delete of instance '<instance_name>'. Instance is not been stopped, failed or ready.", "The instance is being started, stopped or deleted, or is started, so it can not be deleted. It has one of the following statuses: [starting, started, stopping, deleting]. Firstly, the instance should be set to the 'stopped' status, or you should wait till it stops working, then you can delete the instance. An instance can not be deleted, if its status is one of the following: [ready, failed, stopped]."
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are available: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just methadata record can be missed for some reasons."
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."

Start an instance
~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 
 
 "Instance '<instance_name>' has not been found.", "The instance does not exist."
 "Cannot start of instance. Instance has already launched.", "The instance can not be started as it has one of the following statuses: [starting, started, stopping, deleting]. To start an instance it should be of one of the following statuses: [ready, failed, stopped]."
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are available: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just methadata record can be missed for some reasons."
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."

Get the information about instance tasks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Instance '<instance_name>' has not been found.", "The instance does not exist."
 "Cannot get instance framework tasks info. The instance framework has not been launched.", "The information on instance framework tasks is not available as the framework is not started."
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are available: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just methadata record can be missed for some reasons."
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."

Stop an instance
~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Instance '<instance_name>' has not been found.", "The instance does not exist."
 "Cannot stop instance. Instance has not been started.", "The instance can not be stopped as it has one of the following statuses: [starting, stopping, deleting, failed, stopped]. An instance with the 'started' status only can be stopped."
 "Module type '<module_type>' does not exist.", "The module of the specified type does not exist. The following options are available: 'input-streaming', 'regular-streaming', 'output-streaming', 'batch-streaming'."
 "Module '<module_type>-<module_name>-<module_version>' has not been found.", "No information that the specified module exists is found. That does not precisely mean that the file does not exist. Just methadata record can be missed for some reasons."
 "Jar of module '<module_type>-<module_name>-<module_version>' has not been found in the storage.", "The specified module does not exist."
 
.. _Config_Settings_Errors:

Config Settings API Error Responses
--------------------------------------------

The responses description for CRUD REST-API for Configurations methods are presented below.

A configuraition can relate to one of the domains: configuration.system, configuration.t-streams, configuration.apache-kafka, configuration.sql-database, configuration.elasticsearch, configuration.apache-zookeeper.

Create a new configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Possible error responses for **configuration creation**:

*Cannot create сonfig setting. Errors:* 

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
  
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "Configuration with name '<confguration_name>' already exists.", "All fields are completed correctly according to the requirements but a config setting with the same name already exists."
 "Сonfiguration has incorrect name: '<confguration_name>'. Name of configuration can contain digits, lowercase letters, hyphens or periods. First symbol must be a letter.", "All fields are completed correctly according to the requirements except the 'Name' field."
 "Сonfiguration has incorrect name: '<confguration_name>'. T-streams domain configuration must be only for consumer or producer.", "All fields are completed correctly according to the requirements except the 'Name' field. For T-streams the configuration can be set only for a consumer/producer. The list of valid settings is at the T-streams site for `producers <http://t-streams.com/docs/a2-api/tstreams-factory-api/#TSF_DictionaryProducer_keyset>`_ and `consumers <http://t-streams.com/docs/a2-api/tstreams-factory-api/#TSF_DictionaryConsumer_keyset>`_ (the 'Textual constant' column)."
 "'Name' attribute is required.", "The Name field is not completed or is an empty string."
 "'Value' attribute is required.", "The Value field is not completed or is an empty string."
 "'Domain' attribute is required.", "The Domain field is not completed or is an empty string."
 "Unknown value of 'domain' attribute: '<confg-setting_domain>'. 'Domain' must be one of: [configuration.system, configuration.t-streams, configuration.apache-kafka, configuration.elasticsearch, configuration.apache-zookeeper, configuration.sql-database].", "All fields are completed correctly according to the requirements except the 'Domain' field."

Delete a config setting
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "'<domain>' сonfiguration '<confg-setting_name>' has not been found.", "The configuration does not exist." 
 "Cannot recognize configuration domain '<domain>'. Domain must be one of the following values: 'configuration.system, configuration.t-streams, configuration.apache-kafka, configuration.elasticsearch, configuration.apache-zookeeper, configuration.sql-database'.", "The domen for the configuration does not exist."


Get all config settings for specific config domain
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60
 
 "Cannot recognize configuration domain '<domain>'. Domain must be one of the following values: 'configuration.system, configuration.t-streams, configuration.apache-kafka, configuration.elasticsearch, configuration.apache-zookeeper, configuration.sql-database'.", "The domen for the config setting does not exist."

Get a config setting
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60

 "'<domain>' сonfiguration '<confguration_name>' has not been found.", "The configuration does not exist." 
 "Cannot recognize configuration domain '<domain>'. Domain must be one of the following values: 'configuration.system, configuration.t-streams, configuration.apache-kafka, configuration.elasticsearch, configuration.apache-zookeeper, configuration.sql-database'.", "The domen for the configuration does not exist."

