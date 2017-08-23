Tutorial
========================

.. Contents::

Introduction 
-----------------------

This tutorial is aimed to represent  SJ-Platform and give a quick start for a user to see the platform at work.

The demo project presented below is an example code developed to demonstrate a user how to run his first SJ module. A step-by-step guidance will help to deploy the system in a local mode (minimesos) or at a cluster (Mesos) and to implement a module example to a real-life task. 

Through an example project a user will get to know the system structure, its key components and general concepts of the platform workflow.


SJ-Platform Overview
----------------------------------

As a quick reminder,  SJ-Platform is a real-time processing system. The data are processed in modules where they are passed via streams. The result data are exported to an external storage.

A general structure of SJ-Platform can be presented as at the image below:

.. figure:: _static/TutorialGeneral.png

Processor represents a module pipeline where the data processing is performed.

Configurations uploaded to the modules via REST determine the mode of data processing.

The processing result is exported to an external storage. It can be Elasticsearch, RESTful or JDBC-compatible data storages.

Besides,  SJ-Platform provides a user with a comprehensive RESTful API instrumentation and Web UI.

Below an example of a real-life task will be provided for better understanding of how the data processing can be performed in the platform. Thus, the tutorial will provide you with:

1. a ready-to-use problem resolution of an example task on SJ-Platform base;

2. instructions on development, deployment and customization of your own code for your specific aims.


Example task
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Let’s introduce an example task which will illustrate the platform workflow in the real-world use.

The demo code is responsible for collecting of aggregated information on the accessibility of nodes. 

The result data will render the nodes’ work.

The number of accessible and inaccessible IPs per a period of time will be summed up.

Besides, calculated average response time on each node will give the idea of the node efficiency. 

Before providing a solution to the task let’s have a quick look at a processing level of the platform.

Processing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The processing workflow in the platform can be generally illustrated as at the diagram below:

.. figure:: _static/ModulePipeline.png

Green, yellow and purple blocks are executed with SJ-Platform and it is an input module, a processing module and an output module, respectively.

The input module receives raw data and transforms them into a data stream of a proper type compatible with the processing module type. 

The processing module performs data aggregation and sends the result to the output module. In the provided example the data aggregation is performed with a regular-streaming module.

In the output module the processed data is transformed into a flow of entities appropriate for storing into an external storage of a specified type. In the provided example the output module will export the result data into the Elasticsearch external data storage.
                
The illustrated pipeline is a general solution. It fits for most real-life problems.
But the platform allows resolution of more complicated tasks. So the pipeline can be more expanded. More input streams can ingest raw data. Several Input modules can be included in the pipeline to accept the raw data and transform it for passing further to the processing stage.

You can launch more than one processing module. The data streams can be distributed among them in various ways.

A few Output modules may receive the processed data and put them to a storage.

In the example task solution the processing workflow is formed in the following way:

.. figure:: _static/FPingDemo.png

This diagram demonstrates the processing workflow of the demo. As a quick reminder, the task is to collect the aggregated information on the accessibility of nodes.

As you can see, the data come to a TCP input module through a pipeline of fping and netcat.

Then the input module parses ICMP echo responses (select IP and response time) and ICMP unreachable responses (select only IP) and puts parsed data into 'echo-response' stream and 'unreachable-response' stream, respectively.

After that, the processing module aggregates response time and a total amount of echo/unreachable responses by IP per 1 minute and sends aggregated data to 'echo-response-1m' stream.

Two more processing modules are embedded into the pipeline to calculate responses per 3 minutes and per 1 hour. Correspondingly, 'echo-response-3m' and 'echo-response-1h' streams are created for those processing modules to put the aggregated data on echo-responses to.

Finally, the output modules export aggregated data from echo-response streams to Elasticsearch. The result then can be visualized in a diagram using Kibana.

The data is fed to the system, passed from one module to another and exported from the system via streams. Read more about streams under the “Creating Streams” section.

