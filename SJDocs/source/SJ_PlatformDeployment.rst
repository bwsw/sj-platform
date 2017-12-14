.. _Platform_Deployment:

Platform Deployment
================================

In this section you will find the information on how to start working with the Stream Juggler Platform. We offer two options here: 

1) to deploy the platform on Mesos cluster, or
2) to run the pre-built |VirtualBox (TM)| image with the :ref:`fping-example-task` solution implemented on the SJ-Platform base.

Please, read the requirements for each option and decide which is more suitable for your aims.

SJ-Platform Deployment on Mesos Cluster
-----------------------------------------

The first option is to deploy SJ-Platform on a cluster. Currently, the deployment on `Apache Mesos <http://mesos.apache.org/>`_ as a universal distributed computational engine is supported.

The detailed step-by-step instructions on Stream Juggler Platform deployment on Mesos cluster is provided. You will find here a complete list of requirements and the deployment procedure description. We will deploy and start all the necessary services. The platform will contain no entities. It means you can structure the pipeline corresponding to your aims from scratch. The entities can be added to the platform via `REST API <http://streamjuggler.readthedocs.io/en/develop/SJ_CRUD_REST_API.html>`_ or `the UI <http://streamjuggler.readthedocs.io/en/develop/SJ_UI_Guide.html>`_ . 

.. Another option is to deploy SJ-Platform in a local mode using `minimesos <https://www.minimesos.org/>`_ as a testing environment.

Minimum system requirements are as follows:

- Linux host with 4-8 GB of RAM and 4 CPU cores; 
- Docker v17.03 installed (see the official `installation guide <https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/#install-docker-ce>`_);  
- Java installed (see  the official `installation guide <https://tecadmin.net/install-oracle-java-8-ubuntu-via-ppa/>`_).

Overall Deployment Infrastructure
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Stream Juggler Platform works on the base of the following services:

- Resource management is fulfilled via `Apache Mesos <http://mesos.apache.org/>`_.

- To start applicable services on Mesos we use `Docker <http://mesos.apache.org/documentation/latest/docker-containerizer/>`_.

- `Marathon <https://mesosphere.github.io/marathon/>`_ allows running long-life tasks on Mesos.

- To perform leader election in case the currently leading Marathon instance fails `Apache Zookeeper <https://zookeeper.apache.org/>`_ is used. Zookeeper is also responsible for instance task synchronization for a Batch module.

- Data sources for the platform are `Netty <https://netty.io/>`_ and `T-streams <https://t-streams.com>`_ libraries and `Apache Kafka <https://kafka.apache.org/>`_. 

- We use `MongoDB <https://www.mongodb.com/>`_ as a document database that provides high performance and availability. All created platform entities (Providers, Services, Streams, Instances, etc.), as well as configurations, are stored here. 

- Elasticsearch 5.5.2, SQL database or a system which provides the RESTful interface are external storages the output data is stored to.

SJ-Platform's backend is written in Scala. The UI is based on Angular 4.0. REST API is written in Akka HTTP.

The section covers the steps on deployment of necessary services and configurations. The system is set up with no entities. You can create them depending on your aims and tasks. The steps on creating platform entities are provided in the :ref:`UI-Guide`.

.. _Mesos_deployment:

Deployment of Required Services on Mesos
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In the first step, deploy Mesos and other required services. 

1. Deploy Mesos, Marathon, Zookeeper. You can follow the instructions in the official `instalation guide <http://www.bogotobogo.com/DevOps/DevOps_Mesos_Install.php>`_ .

    If you are planning to launch an instance with greater value of the ``parallelizm`` parameter, i.e. to run tasks on more than 1 nodes, you need to increase the ``executor_registration_timeout`` parameter for Mesos-slave.

    The requirements to Mesos-slave: 

     - 2 CPUs; 
     - 4096 memory.

    Mesos-slave should support Docker containerizer.

    Now make sure you have access to Mesos interface, Marathon interface. Apache Zookeeper should be active.

2. Create a configuration file (config.properties) and JSON files for the physical services - MongoDB, SJ-rest, tts. Please, name them as it is specified here.

**mongo.json**

Replace <mongo_port> with the port of MongoDB. It should be one of the available Mesos-slave ports.

::

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
               "hostPort":"<mongo_port>",
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

Please, replace:

- <slave_advertise_ip> with a valid Mesos-slave IP;
- <zk_ip> and <zk_port> with the Zookeeper address;
- <rest_port> with the port for the SJ-rest service. It should be one of the available Mesos-slave ports.
- <mongo_port> with the port of MongoDB. Use the one you specified in **mongo.json**.

::

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
               "hostPort":"<rest_port>",
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

This is a file with configurations for the tts service (used for T-streams). 

Please, replace:

- <zk_ip> according to the Zookeeper address;
- <token> and <prefix-name> with valid token and prefix (description is provided in the :ref:`T-streams-service`). These token and prefix should be specified then in the T-streams service JSON (see below).

::

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
  "description": "Example of T-streams service",
  "type": "service.t-streams",
  "provider": "zookeeper-ps-provider",
  "prefix": "<prefix-name>",
  "token" : "<token>"
 }

