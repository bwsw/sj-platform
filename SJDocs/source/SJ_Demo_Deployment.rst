Demo Deployment on Virtual Machine
------------------------------------

.. warning:: The section is under development!

SJ-Platform can be deployed on a virtual machine. We suggest deploying the platform locally via Vagrant with VirtualBox as a provider.
 
This is the most rapid way to get acquainted with the platform and assess its performance.

Requirements:

- At least 8 GB of free RAM.

- VT-x must be enabled in BIOS.

To determine if CPU VT extensions are enabled in BIOS, do the following:

1) Install CPU-checker::

    $ sudo apt-get update
    $ sudo apt-get install cpu-checker

2) Then check::

    $ kvm-ok

If the CPU is enabled, you will see::

 INFO: /dev/kvm exists
 KVM acceleration can be used

Otherwise, the respond will look as presented below::

 INFO: /dev/kvm does not exist
 HINT: sudo modprobe kvm_intel 
 INFO: Your CPU supports KVM extensions
 INFO: KVM (vmx) is disabled by your BIOS
 HINT: Enter your BIOS setup and enable Virtualization Technology (VT),
      and then hard poweroff/poweron your system
 KVM acceleration can NOT be used


Deployment
~~~~~~~~~~~~~~~~~~~~~~~

1. At the first step Vagrant and VirtualBox should be installed. 

You can do it following the instructions in the official documentation: 

- `for Vagrant <https://www.vagrantup.com/docs/installation/>`_
- `for VirtualBox <https://www.virtualbox.org/wiki/Downloads>`_

Please, make sure the service versions are as specified below:

- Vagrant 1.9.1
- VirtualBox 5.0.40
- Ubuntu 16.04

2. Then, clone the project from the GitHub repository::

    $ git clone https://github.com/bwsw/sj-demo-vagrant.git
    $ cd sj-demo-vagrant

Launching Virtual Machine
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To launch Vagrant use the following command::

 $ vagrant up

It will take up to 30 minutes, 8GB memory and 7 CPUs.

.. important:: Please, make sure the ports are opened!

At the end of deploying you can see urls of all services.

Description
"""""""""""""""""""

After launching Vagrant you will get the access to Mesos, Marathon, SJ-Platform REST and UI. In the UI you will see all the platform entities created for the `fping demo <http://streamjuggler.readthedocs.io/en/develop/Tutorial.html>`_ project:

- configurations for modules;
- modules;
- streams with infrastructurte (providers, services);
- instances;
- data store as a result destination.

Vagrant creates Ubuntu/Xenial64 virtual machines with specific parameters:

- Master VM - 2 CPUs, 1GB memory

- Slave1 VM - 2 CPUs, 3GB memory

- Slave2 VM - 1 CPUs, 2GB memory

- Storage VM - 1 CPUs, 512MB memory

- Executor VM - 1 CPUs, 200MB memory

All VMs are launched in the private network: 192.168.50.0

Also you can access VM with *vagrant ssh* <name>.

**Master VM**

VM name = master

VM hostname = master

*Resources*:

- 2 CPUs

- 1 GB memory

- ip = 192.168.50.51

- forwarding ports: 2181, 5050, 8080

*Services*:

- Apache Zookeeper - on port 2181

- Mesos Master - on port 5050

- Marathon - on port 8080

Description:
    After VM is launched, Vagrant installs Docker engine and firstly runs Apache Zookeeper in Docker.
    
    Next, Mesos-Master service is launched with the following configurations: 
    
    - ip=0.0.0.0, 
    - advertise_ip=192.168.50.51, 
    - hostname=192.168.50.51, 
    - zk=zk://192.168.50.51:2181/mesos.
    
    Next, Marathon service is launched with the following configurations: 
    
    - hostname=192.168.50.51, 
    - master=zk://192.168.50.51:2181/mesos, 
    - zk=zk://192.168.50.51:2181/marathon.

**Slave1 VM**

VM name = slave1

VM hostname = slave1

*Resources*:

- 2 CPUs

- 3 GB memory

- ip = 192.168.50.52

- forwarding ports: 5051, 8888, 9092, 7203, 31071, 5601, 9200, 9300

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

VM name = slave2

VM hostname = slave2

*Resources*:

- 1 CPUs

- 2 GB memory

- ip = 192.168.50.53

- forwarding ports: 31500 - 31600

*Services*:

- Mesos-Slave

Description:
  After VM is launched, Vagrant firstly launches Mesos-Slave with the following configurations: 
  
  - ip = 0.0.0.0, 
  
  - advertise_ip = 192.168.50.53, 
  
  - hostname = 192.168.50.53, 
  
  - zk = zk://192.168.50.51:2181/mesos, 
  
  - ports = forwarding ports.
  
  Next, Docker engine is installed.

**Storage VM**

VM name = storage

*Resource*:

- 1 CPUs

- 512 MB memory

- ip = 192.168.50.55

- forwarding ports: 27017

*Srevices*:

- MongoDB

Description:

After VM is launched, Vagrant firstly installs Docker engine and then launches MongoDB in Docker.

**Executor VM**

VM name = executor

*Resource*:

- 1 CPUs

- 200 MB memory

- ip = 192.168.50.54

Description:
  This VM is used to launch services and create entities.
  
  Once VM is launched, Vagrant firstly launches services on Marathon: SJ-rest, Kafka, tts.
  
  After services are launched, Vagrant creates all entities via SJ-rest.


A full list of ports to get access to the services:

- 8080 - Marathon

- 5050 - Mesos Master

- 5051 - Mesos Agent

- 8888 - SJ REST

- 27017 - MongoDB

- 2181 - Apache Zookeeper

- 9200 - Elasticsearch

- 5601 - Kibana

- 9092,7203 - Kafka

Use local host - 0.0.0.0


The platform is deployed with the entities: providers, services, streams, configurations.

Modules and instances are created as for the f-ping-demo project described in :ref:`Tutorial` .

To proceed working with the platform via the UI, please, see the `UI Guide <http://streamjuggler.readthedocs.io/en/develop/SJ_UI_Guide.html>`_ .

Now you can launch the instances, view the statistics of task execution in the UI. 

Or you are enabled to create your own pipeline with modules and instances that are suitable to achieve your goals.

How to create your own module is described in detail `here <http://streamjuggler.readthedocs.io/en/develop/SJ_CustomModule.html>`_ .

Destroying Virtual Machine
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To destroy the virtual machine(s) use::

 $ vagrant destroy
 
VMs will be terminated. 