In the demo project, the entities are added to the system via REST API as it is less time-consuming. The platform entities can be also created via the UI filling in the forms for each entity with necessary settings.

The result is easy-to-see via Web UI.  Or send ‘GET’ API requests to return created entities in JSON.

Now having the general idea on the platform workflow, we can dive into solving an example task on the base of SJ-Platform. 

And the first step is the system deployment.


Step 1. Deployment 
-----------------------------

The system works on the basis of the following core technologies: Apache Mesos, Apache Zookeeper, Apache Kafka, Docker, MongoDB, Hazelcast, Elasticsearch, SQL database, REST.

To solve the example task we need to deploy:

1) Apache Mesos - for all computations;
2) Mesosphere Marathon - a framework for executing tasks on Mesos;
3) Apache Zookeeper -  for coordination;
4) Java
5) Docker
6) MongoDB - as a database;
7) T-streams - as a message broker; 
8) REST - for access to the UI;
9) Elasticsearch - as an external data storage;
10) Kibana - to visualize Elasticsearch data.

There are 2 ways of the platform deployment – on cluster (i.e. Mesos) and locally (on minimesos). Choose which is more convenient for you. 

For the example task, the instructions are provided for the system deployment on Mesos.

The deployment is performed via REST API.

Firstly, deploy Mesos and other services.

1) Deploy Mesos, Marathon, Zookeeper. You can follow the instructions at the official `installation guide <http://www.bogotobogo.com/DevOps/DevOps_Mesos_Install.php>`_

Start Mesos and the services. Make sure you have access to Mesos interface, Marathon interface, and Zookeeper is running. 

For Docker deployment follow the instructions at the official `installation guide <https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/#install-docker-ce>`_

Install Java::
                                         
 $ sudo add-apt-repository ppa:webupd8team/java
 $ sudo apt-get update
 $ sudo apt-get install oracle-java8-installer
 $ sudo apt-get install oracle-java8-set-default

Find detailed instructions `here <https://tecadmin.net/install-oracle-java-8-ubuntu-via-ppa/>`_.

2) Create json files and a configuration file (config.properties) for tts. 

**mongo.json**::

 {  
   "id":"mongo",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"mongo",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":27017,
               "hostPort":31027,
               "protocol":"tcp" 
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always" 
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":512
 }

**sj-rest.json**::

 {  
   "id":"sj-rest",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"bwsw/sj-rest:dev",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":8080,
               "hostPort":31080,
               "protocol":"tcp" 
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always" 
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":1024,
   "env":{
      "MONGO_HOSTS":"172.17.0.1:31027",
      "ZOOKEEPER_HOST":"172.17.0.1",
      "ZOOKEEPER_PORT":"2181" 
   }
 }

**elasticsearch.json**::

 {  
   "id":"elasticsearch",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"elasticsearch",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":9200,
               "hostPort":31920,
               "protocol":"tcp" 
            },
        {  
               "containerPort":9300,
               "hostPort":31930,
               "protocol":"tcp" 
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always" 
            }
         ]
      }
   },
   "args": ["-Etransport.host=0.0.0.0", "-Ediscovery.zen.minimum_master_nodes=1"],
   "instances":1,
   "cpus":0.2,
   "mem":256
 }

