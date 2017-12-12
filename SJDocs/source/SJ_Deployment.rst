SJ-Platform Deployment on Cluster 
=====================================

.. Contents::

The section provides a detailed step-by-step instruction on Stream Juggler Platform deployment on cluster. 

Currently, the deployment on **Mesos** is supported for SJ-Platform.

A complete list of requirements and the deployment procedure description can be found below. This is a demo case for which all the services are deployed and started. Then entities are added to the platform - providers, services, streams, modules and instances. These entities are also of the particular types necessary to solve the demo task.

Overall Deployment Infrastructure
--------------------------------------------

The Stream Juggler platform works on the base of the following services:

- Resource management is fulfilled via `Apache Mesos <http://mesos.apache.org/>`_ that allows to run the system at scale and to support different types of workloads.

- To start applicable services in the Mesos we use `Docker <http://mesos.apache.org/documentation/latest/docker-containerizer/>`_.

- `Marathon <https://mesosphere.github.io/marathon/>`_ allows running long-life tasks on Mesos.

- To perform leader election in case the currently leading Marathon instance fails `Apache Zookeeper <https://zookeeper.apache.org/>`_ is used. Zookeeper is also responsible for instance task synchronization for a Batch module.

- Data sources for the platform are `Netty <https://netty.io/>`_ and `T-streams <https://t-streams.com>`_ libraries and `Apache Kafka <https://kafka.apache.org/>`_. 

- We use `MongoDB <https://www.mongodb.com/>`_ as a document database that provides high performance and availability. All created platform entities (Providers, Services, Streams, Instances, etc.), as well as configurations, are stored here. 

- Elasticsearch 5.5.2, SQL database or a system which provides the RESTful interface are external storages the output data is stored to.

SJ-Platform's backend is written in Scala. The UI is based on Angular 4.0. REST API is written in Akka HTTP.

Below, you will find necessary instructions to run the services on a cluster (Mesos). The section covers the steps on deployment of necessary services and configurations. The system is set up with no entities. You can create them depending on your aims ans tasks. The steps on creating platform entities are provided in the :ref:`fping-example-task` section.

The deployment is performed via REST API.

.. _Mesos_deployment:

Deployment of Required Services on Mesos
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Firstly, deploy Mesos and other required services. 

1. Deploy Mesos, Marathon, Zookeeper. You can follow the instructions in the official `instalation guide <http://www.bogotobogo.com/DevOps/DevOps_Mesos_Install.php>`_ .

Please, note, the deployment is described for one default Mesos-slave with available ports [31000-32000]. 

If you are planning to launch an instance with greater value of the "parallelizm" parameter, i.e. to run tasks on more than 1 nodes, you need to increase the "executor_registration_timeout" parameter for Mesos-slave.

The requirements to Mesos-slave: 

- 2 CPUs, 
- 4096 memory.

Mesos-slave must support Docker containerizer.

2. For Docker deployment follow the instructions at the official `installation guide <https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/#install-docker-ce>`_

3. Install Java::
                                         
    sudo add-apt-repository ppa:webupd8team/java
    sudo apt-get update
    sudo apt-get install oracle-java8-installer
    sudo apt-get install oracle-java8-set-default

   Find detailed instructions on Java deployment in the `installation guide <https://tecadmin.net/install-oracle-java-8-ubuntu-via-ppa/>`_.

4. Start Mesos-master, Mesos-slave and the services. 

After performing all the steps, make sure you have access to Mesos interface, Marathon interface. Apache Zookeeper now should be active.

5. Create a configuration file (config.properties) and JSON files for the physical services - MongoDB, RESTful, T-streams server. Please, name them as it is specified here.

Replace <mongo_port> with a port for MongoDB. It is one of the available Mesos-slave ports.

