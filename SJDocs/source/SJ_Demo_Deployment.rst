Demo Deployment on Virtual Machine
------------------------------------

.. warning:: The section is under development!

SJ-Platform can be deployed on the virtual machine. We suggest deployin the platform loccaly via Vagrant with VirtualBox as a provider.

Requirements:

- At least 7GB free ram.

- VT-x must be enabled in bios.

To determine if cpu vt extensions are enabled in bios, do the following:

1) Install cpu-checker::

 $ sudo apt-get update
 $ sudo apt-get install cpu-checker

2) Then check::

 $ kvm-ok

If the CPU is enabled, you will see::

 INFO: /dev/kvm exists
 KVM acceleration can be used

Otherwise, the respond will be::

 INFO: /dev/kvm does not exist
 HINT:   sudo modprobe kvm_intel 
 INFO: Your CPU supports KVM extensions
 INFO: KVM (vmx) is disabled by your BIOS
 HINT: Enter your BIOS setup and enable Virtualization Technology (VT),
      and then hard poweroff/poweron your system
 KVM acceleration can NOT be used


Deployment
-----------------------

At first install Vagrant and Virtualbox. 

You can do it following official instructions: 

- `for Vagrant <https://www.vagrantup.com/docs/installation/>`_
- `for VirtualBox <https://www.virtualbox.org/wiki/Downloads>`_

Check the versions of the services:

1) Vagrant 1.9.1
2) VirtualBox 5.0.40
3) Ubuntu 16.04

Clone the project from the GitHub repository::

 git clone https://github.com/bwsw/sj-demo-vagrant.git
 cd sj-demo-vagrant

Launch vagrant::

 vagrant up

It takes up to two and a half hours, 10GB mem and 4 cpu

At the end of deploying you can see urls of all services.

To destroy vagrant use::

 vagrant destroy

Also, you can turn VM off and after a while turn it on again, without destroying. All services will work.

Description

Vagrant creates Ubuntu/Xenial64 VM with 4 cpus, 12 GB memory and 10 GB disk space.
In VM there is a launched Mesos with all required services on docker swarm via docker stack deploy.

List of used ports:

8080 - Marathon
5050 - Master
5051 - Agent
8888 - SJ Rest
27017 - Mongo
2181 - Zookeeper
9200 - Elasticsearch
5601 - Kibana

Host - 0.0.0.0

The platform is deployed with the entities: providers, services, streams, configurations.

Modules and instances are created as for the f-ping-demo project described in :ref:`Tutorial`