**Config.properties** (replace <zk_ip> with a valid ip)::

 key=pingstation
 active.tokens.number=100
 token.ttl=120

 host=0.0.0.0
 port=8080
 thread.pool=4

 path=/tmp
 data.directory=transaction_data
 metadata.directory=transaction_metadata
 commit.log.directory=commit_log
 commit.log.rocks.directory=commit_log_rocks

 berkeley.read.thread.pool = 2

 counter.path.file.id.gen=/server_counter/file_id_gen

 auth.key=dummy
 endpoints=127.0.0.1:31071
 name=server
 group=group

 write.thread.pool=4
 read.thread.pool=2
 ttl.add-ms=50
 create.if.missing=true
 max.background.compactions=1
 allow.os.buffer=true
 compression=LZ4_COMPRESSION
 use.fsync=true

 zk.endpoints=<zk_ip>
 zk.prefix=/pingstation
 zk.session.timeout-ms=10000
 zk.retry.delay-ms=500
 zk.connection.timeout-ms=10000

 max.metadata.package.size=100000000
 max.data.package.size=100000000
 transaction.cache.size=300

 commit.log.write.sync.value = 1
 commit.log.write.sync.policy = every-nth
 incomplete.commit.log.read.policy = skip-log
 commit.log.close.delay-ms = 200
 commit.log.file.ttl-sec = 86400
 stream.zookeeper.directory=/tts/tstreams

 ordered.execution.pool.size=2
 transaction-database.transaction-keeptime-min=70000
 subscribers.update.period-ms=500

**tts.json** (replace <path_to_conf_directory> with an appropriate path to the configuration directory on your computer and <external_host> with a valid host)::

 {
    "id": "tts",
    "container": {
        "type": "DOCKER",
        "volumes": [
            {
                "containerPath": "/etc/conf",
                "hostPath": "<path_to_conf_directory>",
                "mode": "RO" 
            }
        ],
        "docker": {
            "image": "bwsw/tstreams-transaction-server",
            "network": "BRIDGE",
            "portMappings": [
                {
                    "containerPort": 8080,
                    "hostPort": 31071,
                    "protocol": "tcp" 
                }
            ],
            "parameters": [
                {
                    "key": "restart",
                    "value": "always" 
                }
            ]
        }
    },
    "instances": 1,
    "cpus": 0.1,
    "mem": 512,
    "env": {
      "HOST":"<external_host>",
      "PORT0":"31071" 
    }
}

**kibana.json**::

 {  
   "id":"kibana",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"kibana",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":5601,
               "hostPort":31561,
               "protocol":"tcp" 
            }
         ],
         "parameters":[  
            {  
               "key":"restart",
               "value":"always" 
            }
         ]
      }
   },
   "instances":1,
   "cpus":0.1,
   "mem":256,
   "env":{  
      "ELASTICSEARCH_URL":"http://172.17.0.1:31920" 
   }
 }

3) Run the services on Marathon:

**Mongo**::
 
 $ curl -X POST http://172.17.0.1:8080/v2/apps -H "Content-type: application/json" -d @mongo.json 


**Elasticsearch**:

Please, note that `vm.max_map_count` should be slave::

 sudo sysctl -w vm.max_map_count=262144


Then launch elasticsearch::

 $ curl -X POST http://172.17.0.1:8080/v2/apps -H "Content-type: application/json" -d 
 @elasticsearch.json


**SJ-rest**::

 $ curl -X POST http://172.17.0.1:8080/v2/apps -H "Content-type: application/json" -d @sj-rest.json    
    
**T-Streams**::
 
 $ curl -X POST http://172.17.0.1:8080/v2/apps -H "Content-type: application/json" -d @tts.json 


**Kibana**::

 $ curl -X POST http://172.17.0.1:8080/v2/apps -H "Content-type: application/json" -d @kibana.json


Via the Marathon interface make sure the services are deployed.

.. figure:: _static/ServicesOnMarathon.png

4) Copy the github repository of SJ-Platform::

    $ git clone https://github.com/bwsw/sj-platform.git

5) Add the settings if running the framework on Mesos needs principal/secret:: 
 
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"framework-principal\",\"value\": <principal>,\"domain\": \"configuration.system\"}" 
    $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"framework-secret\",\"value\": <secret>,\"domain\": \"configuration.system\"}" 
 
6) Copy the demo project repository::

     cd ..
    $ git clone https://github.com/bwsw/sj-fping-demo.git
    $ cd sj-fping-demo