**mongo.json**::

 {  
   "id":"mongo",
   "container":{  
      "type":"DOCKER",
      "docker":{  
         "image":"mongo:3.4.7",
         "network":"BRIDGE",
         "portMappings":[  
            {  
               "containerPort":27017,
               "hostPort":<mongo_port>,
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

**sj-rest.json**

Replace <slave_advertise_ip> with a valid Mesos-salve IP.

Replace <zk_ip> and <zk_port> according to the zookeeper address.

Replace <rest_port> with the port for the SJ-rest service. It should be one of the available Mesos-slave ports.

Replace <mongo_port> - port of MongoDB, one of the available Mesos-slave ports::

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
               "hostPort":<rest_port>,
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
      "MONGO_HOSTS":"<slave_advertise_ip>:<mongo_port>",
      "ZOOKEEPER_HOST":"<zk_ip>",
      "ZOOKEEPER_PORT":"<zk_port>" 
   }
 }

For sj-rest.json it is better to upload the docker image separately::
 
 sudo docker pull bwsw/sj-rest:dev

**config.properties** 

Replace <zk_ip> according to the Zookeeper address.

Replace <token> and <prefix-name> with valid prefix and token. These names should be specified then in the T-streams service JSON (see below)::

 key=<token>
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
 zk.prefix=<prefix_name>
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

Specify the same token and prefix in the T-streams service JSON::

 {
  "name": "tstream-ps-service",
  "description": "Tstream service for demo",
  "type": "service.t-streams",
  "provider": "zookeeper-ps-provider",
  "prefix": <prefix-name>,
  "token" : <tolen>
 }

**tts.json** 

This is a JSON file for T-streams. Please, replace <path_to_conf_directory> with an appropriate path to the configuration file directory on your computer. Also replace <slave_advertise_ip> with the Mesos-slave IP. 

Replace <tts_port> with the port for the tts service. It should be one of the available Mesos-slave ports::

 {
    "id": "tts",
    "container": {
        "type": "DOCKER",
        "volumes": [
            {
                "containerPath": "/etc/conf/config.properties",
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
                    "hostPort": <tts_port>,
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
      "HOST":"<slave_advertise_ip>",
      "PORT0":<tts_port> 
    }
 }

6. Run the services on Marathon.

Replace <marathon_address> with a valid Marathon address.

**Mongo**::
 
 curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @mongo.json 

**SJ-rest**::

 curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @sj-rest.json  

**tts**::
 
 curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @tts.json 

Via the Marathon interface make sure the services are deployed.

Now look and make sure you have access to the Web UI. You will see the platform but it is not completed with any entities yet. 

In the next section we will show you how to upload modules as well as engines for them, configurations for engines

Engine Uploading
""""""""""""""""""""""""""
Before uploading modules, upload the engine jars for them. 

1. You should download the engine jars for each module types (input-streaming, regular-streaming, batch-streaming, output-streaming) and a Mesos framework::

    wget http://c1-ftp1.netpoint-dc.com/sj/1.0-SNAPSHOT/sj-mesos-framework.jar
    wget http://c1-ftp1.netpoint-dc.com/sj/1.0-SNAPSHOT/sj-input-streaming-engine.jar
    wget http://c1-ftp1.netpoint-dc.com/sj/1.0-SNAPSHOT/sj-regular-streaming-engine.jar
    wget http://c1-ftp1.netpoint-dc.com/sj/1.0-SNAPSHOT/sj-batch-streaming-engine.jar
    wget http://c1-ftp1.netpoint-dc.com/sj/1.0-SNAPSHOT/sj-output-streaming-engine.jar
    
Now upload the engine jars into the platform. Please, replace <slave_advertise_ip> with the Mesos-slave IP::

    cd sj-platform
    address=<slave_advertise_ip>:31080
    
    curl --form jar=@sj-mesos-framework.jar http://$address/v1/custom/jars
    curl --form jar=@sj-input-streaming-engine.jar http://$address/v1/custom/jars
    curl --form jar=@sj-regular-streaming-engine.jar http://$address/v1/custom/jars
    curl --form jar=@sj-batch-streaming-engine.jar http://$address/v1/custom/jars
    curl --form jar=@sj-output-streaming-engine.jar http://$address/v1/custom/jars

When creating a module you should use correct name and version of the engine:

==========================  =======================================  ==============================================
Module type                 Engine name                              Engine version
==========================  =======================================  ==============================================
*Input-streaming*           com.bwsw.input.streaming.engine          1.0

*Regular-streaming*         com.bwsw.regular.streaming.engine        1.0   
 
*Batch-streaming*           com.bwsw.batch.streaming.engine          1.0		   

*Output-streaming*          com.bwsw.output.streaming.engine         1.0

==========================  =======================================  ==============================================

Specify them in the module specification JSON for ``engine-name`` and ``engine-version`` fields, for example::
  
  },
  "module-type": "regular-streaming",
  "engine-name": "com.bwsw.regular.streaming.engine",
  "engine-version": "1.0",
  "options": {},
  "validator-class": "com.bwsw.sj.examples.pingstation.module.regular.Validator",
  "executor-class": "com.bwsw.sj.examples.pingstation.module.regular.Executor"
 }
 
2. Setup configurations for engines.

The range of configurations includes required and optional ones. 

The list of all configurations can be viewed at the :ref:`Configuration` page.

To set up required configurations for the engines, run the following commands. Please, replace <slave_advertise_ip> with the Mesos-slave IP and <marathon_address> with the address of Marathon::

   curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"session-timeout\",\"value\": \"7000\",\"domain\": \"configuration.apache-zookeeper\"}" 
   curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"current-framework\",\"value\": \"com.bwsw.fw-1.0\",\"domain\": \"configuration.system\"}" 
   curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-host\",\"value\": \"<slave_advertise_ip>\",\"domain\": \"configuration.system\"}" 
   curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-port\",\"value\": \"31080\",\"domain\": \"configuration.system\"}" 
   curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect\",\"value\": \"http://<marathon_address>\",\"domain\": \"configuration.system\"}" 
   curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect-timeout\",\"value\": \"60000\",\"domain\": \"configuration.system\"}" 
   curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"kafka-subscriber-timeout\",\"value\": \"100\",\"domain\": \"configuration.system\"}" 
   curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"low-watermark\",\"value\": \"100\",\"domain\": \"configuration.system\"}" 

3. Send the next POST requests to upload configurations for module validators::

    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"input-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.InputInstanceValidator\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"regular-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.RegularInstanceValidator\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"batch-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.BatchInstanceValidator\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"output-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.OutputInstanceValidator\",\"domain\": \"configuration.system\"}" 
    
4. You can add the following optional configuraions if necessary. They have default values in the system but can be overriden.

**Optional** configurations:

.. csv-table:: 
  :header: "Config Domain","Name", "Description", "Default value"
  :widths: 15, 20, 50, 15
  
  "system", "framework-principal", "Framework principal for mesos authentication", "---"
  "system", "framework-secret",  "Framework secret for mesos authentication", "---"
  "system", "framework-backoff-seconds", "Seconds for first delay after crash", "7"
  "system", "framework-backoff-factor", "Factor for backoffSeconds parameter of following delays", "7.0"
  "system", "framework-max-launch-delay-seconds", "Max seconds for delay", "600"
  "system", "output-processor-parallelism", "A number of threads used to write data to an external datastorage (Elasticsearch or RESTful)", "8"

.. note::  In general 'framework-backoff-seconds', 'framework-backoff-factor' and 'framework-max-launch-delay-seconds' configure exponential backoff behavior when launching potentially sick apps. This prevents sandboxes associated with consecutively failing tasks from filling up the hard disk on Mesos slaves. The backoff period is multiplied by the factor for each consecutive failure until it reaches ``maxLaunchDelaySeconds``. This applies also to tasks that are killed due to failing too many health checks.

.. Сonfiguration domain named 'Apache Kafka' contains properties used to create an Apache Kafka consumer (see `the official documentation <https://kafka.apache.org/documentation/#consumerconfigs>`_). .. note:: You must not define properties such as 'bootstrap.servers', 'enable.auto.commit', 'key.deserializer' and 'value.deserializer' in order to avoid a system crash.

Сonfiguration domain named 'T-streams' contains properties used for a T-streams consumer/producer. 

.. note:: You must not define properties such as 'producer.bind-host', 'producer.bind-port', 'consumer.subscriber.bind-host' and 'consumer.subscriber.bind-port' to avoid a system crash. 

To see the properties list check the following links: for a `producer <http://t-streams.com/docs/a2-api/tstreams-factory-api/#TSF_DictionaryProducer_keyset>`_ and for a `consumer <http://t-streams.com/docs/a2-api/tstreams-factory-api/#TSF_DictionaryConsumer_keyset>`_ (you should use the textual constants to create a configuration).

For each uploaded custom jar a new configuration is added in the following format:: 

 key = {custom-jar-name}-{version}, value = {file-name}


Creating Platform Entities
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Under this section you will find the information on platform entities creation.

We will not provide you with specific instructions as this part is custom and the set of platform entities you need for your tasks may differ. Step-by-step instructions on creating platform entities are provided in the :ref:`fping-example-task` section.

The following entities should be uploaded or created in the system:

1) Modules - input-streaming, regular-streaming or batch-streaming, output-streaming types;
2) Providers; 
3) Services;
4) Streams;
5) Instances fo reach module types.