**tts.json** 

This is a JSON file for T-streams. Please, replace:

- <path_to_conf_directory> with an appropriate path to the configuration file directory on your computer;
- <slave_advertise_ip> with the Mesos-slave IP;
- <tts_port> with the port for the tts service. It should be one of the available Mesos-slave ports.

::

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
                    "hostPort": "<tts_port>",
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
      "PORT0":"<tts_port>"
    }
 }

3. Run the services on Marathon.

   We will run the services via REST API. Send the provided requests.

   Replace <marathon_address> with a valid Marathon address.

   **Mongo**::
 
    curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @mongo.json 

   **SJ-rest**::

    curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @sj-rest.json  

   **tts**::
 
    curl -X POST http://<marathon_address>/v2/apps -H "Content-type: application/json" -d @tts.json 

   Via the Marathon interface make sure the services are deployed.

   Now look and make sure you have access to the Web UI. You will see the platform but it is not completed with any entities yet. 

   In the next section we will show you how to upload engines for your modules, configurations for engines and module validators.

Engine Uploading
""""""""""""""""""""""""""
Before uploading modules, upload the engine jars for them. 

1. You should download the engine jars for each module type (input-streaming, regular-streaming, batch-streaming, output-streaming) and a Mesos framework::

    wget http://c1-ftp1.netpoint-dc.com/sj/1.0-SNAPSHOT/sj-input-streaming-engine.jar
    wget http://c1-ftp1.netpoint-dc.com/sj/1.0-SNAPSHOT/sj-regular-streaming-engine.jar
    wget http://c1-ftp1.netpoint-dc.com/sj/1.0-SNAPSHOT/sj-batch-streaming-engine.jar
    wget http://c1-ftp1.netpoint-dc.com/sj/1.0-SNAPSHOT/sj-output-streaming-engine.jar
    wget http://c1-ftp1.netpoint-dc.com/sj/1.0-SNAPSHOT/sj-mesos-framework.jar
    
   Now upload the engine jars into the platform. Please, replace <slave_advertise_ip> with the Mesos-slave IP and <rest-port> with the SJ-rest service port::

    cd sj-platform
    address=<slave_advertise_ip>:<rest-port>
    
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
  
     {...
     "module-type": "regular-streaming",
     "engine-name": "com.bwsw.regular.streaming.engine",
     "engine-version": "1.0",
     ...}
 
2. Setup configurations for engines.

   The range of configurations includes required and optional ones. 

   The list of all configurations can be viewed at the :ref:`Configuration` page.

   To set up required configurations for the engines, run the following commands. Please, replace:

    - <slave_advertise_ip> with the Mesos-slave IP; 
    - <marathon_address> with the address of Marathon;
    - <rest-port> with the SJ-rest service port.
    
   ::

    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"session-timeout\",\"value\": \"7000\",\"domain\": \"configuration.apache-zookeeper\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"current-framework\",\"value\": \"com.bwsw.fw-1.0\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-host\",\"value\": \"<slave_advertise_ip>\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"crud-rest-port\",\"value\": \"<rest-port>\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect\",\"value\": \"http://<marathon_address>\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"marathon-connect-timeout\",\"value\": \"60000\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"kafka-subscriber-timeout\",\"value\": \"100\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"low-watermark\",\"value\": \"100\",\"domain\": \"configuration.system\"}" 

3. Send the next POST requests to upload configurations for module validators::

    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"input-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.InputInstanceValidator\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"regular-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.RegularInstanceValidator\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"batch-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.BatchInstanceValidator\",\"domain\": \"configuration.system\"}" 
    curl --request POST "http://$address/v1/config/settings" -H 'Content-Type: application/json' --data "{\"name\": \"output-streaming-validator-class\",\"value\": \"com.bwsw.sj.crud.rest.instance.validator.OutputInstanceValidator\",\"domain\": \"configuration.system\"}" 
    
4. You can add optional configuraions if necessary. They have default values in the system but can be overriden. Find the full list of optional configurations at the :ref:`table-optional` table.


Creating Platform Entities
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Under this section you will find the information on platform entities creation.

We will not provide you with specific instructions as this part is custom and the set of platform entities you need for your tasks may differ. Step-by-step instructions on creating entities for example issue solutions are provided in the :ref:`fping-example-task` and :ref:`sflow-example-task` sections of Tutorial.

The following entities should be uploaded or created in the system:

1) Modules;
2) Providers; 
3) Services;
4) Streams;
5) Instances.

Modules
""""""""""

You should create your own modules. Please, use instructions on module creation at :ref:`Custom_Module`.

Then upload modules following the instruction in :ref:`Module_Uploading` of the Tutorial. Use REST API requests to  upload each module (see :ref:`Modules_REST_API`). Replace <module_jar_name> with the name of the module JAR file::

 curl --form jar=@<module_jar_name>.jar http://$address/v1/modules

Or module uploading can be performed via the UI (see :ref:`UI_Modules`).

Providers
""""""""""
Providers are a part of the streaming infrastructure. They can be created using REST API (replace <provider_name> with the name of the provider JSON file)::

 curl --request POST "http://$address/v1/providers" -H 'Content-Type: application/json' --data "@api-json/providers/<provider_name>.json"

