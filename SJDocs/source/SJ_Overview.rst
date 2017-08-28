Stream Juggler Platform Overview
================================

Introduction to Stream Juggler Platform
----------------------------

Stream Juggler Platform (**SJ-Platform**) is an open source, scalable solution for real-time and micro-batched unbounded streams processing. The system fits for building an event processing systems and allows a developer to construct connected pipelines for analyzing data streams. **Stream** is unbound sequence of events processed sequentially from the oldest ones to newest ones. SJ-Platform is built to be smooth and easy to understand and learn for an average developer who knows Scala language. The system doesn't require any specific knowledge of mathematical concepts like some competing require. Thus, it makes it easy for a common type engineer to solve stream processing tasks.

Basically, SJ-Platform is inspired by `Apache Samza <http://samza.apache.org/>`_, but has a lot of features which Samza doesn't provide, like exactly-once processing capability, integrated RESTful API and Web UI and lots of others.

Stream Processing Systems are widely used in the modern world. There are a lot of cases where developers should use stream processing systems. All those cases usually involve large data streams which can not be handled by a single computer effectively, specific requirements are applied to computations like processing idempotence, exactly-once processing and predictable behavior in case of crashes. Every stream processing platform is a framework which enforces certain code restrictions guaranteeing that the processing is stable and results are reproducible if a developer follows the restrictions.

There are many systems which can compete today - Apache Spark, Apache Kafka, Apache Flink, Apache Storm are the most famous. Every system has its own strengths and weaknesses, making it better or worse for certain cases. SJ-Platform also has such features. But we developed it to be universal and convenient for a broad range of tasks. We hope the features of SJ-Platform make developers solve and support tasks faster and system engineers operate clusters easily. 

Every stream processing system must be fast and scalable. This is the most important requirement. SJ-Platform is fast and scalable as well. It is written in `Scala <https://www.scala-lang.org/>`_ language - well known JVM language which is fast and provides an access to lots of open-source libraries and frameworks. On the other hand, horizontal scaling is vital for stream processing systems which require the capability to distribute computations between compute nodes. SJ-Platform achieves that goal with the help of well-known distributed scheduler system - Apache Mesos.

SJ-Platform stands on shoulders of well-known technologies which simplify the deployment and operation and support best industrial practices. Core SJ-Platform technologies are mentioned in the following list:

1. `Apache Mesos <http://mesos.apache.org>`_ - universal distributed computational engine;
2. `Apache Zookeeper <http://zookeeper.apache.org>`_ - distributed configuration and coordination broker;
3. `Apache Kafka <http://kafka.apache.org>`_ - high-performance message broker;
4. `Mesosphere Marathon <https://mesosphere.github.io/marathon/>`_ - universal framework for executing tasks on Mesos;
5. `MongoDB <https://www.mongodb.com/>`_ - highly available document database;
6. `Hazelcast <https://hazelcast.com/>`_ - leading in-memory grid.

Further, in the documentation, we explain how, why and when technologies mentioned above are used in the system.

Documentation Structure
-------------------------------
The documentation is organized in two big parts. The first one is a tutorial part which guides a reader through the system in a way which motivates him/her to observe, try and explain every step in practice. The second part is a referential one, which explains specific topics and lists system specifications for administrative and programming API, RESTful interface and Web UI.

Preliminary Requirements to The Reader
-------------------------------
SJ-Platform is a quite complex system, but the tutorial tries to guide the reader as smooth as possible. So, there is quite a small amount of requirements to a reader. To achieve the success the reader must have knowledge of:

1. scala programming language and generic data structures;
2. basics of Docker.

Also, the reader should have working Linux host with 4-8GB of RAM and 4 CPU cores with Docker installed (in the tutorial the installation of Docker for Ubuntu 16.04 OS will be explained).

In the tutorial we will demonstrate the functionality of SJ-Platform and train the reader to develop the modules for it using the example problem, which is listed further:

TODO PROBLEM DESCRIPTION

The problem is not a case of a "heavy" task but it includes some problems which are very specific to stream processing tasks and introduces all SJ-Platform functionality step-by-step without deep knowledge requirements of specific problem domains.

Short Features List for Impatient
-------------------------------
Major features implemented in SJ-Platform are listed in the following list:

**Processes data exactly-once**. This is a very critical requirement which is important for many systems. SJ-Platform supports exactly-once mode across the pipeline.

**Two kinds of processing - per-event and micro-batch**. These modes are widely used and cover requirements of all stream processing tasks.

**Stateful and stateless processing**. Developers can use special state management API implementing their algorithms. That API supports resilient and reliable state behavior during system failures and crashes.

**Distributed synchronized data processing**. Micro-batch processing mode provides developers with the capability to synchronize computations across the nodes which is sometimes required.

**Custom context-based batching methods**. Micro-batch processing mode provides developers with API to implement custom algorithms to determine batch completeness which is important feature required in many real-life tasks.

**Use of Apache Kafka, T-streams or TCP as an input source of events**. External systems feed SJ-Platform with events via a list of supported interfaces. Right now it supports several of them. 

The first is **TCP**. The method allows developers design custom protocol to receive events from external systems, deduplicate them and place into processing pipeline.

The second is **Apache Kafka**. Apache Kafka is the de-facto standard for message queueing, so we support it in SJ-Platform providing 3rd party applications with common integration interface.

The third is **T-streams**. T-streams is Kafka-like message broker which is native to SJ-Platform and is used as internal data exchange bus inside the system.

**JDBC/Elasticsearch/RESTful interface as an output destination for processing data**. Processed data are exported to JDBC-compatible database, Elasticsearch or RESTful interface.

**Performance metrics**. SJ-Platform supports embedded performance metrics which help system managers to observe the runtime performance of the system.

**Extensive simulator development framework**. SJ-Platform provides developers with special "mock" infrastructure which helps to develop and test modules without actual deployment to the runtime.

These features will be explained in the documentation in depth.


To find more about the platform, please, visit the pages below:

:ref:`Tutorial` - a quick example to demonstrate the platform in action.

:ref:`Architecture` - here the architecture of the Stream Juggler Platform is presented, its components, connections between them, necessary services and other prerequisites for the Platform operation are described.

:ref:`Modules` - here more information on modules is given: what module types are supported in Stream Juggler Platform, how they work, etc.

:ref:`REST_API` - the REST API service is described here to work with the platform without the UI.

:ref:`UI_Guide` - the section is devoted to the UI and its basic features to configure and monitor the platform.
