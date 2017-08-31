Platform Deployment
================================

This section provides the information on Stream Juggler Platform deployment. 

Below, two ways of deployment are described. Please, read the requirements for each of them and decide what option is more suitable for your aims.

Deployment on Cluster
---------------------------

The `deployment guide <http://streamjuggler.readthedocs.io/en/develop/SJ_Deployment.html>`_ provides a detailed step-by-step instruction on Stream Juggler Platform deployment on a cluster. 

Currently, the deployment on `Apache Mesos <http://mesos.apache.org/>`_ as a universal distributed computational engine is supported.

Another option is to deploy SJ-Platform in a local mode using `minimesos <https://www.minimesos.org/>`_ as a testing environment.

The following technical requirements should be met:

- working Linux host with 4-8 GB of RAM and 4 CPU cores; 
- Docker installed.  

The platform is deployed with no entities. Thus, the pipeline can be structured from scratch. The entities can be added to the platform via `REST API <http://streamjuggler.readthedocs.io/en/develop/SJ_CRUD_REST_API.html>`_ or `the UI <http://streamjuggler.readthedocs.io/en/develop/SJ_UI_Guide.html>`_ .

Deployment on Virtual Machine
----------------------------------

SJ-Platform can be deployed on a virtual machine. We suggest deploying the platform locally via Vagrant with VirtualBox as a provider.

This is the most rapid way to get acquainted with the platform and assess its performance. It takes up to 30 minutes. The platform is deployed with all entities necessary to demonstrate the solution for the example task: providers, services, streams, configurations.

All that's needed is:

- At least 8 GB of free RAM;

- VT-x must be enabled in BIOS;

- Vagrant 1.9.1 installed;

- VirtualBox 5.0.40 installed.

These requirements are provided for deployment on Ubuntu 16.04 OS.

Please, find detailed instruction in `the guide on deployment on the virtual machine <http://streamjuggler.readthedocs.io/en/develop/SJ_Demo_Deployment.html>`_ .

In case, any problems occur during the deployment, please, open an issue in the project `GitHub repository <https://github.com/bwsw/sj-platform/tree/develop>`_ and let the project team solve it.