Now look and make sure you have access to the Web UI. You will see the platform but it is not completed with any entities yet. They will be added in the next steps.

At first, the infrastructure for the module performance can be created next.


Step 2. Module Uploading 
---------------------------------

Now as the system is deployed, modules can be uploaded.

A module is a .jar file, containing module specification and configurations.

.. figure:: _static/ModuleExecutorAndValidator.png
   :scale: 120%
   
.. note:: Find more about modules at the :ref:`Modules` page.  A hello-world on a custom module can be found at the :ref:`Custom_Module` section.

For the stated example task the following modules will be uploaded:

- a TCP input module - sj-regex-input module that accepts TCP input streams and transforms raw data to put them to T-streams and pass for processing;

- a processing module - ps-process module, which is a regular-streaming module that processes data element-by-element.

- an output module - ps-output module that exports resulting data to Elasticsearch.

Download the modules from the Sonatype repository and upload it to the system following the instructions for the example task.


Example Task
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please, follow these steps to build and upload the modules of pingstation demo.

To configure environment::

 address=<host>:<port>

<host>:<port> — SJ Rest host and port.

Module Downloading from Sonatype Repository
"""""""""""""""""""""""""""""""""""""""""""""""""""""""

- To download the sj-regex-input module from the sonatype repository::

   $ curl "https://oss.sonatype.org/content/repositories/snapshots/com/bwsw/sj-regex-input_2.12/1.0-SNAPSHOT/sj-regex-input_2.12-1.0-SNAPSHOT.jar" -o sj-regex-input.jar 

- To download the ps-process module from the sonatype repository::

   $ curl “https://oss.sonatype.org/content/repositories/snapshots/com/bwsw/ps-process_2.12/1.0-SNAPSHOT/ps-process_2.12-1.0-SNAPSHOT.jar” -o ps-process-1.0.jar

- To download the ps-output module from the sonatype repository::

   $ curl “https://oss.sonatype.org/content/repositories/snapshots/com/bwsw/ps-output_2.12/1.0-SNAPSHOT/ps-output_2.12-1.0-SNAPSHOT.jar” -o ps-output-1.0.jar

Module Uploading
""""""""""""""""""""""""""""""""""""

Upload modules to the system::

 $ curl --form jar=@sj-regex-input.jar http://$address/v1/modules
 $ curl --form jar=@ps-process/target/scala-2.11/ps-process-1.0.jar http://$address/v1/modules
 $ curl --form jar=@ps-output/target/scala-2.11/ps-output-1.0.jar http://$address/v1/modules

Now in UI you can see the uploaded modules under the ‘Modules’ tab.

.. figure:: _static/ModulesUploaded.png

Step 3. Configurations and Engine Jars Uploading 
----------------------------------------------------------------

An engine is required to start a module. A module can not process data without an engine (that is a .jar file containing required configuration settings). In fact, it is a framework that launches the module executor.

.. figure:: _static/Engine.png
   :scale: 110%
   
To implement the processing workflow for the example task resolution the following jars should be uploaded:

1. a jar per each module type  - input-streaming, regular-streaming, output-streaming;

2. a jar for Mesos framework that starts the engine.

Thus, as a next step engines should be compiled and uploaded.
 
Upload engine jars
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Please, upload the engine jars for the three modules ( input-streaming, regular-streaming, output-streaming) and the Mesos framework. You can find them at our GitHub repository::

 $ cd sj-platform

 $ address=sj-rest.marathon.mm:8080

 $ curl --form jar=@core/sj-mesos-framework/target/scala-2.12/sj-mesos-framework-1.0-SNAPSHOT.jar http://$address/v1/custom/jars
 $ curl --form jar=@core/sj-input-streaming-engine/target/scala-2.12/sj-input-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars
 $ curl --form jar=@core/sj-regular-streaming-engine/target/scala-2.12/sj-regular-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars
 $ curl --form jar=@core/sj-output-streaming-engine/target/scala-2.12/sj-output-streaming-engine-1.0-SNAPSHOT.jar http://$address/v1/custom/jars

