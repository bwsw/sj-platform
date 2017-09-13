.. Test Branch documentation master file, created by
   sphinx-quickstart on Thu Jun  8 13:48:55 2017.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

Welcome to Stream Juggler Platform 1.1.1!
===========================================

.. figure:: _static/logo.png

The Stream Juggler Platform (SJ-Platform) is an open source, scalable solution for real-time and batched stream processing. The system fits for building both simple and complex event processing systems (CEP) and allows a developer to construct pipelines for analyzing data streams.

The Stream Juggler Platform uses Apache Mesos, Kafka and T-streams to construct scalable and flexible processing algorithms. It enables exactly-once processing and provides an integrated solution with a RESTful interface, JavaScript UI and an ad hoc repository for modules, services, streams and other data processing pipeline components.

Thus, the SJ-Platform is a platform that allows high-throughput, fault-tolerant stream processing of live data streams. Data can be ingested from different sources like Kafka, or TCP sockets, and can be processed using complex algorithms. Finally, processed data can be pushed out to filesystems, external databases.

The documentation presented here is for SJ-Platform Release 1.1.1. It gives a complete understanding of the platform, its components, basic features fulfilled in it. A detailed :ref:`Tutorial` provides a real-life example task  resolved with SJ-Platform as well as detailed platform deployment instructions. A custom module development guide explains how to write a module using a simple hello-world example. The guides for the Web UI and the CRUD REST API may be of utmost importance for platform administrators. 

**SJ-Platform 1.1.1** Documentation structure:

.. toctree::
   :maxdepth: 2

   SJ_Overview
   Tutorial
   SJ_Modules
   SJ_Engines
   SJ_CustomModule
   SJ_Simulators
   SJ_Architecture
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