For more details see :ref:`REST_Providers`.

Or providers can be created via the UI (see :ref:`UI_Providers`).

Services
""""""""""
Services are a part of the streaming infrastructure. They can be created using REST API (replace <service_name> with the name of the service JSON file)::

 curl --request POST "http://$address/v1/services" -H 'Content-Type: application/json' --data "@api-json/services/<service_name>.json"

For more details see :ref:`REST_Services`.

Or services can be created via the UI (see :ref:`UI_Services`).

Streams
""""""""""
Streams provide data exchange between modules. They can be created using REST API (replace <stream_name> with the name of the stream JSON file)::

 curl --request POST "http://$address/v1/streams" -H 'Content-Type: application/json' --data "@api-json/streams/<stream_name>.json"

For more details see :ref:`REST_Streams`.

Or streams can be created via the UI (see :ref:`UI_Streams`).

Instances
""""""""""

Instances are used with engines to determine their collaborative work with modules. Each module needs an individual instance for it. Its type corresponds to the module type (input-streaming, regular-streaming or batch-streaming, output-streaming). Several instances with different settings can be created for one module to enable different processing scenarios.

Instances can be created using REST API (replace <instance_name> with the name of the instance JSON file)::
 
 curl --request POST "http://$address/v1/modules/input-streaming/pingstation-input/1.0/instance" -H 'Content-Type: application/json' --data "@api-json/instances/<instance_name>.json"

For more details see :ref:`REST_API_Instance`.

Or instances can be created via the UI (see :ref:`UI_Instances`).

To start processing you should launch instances one by one.  Use REST API (see :ref:`REST-API-Start-Instance`) or the Web UI (see :ref:`UI_Instances`) to start processing and monitor the task execution.


Running Pre-built |VirtualBox (TM)| Image
-------------------------------------------------------

Another option to start working with SJ-Platform is to run a pre-built |VirtualBox (TM)| image.

We suggest deploying the platform using Vagrant with VirtualBox® as a provider. This is the most rapid way to run the platform and assess its performance. It takes up to 30 minutes. The platform is deployed with all entities necessary to demonstrate the solution for the example task described in the :ref:`fping-example-task` section.

Minimum system requirements are as follows:

- At least 8 GB of free RAM;

- VT-x enabled in BIOS;

- `Vagrant 1.9.1 <https://www.vagrantup.com/downloads.html>`_ installed;

- `VirtualBox 5.0.40 <https://www.virtualbox.org/>`_ installed.

These requirements are provided for deployment on Ubuntu 16.04 OS.

To determine if CPU VT extensions are enabled in BIOS, do the following:

1) Install CPU-checker::

    $ sudo apt-get update
    $ sudo apt-get install cpu-checker

2) Then check::

    $ kvm-ok

If the CPU is enabled, you will see::

 INFO: /dev/kvm exists
 KVM acceleration can be used

Otherwise, the response will look as presented below::

 INFO: /dev/kvm does not exist
 HINT: sudo modprobe kvm_intel 
 INFO: Your CPU supports KVM extensions
 INFO: KVM (vmx) is disabled by your BIOS
 HINT: Enter your BIOS setup and enable Virtualization Technology (VT),
      and then hard poweroff/poweron your system
 KVM acceleration can NOT be used

Prerequisites
~~~~~~~~~~~~~~~~~~~~~~~

1. At the first step install Vagrant and VirtualBox. 

You can do it following the instructions in the official documentation: 

- `for Vagrant <https://www.vagrantup.com/docs/installation/>`_
- `for VirtualBox <https://www.virtualbox.org/wiki/Downloads>`_

Please, make sure to install the service of the versions specified below:

- Vagrant 1.9.1
- VirtualBox 5.0.40
- Ubuntu 16.04

2. Then, clone the project repository from GitHub::

    $ git clone https://github.com/bwsw/sj-demo-vagrant.git
    $ cd sj-demo-vagrant

Launching Virtual Machine
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To launch Vagrant use the following command::

 $ vagrant up

It will take up to 30 minutes, 8GB memory and 7 CPUs.

.. note:: Please, make sure the ports are available!  In Linux you can use netstat for monitoring network connections and statistics. Run the following command to list all open ports or currently running ports including TCP and UDP::
          
           netstat -lntu

At the end of deploying you can see URLs of all services.

The detailed :ref:`VM_Description` is provided for you to understand the process of virtual machines' creation.

The platform is deployed with the entities: configurations, engines, providers, services, streams. Modules and instances are created as for the :ref:`fping-example-task` described in Tutorial. To launch the data processing follow the instructions provided in the :ref:`fping-Launch-Instances` step of the example task.

Destroying Virtual Machine
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To destroy the virtual machine(s) use::

 $ vagrant destroy
 
Virtual machine(s) will be terminated. 

In case, any problems occur during the deployment, please, open an issue in the project `GitHub repository <https://github.com/bwsw/sj-platform/tree/develop>`_ and let the project team solve it.

.. |VirtualBox (TM)| unicode:: VirtualBox U+00AE