Now engine jars should appear in the UI under Custom Jars of the "Custom files" navigation tab.

.. figure:: _static/EnginesUploaded.png

Setup configurations for engines
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The configurations will be added to the system via REST. 

The range of configurations includes required and optional ones. The full list of all configurations can be viewed at the :ref:`Configuration` page. 

To resolve the example task it is enough to upload the required configurations only.

Example Task
""""""""""""""""""

For solving an example task, we will upload the following configurations via REST:

- session.timeout - Use when connecting to zookeeper in milliseconds (usually when we are dealing with t-streams consumers/producers and kafka streams)

- current-framework - Indicates what file is used to run a framework. By this value you can get a setting that contains a file name of framework jar.

- crud-rest-host - For the host on the which the rest has launched.

- crud-rest-port - For the port on the which the rest has launched.

- marathon-connect - Use to launch a framework that is responsible for running engine tasks and provides the information about launched tasks. It should start with 'http://'.

- marathon-connect-timeout - Use when trying to connect by 'marathon-connect' (in milliseconds).


Send the next POST requests to upload the configs::

 $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"session-timeout\",\"value\": \"7000\",\"domain\": \"configuration.apache-zookeeper\"}"
 $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"current-framework\",\"value\": \"com.bwsw.fw-1.0\",\"domain\": \"configuration.system\"}"

 $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-host\",\"value\": \"sj-rest.marathon.mm\",\"domain\": \"configuration.system\"}"
 $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-port\",\"value\": \"8080\",\"domain\": \"configuration.system\"}"

 $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect\",\"value\": \"http://marathon.mm:8080\",\"domain\": \"configuration.system\"}"
 $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect-timeout\",\"value\": \"60000\",\"domain\": \"configuration.system\"}"


Send the next POST requests to upload configurations for module validators::

 $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"regular-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.RegularInstanceValidator\",\"domain\": \"configuration.system\"}"
 $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"input-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.InputInstanceValidator\",\"domain\": \"configuration.system\"}"
 $ curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"output-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.OutputInstanceValidator\",\"domain\": \"configuration.system\"}"

In the UI you can see the uploaded configurations under the “Configuration” tab of the main navigation.

.. figure:: _static/ConfigurationsUploaded.png

Step 4. Creating Streaming Layer 
------------------------------------------

The raw data is fed to the platform from different sources. And within the platform, the data is passed to and from a module in streams. Thus, in the next step, the streams for data ingesting and exporting will be created.

Prior to creating a stream, the infrastructure needs to be created for the streaming layer.

Different modules require different stream types for input and output.
                   
A module receives data from input streams from TCP or Kafka. Within the platform, the data is transported to and from modules via T-streams. It is a native streaming type for SJ-Platform that allows exactly-once data exchange between modules. 


.. figure:: _static/ModuleStreaming.png

Streams need infrastructure: **Providers** and **Services**. This is a required presetting without which streaming will not be so flexible. 

Streaming flexibility lies in the one-to-many connection between providers and services, services and streams. One provider works with many services (they can be of various types) as well as one service can provide several streams. These streams take necessary settings from the common infrastructure (providers and services). There is no need to duplicate the settings for each individual stream.

The type of Provider and Service is determined with the type of streams. Find more about types of platform entities at `the UI guide <http://streamjuggler.readthedocs.io/en/develop/SJ_UI_Guide.html#get-started>`_ .

In the example task solution the following stream types are implemented:

1. TCP input stream ingests the raw data into the system;

2. T-streams streaming passes the data to and from the processing module;

3. output modules export aggregated data from T-streams to Elasticsearch.

.. figure:: _static/StreamingInPlatform.png

Below the steps for creating streaming infrastructure such as providers, services, and streams via REST API can be found.

