Details of Running Pre-built |VirtualBox (TM)| Image
-----------------------------------------------------

For the first acquaintance with SJ-Platform, we suggest deploying the platform using Vagrant with |VirtualBox (TM)| as a provider. In this case, you use a pre-built |VirtualBox (TM)| image of the platform. So this is the most rapid way to run the platform and assess its performance. It takes up to 30 minutes. The platform is started with all entities necessary to demonstrate the solution for the example task described in the :ref:`fping-example-task` section: configurations, engines, providers, services, streams, modules and instances. 

Requirements:

- At least 8 GB of free RAM.

- VT-x enabled in BIOS.

Below a detailed description is provided on what is happening when running the pre-built |VirtualBox (TM)| image.

.. _VM_Description:

Description
"""""""""""""""""""

After launching Vagrant you will get access to Mesos, Marathon, the SJ-Platform REST API and UI. In the UI you will see all the platform entities created for the `fping demo <http://streamjuggler.readthedocs.io/en/develop/Tutorial.html#fping-example-task>`_ project:

- configurations for modules;
- modules;
- streams with infrastructure (providers, services);
- instances;
- data storage as a resulting data destination.

Vagrant creates Ubuntu/Xenial64 virtual machines with specific parameters:

- Master VM - 2 CPUs, 1GB memory

- Slave1 VM - 2 CPUs, 3GB memory

- Slave2 VM - 1 CPUs, 2GB memory

- Storage VM - 1 CPUs, 512MB memory

- Executor VM - 1 CPUs, 200MB memory

All VMs are launched in the private network: 192.168.50.0

Also, you can use the following command to establish an SSH session into a running virtual machine to get shell access::

 vagrant ssh <name>

Find below the detailed descrition for each virtual machine.

**Master VM**

Virtual machine name is "master". Its hostname is "master".

*Resources*:

- 2 CPUs

- 1 GB memory

- ip = 192.168.50.51

- forwarded ports: 2181, 5050, 8080

*Services*:

- Apache Zookeeper - on port 2181

- Mesos Master - on port 5050

- Marathon - on port 8080

Description:
    After VM is launched, Vagrant installs Docker engine and firstly runs Apache Zookeeper in Docker.
    
    Next, the Mesos-Master service is launched with the following configurations: 
    
    - ip=0.0.0.0, 
    - advertise_ip=192.168.50.51, 
    - hostname=192.168.50.51, 
    - zk=zk://192.168.50.51:2181/mesos.
    
    Next, the Marathon service is launched with the following configurations: 
    
    - hostname=192.168.50.51, 
    - master=zk://192.168.50.51:2181/mesos, 
    - zk=zk://192.168.50.51:2181/marathon.

**Slave1 VM**

Virtual machine name is "slave1". Its hostname is "slave1".

*Resources*:

- 2 CPUs

- 3 GB memory

- ip = 192.168.50.52

- forwarded ports: 5051, 8888, 9092, 7203, 31071, 5601, 9200, 9300

*Services*:

- Mesos-Slave - on port 5051

- Elasticsearch - on ports 9200, 9300

- Kibana - on port 5601

- SJ-rest - on port 8888

- T-streams transaction server - on port 31071

- Apache Kafka - on ports 9092, 7203

Description:
   After VM is launched, Vagrant firstly runs Mesos-Slave with the following configurations: 
   
   - ip = 0.0.0.0, 
   
   - advertise_ip = 192.168.50.52, 
   
   - hostname = 192.168.50.52, 
   
   - zk = zk://192.168.50.51:2181/mesos,
   
   - ports = forwarding ports.

   Next, Docker engine is installed, and Elasticsearch and Kibana are launched in Docker.

**Slave2 VM**

Virtual machine name is "slave2". Its hostname is "slave2".

*Resources*:

- 1 CPUs

- 2 GB memory

- ip = 192.168.50.53

- forwarded ports: 31500 - 31600

*Services*:

- Mesos-Slave

Description:
  After VM is launched, Vagrant firstly launches Mesos-Slave with the following configurations: 
  
  - ip = 0.0.0.0, 
  
  - advertise_ip = 192.168.50.53, 
  
  - hostname = 192.168.50.53, 
  
  - zk = zk://192.168.50.51:2181/mesos, 
  
  - ports = forwarded ports.
  
  Next, Docker engine is installed.

**Storage VM**

Virtual machine name is "storage".

*Resource*:

- 1 CPUs

- 512 MB memory

- ip = 192.168.50.55

- forwarded ports: 27017

*Services*:

- MongoDB

Description:
  After VM is launched, Vagrant firstly installs Docker engine and then launches MongoDB in Docker.

**Executor VM**

Virtual machine name is "executor".

*Resource*:

- 1 CPUs

- 200 MB memory

- ip = 192.168.50.54

Description:
  This VM is used to launch services and create entities.
  
  Once VM is launched, Vagrant firstly launches services on Marathon: SJ-rest, Kafka, tts.
  
  After services are launched, Vagrant creates all entities via SJ-rest.


Here is the full list of addresses to get access to the services:

- 0.0.0.0:8080 - Marathon

- 0.0.0.0:5050 - Mesos Master

- 0.0.0.0:5051 - Mesos Agent

- 0.0.0.0:8888 - SJ REST

- 0.0.0.0:27017 - MongoDB

- 0.0.0.0:2181 - Apache Zookeeper

- 0.0.0.0:9200 - Elasticsearch

- 0.0.0.0:5601 - Kibana

- 0.0.0.0:9092, 0.0.0.0:7203 - Kafka


The platform is deployed with the entities: configurations, engines, providers, services, streams. Modules and instances are created as for the :ref:`fping-example-task` described in Tutorial. To launch the data processing follow the instructions provided in the :ref:`fping-Launch-Instances` step of the example task.

.. Or you can create your own pipeline with modules suitable to achieve your goals. How to create your own module is described `here <http://streamjuggler.readthedocs.io/en/develop/SJ_CustomModule.html>`_ in detail.
.. |VirtualBox (TM)| unicode:: VirtualBox U+00AE
