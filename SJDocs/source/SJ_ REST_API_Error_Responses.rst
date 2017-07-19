API Error Responses
=========================

At this page all possible API error responses are described. It will allow you to discern the causes of error responses and quickly correct them for smoothing the system workflow. 

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

The responses description for CRUD REST-API for Providers methods.

Responses for Creating of Provider
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**'Cannot create provider' Errors:**


.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60  

 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "Provider with name '<provider_name>' already exists.", "All fields are filled following the requirements, but the provider with this name already exists in the system. 
 
 In this case it does not matter that all other fields differ from the fields of the existing provider. "
 "Provider has incorrect name: '<provider_name>'. Name of the provider must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are filled following the requirements except the 'Name' field."
 "Unknown type '<provider_type>' provided. Must be one of: [ES, Kafka, zookeeper, JDBC, REST].", "All fields are filled following the requirements except the 'Type' field. This error is not possible in UI as the Type field offers the dropdown list of types."
 "Wrong host provided: '<provider_host>'.", "All fields are filled following the requirements except the 'Hosts' field."
 "Host cannot contain any URI path ('<uri_path>').", "All fields are completed following the requirements except the 'Host' field that contains the id of some resource (defining the file location, e.g. an image is stored at the desk: /home/smith_j/desk/imgpsh_fullsize.jpg)."
 "Host '<provider_host>' must contain port", "All fields are filled following the requirements except the 'Hosts' field where no port is defined in one or several elements."
 "'Name' attribute is required.", "The Name field is not completed."
 "'Type' attribute is required.", "The Type field is not completed."
 "'Hosts' attribute is required.", "The Hosts field is not completed."
 "'Hosts' must contain at least one host.", "The Hosts field is empty."
 "Config setting 'jdbc.driver.<driver-name>' is required.", "Config setting jdbc.driver.<driver-name> is not completed (required for JDBC type only)."
 "Config setting 'jdbc.driver.<driver-name>.class' is required.", "Config setting jdbc.driver.<driver-name>.class  is not completed (required for JDBC type only)."
 "Config setting 'jdbc.driver.<driver-name>.prefix' is required.", "Config setting jdbc.driver.<driver-name>.prefix  is not completed (required for JDBC type only)."
 "Prefix '<prefix>' in config setting 'jdbc.driver.<driver-name>.prefix' is incorrect.", "Incorrect jdbc.driver.<driver-name>.prefix value. The following options are allowed: jdbc:mysql, jdbc:postgresql, jdbc:oracle:thin (required for JDBC type only)."

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

The responses description for CRUD REST-API for Services methods.

Create Services
~~~~~~~~~~~~~~~~~~~~~~~~
Possible error responses for **Elasticsearch** service creation:

**'Cannot create service' Errors:**

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements but a service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 " Unknown type '<service_type>' of service provided. Must be one of: [ESInd, KfkQ, TstrQ, ZKCoord, JDBC].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'ES' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (a provider of incorrect type is specified."
 "'Index' attribute is required.", "The Keyspace field is not completed."
 "Service has incorrect 'index': '<service_index>'. Name must contain digits, lowercase letters or underscore. First symbol must be a letter.", "All fields are completed following the requirements except the 'Index' field."


Possible error responses for **Kafka** service creation:

**'Cannot create service' Errors:**

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements buta service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<service_type>' of service provided. Must be one of: [ESInd, KfkQ, TstrQ, ZKCoord, JDBC].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the 'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'kafka' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (a provider of incorrect type is specified."
 "'zkNamespace' attribute is required.", "The zkNamespace field is not completed."
 "Service has incorrect 'zkNamespace': '<service_zk-namespace>'. A name must contain digits, lowercase letters or underscore. The first symbol must be a letter.", "All fields are completed following the requirements except the 'zkNamespace' field."
 "'zkProvider' attribute is required.", "The zkProvider field is not completed."
 "Zookeeper provider '<service_zk-provider>' does not exist.", "All fields are completed following the requirements except the 'zkProvider' field (the zk provider specified in the service does not exist.)"
 "'zkProvider' must be of type: 'zookeeper' ('<service_zk-provider_type>' is given instead).", "All fields are completed following the requirements except the 'zkProvider' field (the specified provider is not of a zookeeper type)."


Possible error responses for **T-streams** service creation:

**'Cannot create service' Errors:**

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 

 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements buta service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. Name of service must contain digits, lowercase letters or hyphens. First symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type field is not completed."
 "Unknown type '<service_type>' of service provided. Must be one of: [ESInd, KfkQ, TstrQ, ZKCoord, JDBC].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the 'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'zookeeper' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (the specified provider is not of a zookeeper type)."
 "'Prefix' attribute is required.", "The Prefix field is not completed."
 "Service has incorrect 'prefix': '<service_prefix>'. Prefix must be a valid znode path.", "All fields are completed following the requirements except the 'Prefix' field."
 "'Token' attribute is required.", "The Token field is not completed."
 "Service has incorrect 'token': '<service_token>'. Token must contain no more than 32 symbols|All fields are completed following the requirements except the 'Token' field."


Possible error responses for **ZooKeeper** service creation:

**'Cannot create service' Errors:**

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 
 
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements buta service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type is not completed."
 "Unknown type '<service_type>' of service  provided. Must be one of: [ESInd, KfkQ, TstrQ, ZKCoord, JDBC].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the 'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'zookeeper' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (the specified provider is of a wrong type)."
 "'Namespace' attribute is required.", "The Namespace field is not completed."
 "Service has incorrect 'namespace': '<service_namespace>'. A name must contain digits, lowercase letters or underscore. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Namespace' field."


Possible error responses for **SQL** service creation:

**'Cannot create service' Errors:**

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 
 
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements buta service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type is not completed."
 "Unknown type '<service_type>' of service provided. Must be one of: [ESInd, KfkQ, TstrQ, ZKCoord, JDBC].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
 "'Provider' attribute is required.", "The Provider field is not completed."
 "Provider '<service_provider>' does not exist.", "All fields are completed following the requirements except the 'Provider' field (the provider specified in the service does not exist)."
 "'Provider' must be of type: 'JDBC' ('<service_provider_type>' is given instead).", "All fields are completed following the requirements except the 'Provider' field (the specified provider is of a wrong type)."
 "'Database' attribute is required.", "The Database field is not completed."
 "'Driver' attribute is required.", "The Driver field is not completed."
 "Custom file '<driver-file>' is required. ", "There is no JDBC-driver file <driver-file>."
 "Database '<database_name>' does not exist.", "The Database field points to the database that does not exist."
 "Can not create client: '<reason>'.", "The client is not created for the reason that is specified after colon."



Possible error responses for **RESTful** service creation:

**'Cannot create service' Errors:**

.. csv-table::  
 :header: "Response", "Description"
 :widths: 25, 60 
 
 ":ref:`Incorrect_Json_Api_Responses`", "Incorrect JSON"
 "'Name' attribute is required.", "The Name field is not completed."
 "Service with name '<service_name>' already exists.", "All fields are completed following the requirements buta service with the same name already exists in the system."
 "Service has incorrect name: '<service_name>'. A name of service must contain digits, lowercase letters or hyphens. The first symbol must be a letter.", "All fields are completed following the requirements except the 'Name' field."
 "'Type' attribute is required.", "The Type is not completed."
 "Unknown type '<service_type>' of service provided. Must be one of: [ESInd, KfkQ, TstrQ, ZKCoord, JDBC].", "All fields are completed following the requirements except the 'Type' field.  This error is not possible in UI as the Type field offers the dropdown list of types."
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