Set Up Streaming Infrastructure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Prior to creating streams, it is necessary to provide the infrastructure: providers and services.

They can be of different types. The types of platform entities in the pipeline determine the type of providers and services that are necessary in the particular case.

Example Task
"""""""""""""""""""""""

In the example task pipeline the modules of three types take place: the input-streaming, regular-streaming and output-streaming. For all types of modules, the Apache Zookeeper service is necessary. Thus, it requires the Apache Zookeeper provider.

Besides, the Apache Zookeeper provider is required for T-streams service that is in its turn needed for streams of T-streams type within the platform, and instances of the input-streaming and the regular-streaming modules.

The provider and the service of Elasticsearch type are required by the Elasticsearch output streams to put the result in the Elasticsearch data storage.

As a result, the following infrastructure is to be created:
Providers of Apache Zookeeper and Elasticsearch types;
Services of  Apache Zookeeper, T-streams and Elasticsearch types.


1) Set up providers.

- Apache Zookeeper for T-streams streaming (‘echo-response’ and ‘unreachable-response’ streams) within the platform, for Zookeeper service necessary for all types of  instances::

   $ sed -i 's/176.120.25.19:2181/<zookeeper_address>/g' api-json/providers/zookeeper-ps-provider.json
   $ curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers/zookeeper-ps-provider.json"

- Elasticsearch for output streaming (all ‘es-echo-response’ streams).

There is a default value of Elasticsearch IP (176.120.25.19) in json configuration files, so we need to change it appropriately via sed app before using::

   $ sed -i 's/176.120.25.19/elasticsearch.marathon.mm/g'  api-json/providers/elasticsearch-ps-provider.json
   $ curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers /elasticsearch-ps-provider.json"

The created providers are available in the UI under the “Providers” tab.

.. figure:: _static/ProvidersCreated.png

2) Next set up services:

- Apache Zookeeper service for all modules::

   $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/zookeeper-ps-service.json"

- T-streams service for T-streams streaming (all ‘echo-response’ streams and the ‘unreachable-response’ stream) within the platform and the instances of the input-streaming and the regular-streaming modules::

   $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/tstream-ps-service.json"

- Elasticsearch service for output streaming (all ‘es-echo-response’ streams) and the output-streaming module::

   $ curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/elasticsearch-ps-service.json"

Please, make sure the created services have appeared in UI under the “Services” tab.

.. figure:: _static/ServicesCreated.png

Creating Streams
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Once streaming infrastructure is created, it is high time to create streams. Please, use the “POST” API requests below to create streams that will be used in the instances of input-streaming, regular-streaming and output-streaming modules.

Example task
""""""""""""""""""""""""

For **sj-regex-input module**:

To create an ‘echo-response’ output stream of sj-regex-input module (consequently, an input stream of ps-process module). It will be used for keeping an IP and average time from ICMP echo-response and also a timestamp of the event::

 $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/echo-response.json"

To create an ‘unreachable response’ output stream of the input module (consequently, an input stream of processing module). It will be used for keeping an IP from ICMP unreachable response and also a timestamp of the event::

 $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/unreachable-response.json"


For **ps-process module**:

To create output streams of ps-process module (consequently, an input stream of the output module) named ‘echo-response-1m’, ‘echo-response-3m’ and ‘echo-response-1h’. They will be used for keeping an aggregated information about average time of echo response, total amount of echo responses, total amount of unreachable responses and the timestamp for each IP (per 1 minute, 3 minutes and 1 hour)::

 $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data   "@api-json/streams/echo-response-1m.json"

 $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/echo-response-3m.json"

 $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/echo-response-1h.json"

For **ps-output module**:

To create output streams of ps-output module named ‘es-echo-response-1m’, ‘es-echo-response-3m’, ‘es-echo-response-1h’. They will be used for keeping an aggregated information (per 1 minute, 3 minutes and 1 hour) from the previous stream including total amount of responses::

 $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/es-echo-response-1m.json"

 $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/es-echo-response-3m.json"

 $ curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/es-echo-response-1h.json"

The created streams should be available now in UI under the “Streams” tab.

.. figure:: _static/StreamsCreated.png

Step 5. Create Output Destination
---------------------------------------------

At this step all necessary indexes, tables and mapping should be created for storing the processed result.

Example task
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
In the provided example task the result data is stored to the Elasticsearch data storage.

Thus, it is necessary to create the index and mapping for ES.

Create the index and the mapping for Elasticsearch sending the PUT request::

 $ curl --request PUT "http://176.120.25.19:9200/pingstation" -H 'Content-Type: application/json' --data "@api-json/elasticsearch-index.json"


Step 6. Creating Instances 
-----------------------------------------

Once the system is deployed, configurations and modules are uploaded, the streaming layer with necessary infrastructure is created, an instance is to be created in the next step.

A module uses a specific instance to personalize its work. An instance is a full range of settings to perform a specific executor type.

.. figure:: _static/Instance.png
   :scale: 120%
   
An instance is created with specific parameters and is set to particular streams.
 
For each module an instance should be created.

Creating Instances
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
For instance creation we will send the POST requests. See the instructions below for creating insatnces for the example task solution.

Example task
"""""""""""""""""""""""

