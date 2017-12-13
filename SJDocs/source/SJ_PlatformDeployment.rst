.. _Platform_Deployment:

Platform Deployment
================================

This section provides the information on Stream Juggler Platform deployment. 

Below, two ways of deployment are described. Please, read the requirements for each of them and decide what option is more suitable for your aims.

SJ-Platform Deployment on Cluster
---------------------------------

The first option is to deploy SJ-Platform on a cluster. 

The `deployment guide <http://streamjuggler.readthedocs.io/en/develop/SJ_Deployment.html>`_ provides a detailed step-by-step instruction on Stream Juggler Platform deployment on a cluster. 

Currently, the deployment on `Apache Mesos <http://mesos.apache.org/>`_ as a universal distributed computational engine is supported.

.. Another option is to deploy SJ-Platform in a local mode using `minimesos <https://www.minimesos.org/>`_ as a testing environment.

Minimum system requirements are as follows:

- working Linux host with 4-8 GB of RAM and 4 CPU cores; 
- Docker v17.03 installed.  

The platform is deployed with no entities. Thus, the pipeline can be structured from scratch. The entities can be added to the platform via `REST API <http://streamjuggler.readthedocs.io/en/develop/SJ_CRUD_REST_API.html>`_ or `the UI <http://streamjuggler.readthedocs.io/en/develop/SJ_UI_Guide.html>`_ .

Running Pre-built |VirtualBox (TM)| Image
-------------------------------------------------------

Another option to start SJ-Platform is to run a pre-built |VirtualBox (TM)| image.

Please, find detailed instructions in `the guide <http://streamjuggler.readthedocs.io/en/develop/SJ_Demo_Deployment.html>`_ on running the pre-built |VirtualBox (TM)| image of SJ-Platform.

We suggest deploying the platform using Vagrant with VirtualBox® as a provider. This is the most rapid way to run the platform and assess its performance. It takes up to 30 minutes. The platform is deployed with all entities necessary to demonstrate the solution for the example task described in the :ref:`fping-example-task` section: providers, services, streams, configurations, modules and instances. 

Minimum system requirements are as follows:

- At least 8 GB of free RAM;

- VT-x enabled in BIOS;

- `Vagrant 1.9.1 <https://www.vagrantup.com/downloads.html>`_ installed;

- `VirtualBox 5.0.40 <https://www.virtualbox.org/>`_ installed.

These requirements are provided for deployment on Ubuntu 16.04 OS.



In case, any problems occur during the deployment, please, open an issue in the project `GitHub repository <https://github.com/bwsw/sj-platform/tree/develop>`_ and let the project team solve it.

.. |VirtualBox (TM)| unicode:: VirtualBox U+00AE