Modules
""""""""""

You should create your own modules. Please, use instructions on module creation at :ref:`Custom_Module`.

Then upload modules following the instruction in :ref:`Module_Uploading` of the Tutorial. Use REST API requests to  upload each module (see :ref:`Modules_REST_API`). Replace <module_jar_name> with the name of the module JAR file::

 curl --form jar=@<module_jar_name>.jar http://$address/v1/modules

Or module uploading can be performed via the UI (see :ref:`UI_Modules`).

Providers
""""""""""
Providers are a part of the streaming infrastructure. They can be created using REST API (replace <provider_name> with the name of provider)::

 curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers/<provider_name>.json"

For more details see :ref:`REST_Providers`.

Or providers can be created via the UI (see :ref:`UI_Providers`).

Services
""""""""""
Services are a part of the streaming infrastructure. They can be created using REST API (replace <service_name> with the name of service)::

 curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/<service_name>.json"

For more details see :ref:`REST_Services`.

Or services can be created via the UI (see :ref:`UI_Services`).

Streams
""""""""""
Streams provide data exchange between modules. They can be created using REST API (replace <stream_name> with the name of stream)::

 curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/<stream_name>.json"

For more details see :ref:`REST_Streams`.

Or streams can be created via the UI (see :ref:`UI_Streams`).

Instances
""""""""""

Instances are used with engines to determine their collaborative work with modules. Each module needs an individual instance for it. Its type corresponds to the module type (input-streaming, regular-streaming or batch-streaming, output-streaming). 

Instances can be created using REST API (replace <instance_name> with the name of instance)::
 
 curl --request POST "http://$address/v1/modules/input-streaming/pingstation-input/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/<instance_name>.json"

For more details see :ref:`REST_API_Instance`.

Or instances can be created via the UI (see :ref:`UI_Instances`).

Launch instances one by one to start the flow.