For creating an instance of the sj-regex-input module send the following POST request::

 $ curl --request POST "http://$address/v1/modules/input-streaming/pingstation-input/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-input.json"

For creating an instance of the ps-process module send the following POST request::

 $ curl --request POST "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-process.json"

Create two more instances for the ps-process module with different checkpoint intervals to process data every 3 minute and every hour. Remember to create them with different names::

 $ curl --request POST "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-echo-process-3m.json"

 $ curl --request POST "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-echo-process-1h.json"


For creating an instance of the ps-output module send the following POST request::

 $ curl --request POST "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-output.json"
 
Create two more instances to receive data from the instances processing data every 3 minutes and every hour. Remember to create them with different names. Change the ‘input’ values to ‘echo-response-3m’ and ‘echo-response-1h’ respectively to receive data from these streams. 

Change the ‘output’ values to ‘es-echo-response-3m’ and ‘es-echo-response-1h’ correspondingly to put the result data to these streams:: 

 $ curl --request POST "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-output-3m.json"

 $ curl --request POST "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/pingstation-output-1h.json"

The created instances should be available now in UI under the “Instances” tab. There they will appear with the “ready” status.

.. figure:: _static/InstancesCreated.png

Ready! The module can be launched.

Launching Instances
----------------------------------

After the streaming layer with its infrastructure and instances are created you can start a module. 

The module starts working after it is launched. The input module starts receiving data, transform the data for T-streams to pass to the processing module. The processing module starts processing them and put to T-streams to pass to the output module. The output module starts storing the result in a data storage. 

In fact, it is not a module that is started. It is an instance of the module.

In the example case, there are three modules (input-streaming, regular-streaming and output-streaming modules) and each of them has its own instances. Thus, these instances should be launched one by one. 


For launching the **input module instance** send::

 $ curl --request GET "http://$address/v1/modules/input-streaming/pingstation-input/1.0/instance/pingstation-input/start"


For launching the **processing module instances** send::

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process/start"

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process-3m/start"

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process-1h/start" 

For launching the **output module instances** send::

 $ curl --request GET "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance/pingstation-output/start"

 $ curl --request GET "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance/pingstation-output-3m/start"

 $ curl --request GET "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance/pingstation-output-1h/start" 


To get a list of listening ports of input module instance::

 $ curl --request GET "http://$address/v1/modules/input-streaming/pingstation-input/1.0/instance/pingstation-input"

and look at the field named ‘tasks’, e.g. it may look as follows::

 "tasks": {
  "pingstation-input-task0": {
    "host": "176.120.25.19",
    "port": 31000
  },
  "pingstation-input-task1": {
    "host": "176.120.25.19",
    "port": 31004
  }
 }

