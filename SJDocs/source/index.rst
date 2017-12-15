.. Test Branch documentation master file, created by
   sphinx-quickstart on Thu Jun  8 13:48:55 2017.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.


.. figure:: _static/Sj.svg

Welcome to Stream Juggler Platform 1.1.1 Documentation!
=========================================================

The Stream Juggler Platform (SJ-Platform) is an open source, scalable solution for stream and micro-batched processing. The system fits for building both simple and complex event processing systems (CEP) and allows a developer to construct pipelines for analyzing data streams.

The Stream Juggler Platform uses Apache Mesos, Apache Kafka and T-streams to construct scalable and flexible processing algorithms. It enables exactly-once processing and provides an integrated solution with a RESTful interface, JavaScript UI and an ad hoc repository for modules, services, streams and other data processing pipeline components.

Thus, the SJ-Platform is a system that allows high-throughput, fault-tolerant stream processing of live data streams. Data can be ingested from different sources like Apache Kafka, or via TCP connections, and can be processed using complex algorithms. Finally, processed data can be pushed out to filesystems, external databases.

The documentation presented here corresponds to SJ-Platform Release 1.1.1. It gives a complete understanding of the system, its components, basic features fulfilled in it. 

Tutorial
--------------

A detailed :ref:`Tutorial` provides real-life example tasks resolved with SJ-Platform as well as detailed platform deployment instructions. 

.. toctree::
    :hidden:

    Tutorial

Creating Modules
---------------------

The :ref:`Custom_Module` section explains how to write a module using a simple hello-world example. 

.. toctree::
    :hidden:

    SJ_CustomModule

API and UI documentation
-------------------------------

SJ-Platform provides REST API and Web UI for users to easily manage the platform. The guides for the Web UI and the CRUD REST API may be of utmost importance for platform administrators. 

.. toctree::
    :hidden:

    SJ_UI_Guide
    SJ_CRUD_REST_API

**SJ-Platform 1.1.1** documentation general structure:

.. toctree::
   :maxdepth: 2

   SJ_Overview
   Tutorial
   SJ_Architecture
   SJ_Modules
   SJ_Engines
   SJ_CustomModule
   SJ_Simulators
   SJ_Streaming
   SJ_PlatformDeployment
   SJ_UI_Guide
   SJ_CRUD_REST_API
   Glossary
   
   
   
   
 

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`

