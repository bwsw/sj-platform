Demo Deployment on Virtual Machine
------------------------------------

.. warning:: The section is under development!

SJ-Platform can be deployed on the virtual machine. We suggest deploying the platform locally via Vagrant with VirtualBox as a provider.
 
This is the most rapid way to get acquainted with the platform and assess its performance.

Requirements:

- At least 7GB of free RAM.

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
-----------------------

At the first step Vagrant and VirtualBox should be installed. 

You can do it following the instructions in the official documentation: 

- `for Vagrant <https://www.vagrantup.com/docs/installation/>`_
- `for VirtualBox <https://www.virtualbox.org/wiki/Downloads>`_

Please, make sure the service versions are as specified below:

1) Vagrant 1.9.1
2) VirtualBox 5.0.40
3) Ubuntu 16.04

Clone the project from the GitHub repository::

 $ git clone https://github.com/bwsw/sj-demo-vagrant.git
 $ cd sj-demo-vagrant

Launch Virtual Machine
------------------------

To launch Vagrant use the following command::

 $ vagrant up

It will take up to half an hour, 8GB memory and 7 CPUs.

At the end of deploying you can see urls of all services.

Description
~~~~~~~~~~~~~~~

Vagrant creates Ubuntu/Xenial64 virtual machine with specific parameters:

- Master VM - 2 CPUs, 1GB memory

- Slave1 VM - 2 CPUs, 3GB memory

- Slave2 VM - 1 CPUs, 2GB memory

- Storage VM - 1 CPUs, 512MB memory

- Executor VM - 1 CPUs, 200MB memory

On virtual machine, Mesos is launched with all the required services on Docker in swarm mode via Docker stack deployment.

Host - 0.0.0.0

List of ports to geo access to the services:

- 8080 - Marathon

- 5050 - Mesos Master

- 5051 - Mesos Agent

- 8888 - SJ REST

- 27017 - MongoDB

- 2181 - Apache Zookeeper

- 9200 - Elasticsearch

- 5601 - Kibana


The platform is deployed with the entities: providers, services, streams, configurations.

Modules and instances are created as for the f-ping-demo project described in :ref:`Tutorial` .

To proceed working with the platform via the UI, please, see the `UI Guide <http://streamjuggler.readthedocs.io/en/develop/SJ_UI_Guide.html>`

Now you can launch the instances, view the statistics of tasks. 

Or create your own pipeline with modules and instances that are suitable to achieve your goals.

How to create your own module is described in detail `here <http://streamjuggler.readthedocs.io/en/develop/SJ_CustomModule.html>`.

Destroy VM
-------------

To destroy virtual machine(-s) created by Vagrant use::

 $ vagrant destroy
 
VM(-s) will be terminated. 