And now you can **start a flow**. Please, replace nc with the host and port of your instance task::

 fping -l -g 91.221.60.0/23 2>&1 | nc 176.120.25.19 31000

If you have a look in the UI, you will see the launched modules with the “started” status.

.. figure:: _static/InstancesStarted.png

See the Results 
------------------------------

To see the processing results saved in ElasticSearch, please, go to Kibana. There the aggregated data can be rendered in a diagram.

The result can be viewed while the module is working. A necessary auto-refresh interval can be set for the diagram to update the graph.

Firstly, click the Settings tab and fill in the data entry field '*' instead of 'logstash-*'. 

Then there will appear another data entry field called 'Time-field name'. You should choose 'ts' from the combobox and press the create button. 

After that, click the Discover tab. 

Choose a time interval of 'Last 15 minutes' in the top right corner of the page, as well as an auto-refresh interval of 45 seconds, as an example. Now a diagram can be compiled. 

Select the parameters to show in the graph at the left-hand panel. 

The example below is compiled in Kibana v.5.5.1.

It illustrates average time of echo-responses by IPs per a selected period of time (e.g. 1 min). As you can see, different nodes have the different average time of response. Some nodes respond faster than others. 

.. figure:: _static/Kibana.png

Lots of other parameter combinations can be implemented to view the results.

Instance Shutdown 
-----------------------------

Once the task is resolved and necessary data is aggregated, the instance can be stopped. 

A stopped instance can be restarted again if it is necessary.

If there is no need for it anymore, a suspended instance can be deleted. On the basis of the uploaded modules and the whole created infrastructure (providers, services, streams) other instances can be created for other purposes.

To stop instances in the example task the following requests should be sent.

For suspending the **sj-regex-input module instance** send::

 $ curl --request GET "http://$address/v1/modules/input-streaming/pingstation-input/1.0/instance/pingstation-input/stop"

For suspending the **ps-process module instances** send::

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process/stop "

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process-3m/stop "

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process-1h/stop "

For suspending the **ps-output module instances** send::

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-output/stop" 

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-output-3m/stop"  

 $ curl --request GET "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-output-1h/stop" 

In the UI, you will see the suspended instances with the “stopped” status.

.. figure:: _static/InstancesStopped.png

Deleting Instance
---------------------------------

A stopped instance can be deleted if there is no need for it anymore. An instance of a specific module can be deleted via REST API by sending a DELETE request (as described below). Or instance deleting action is available in the UI under the “Instances” tab.

Make sure the instances to be deleted are stopped and are not with one of the following statuses: «starting», «started», «stopping», «deleting».

The instances of the modules can be deleted one by one. 

For deleting the sj-regex-input module instance send::

 $ curl --request DELETE "http://$address/v1/modules/input-streaming/pingstation-input/1.0/instance/pingstation-input/"

For stopping the ps-process module instance send::

 $ curl --request DELETE "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process/"

 $ curl --request DELETE "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process-3m/" 

 $ curl --request DELETE "http://$address/v1/modules/regular-streaming/pingstation-process/1.0/instance/pingstation-process-1h/"

For stopping the ps-output module instance send::

 $ curl --request DELETE "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance/pingstation-output/"

 $ curl --request DELETE "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance/pingstation-output-3m/"

 $ curl --request DELETE "http://$address/v1/modules/output-streaming/pingstation-output/1.0/instance/pingstation-output-1h/"

Via the UI you can make sure the instances are deleted.

Make sure  via the UI that the instances are deleted.

Find more information at: 

:ref:`Modules` - more about module structure.

:ref:`Custom_Modules` - how to create a module.

`sflow demo on github repo <https://github.com/bwsw/sj-sflow-demo/tree/develop>`_ - another demo task

:ref:`Architecture` - the structure of the platform.


