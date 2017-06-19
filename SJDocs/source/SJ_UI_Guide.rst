Stream Juggler
==============

UI Guide
========

|juggler-logo|

.. |juggler-logo| image:: https://bitworks.software/sites/default/files/logo-juggler-prj.png

Juggler is a real time stream processing platform designed for building both simple and complex event processing (CEP). Juggler uses Apache Messos, Kafka and T-streams to construct scalable and flexible processing algorithms. Juggler functions on the same principle as Apache Samza, but enables exactly-once processing and provides an integrated solution with a RESTful interface, JavaScript UI and an ad hoc repository for modules, services, streams and other data processing pipeline components.

Overview
--------

Juggler's architecture is designed so that exactly-once processing is performed not only within a single processing block but throughout the entire sequence, starting from the moment stream events are fed the system and up to the moment when the output data is stored to conventional data storage.

The approach based on loosely coupled blocks with exactly-once processing support throughout the entire sequence allows for decomposing data processing in order to provide better modularity, performance management and simplicity in development.
Juggler supports four models of feeding events for processing:

- no delay; this model dictates that the events be processed immediately after becoming available in the streams; 

- in a time slot where events are grouped into blocks, each a certain amount of seconds long; processing is scheduled based on overlapping or non-overlapping sliding time slots; 

- in a length-based slot where events are grouped into blocks, each a certain number of transactions long; processing is scheduled based on overlapping or non-overlapping sliding time slots;
 
- in a real-time slot where events are grouped into blocks, each including transactions in a certain time interval; processing is scheduled based on overlapping or non-overlapping sliding time slots; 

Juggler easily integrates with in-memory grid systems, for example, Hazelcast, Apache Ignite.

The systems is available under Apache License v2. 

Get Started
-----------
Juggler has a user-friendly UI to create a processing sequence of arbitrary complexity, watch it in action and manage it using pre-created modules with flexible functionality. 
For beginning go to the http://stream-juggler.z1.netpoint-dc.com:18080 and perform a registration process:
#. 
#. 
#. 
Now you are ready to work with the system. 

Providers 
---------
As the first step a provider should be created.  That is the provider of sevices for input data transformation into a stream (???)

.. image:: CreateProvider.png

Please, press the «Create provider» button and fill in the form:

#. *Choose* *type*  *

  .. image:: CreateProvider_Type.png

  Select from the drop down a type of the provider you are aimed to create. The following options are available:

  - cassandra (temporarily is inactive)

  - aerospike  (temporarily is inactive)

  - zookeeper

  - kafka

  - Elastic Search

  - JDBC

  - REST

  The type of the provider is determined with the type of the instance you want to create and work with at the end.

2. *Name* *

  Enter  a name of the provider here. It should be unique, must contain digits, lowercase letters or hyphens and start with a letter. 

#. *Description* 

  Enter a description for the provider here.

#. *Hosts* *

  Enter a provider host that determines a file locaction.

  Add more hosts clicking at the «Add Host» button and entering host names in the appeared lines.

#. *Login*

  Enter a provider login here if necessary

#. *Password*

  Enter a password for the provider if necessary.

  .. note:: Required fields are marked with an asterisk (*)

Click «Create» at the bottom and see the provider is in the list of providers now. Details of the node are displayed to the right when clicking the provider in the list. 

.. image:: Providers_list.png

In the list of providers the following actions can be performed:

#. **View** provider`s name and description

#. **Delete** a provider clicking at the corresponding icon in the Action block near the name of the provider you want to delete

#. **Test** **Connection**

The list of providers can be filtered by its type and/or a name using the search tool above the list.

Services
--------

The next step is to create services. It is a service to perform an input data into a stream of an exact type. (???)

Under the Services section of the main navigation bar you will find the list of services.

.. image:: CreateService.png

Please, press the «Create services» button and fill in the form:

1.  *Choose* *type* *

   Select from the dropdown a type of the services:

- CassDB (temporarily is unavailable)
- ESInd
- KfkQ
- TstrQ
- ZKCoord
- ArspkDB (temporarily is unavailable)
- JDBC
- REST

  .. image:: CreateService_Type.png

2. *Name* *
 
  Enter a name of the services.  It must contain digits, lowercase letters or hyphens and start with a letter.

3.  *Description*

  Provide a description for the services here if necessary.

4. *Provider* *

  This field appears once the service type is chosen.

  Select a provider for the services here. 

  The range of providers available in the dropdown is determined by the chosen services type.

5. *Namespace* *

  It is a required field for the ZKCoord services type.

  Please, specify a namespace here. It must contain digits, lowercase letters or underscore and start with a letter. 

6.  *Index* *

  This field appears when the ESInd services type is selected and is required for filling in.

  Please, specify an index of the services here. It must contain digits, lowercase letters or underscore and start with a letter. 

7. *Login*

  This field appears when the ESInd services type is selected and is not required for filling in.

  Please, specify a login of the services here. 

8. *Password*

  This field appears when the ESInd services type is selected and is not required for filling in.

  Please, specify a password of the services here. 

9. *ZK provider* *

  This field appears when the KfkQ services type is selected and is required for filling in.

  Please, select a zookeeper provider for the services here. 

10. *ZK namespace* *

  This field appears when the KfkQ services type is selected and is required for filling in.

  Please, specify a zookeeper namespace for the services here. 

11. *Prefix* *

  This field appears when the TstrQ services type is selected and is required for filling in.

  Please, enter a prefix for the services here. 

12. *Token* *

  This field appears when the TstrQ services type is selected and is required for filling in.

  Please, enter a token for the services here. It must contain no more than 32 symbols.

13. *Database* *name* *

  This field appears when the JDBC services type is selected and is required for filling in.

  Please, enter a database name for the services here. 

  .. note:: Required fields are marked with an asterisk (*)

Click «Create» at the bottom and see the servces are in the list of providers now. Details of the node are displayed to the right when clicking the services in the list. 

.. image:: ServicesList.png

In the list of services the following actions can be performed:

1. **View** services` name and description

2. **View** a provider for the services and get the provider`s information in a popup window by clicking at the active provider`s name in the «Providers» column.

  .. image:: ServicesList_ProviderInfo.png

3. **Delete** a service clicking at the corresponding icon in the Action block near the name of the services you want to delete

The list of services can be filtered by its type and/or a name using the search tool above the list.


Streams
-------

The next step is to create a data stream. A stream is a sequence of events happening randomly at irregular intervals.


Modules
-------

Instances
---------

Configuration Settings
----------------------

Custom Files
------------
