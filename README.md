[fabrician.org](http://fabrician.org/)
==========================================================================
Silver Fabric Docker Enabler Guide
==========================================================================

Introduction
--------------------------------------
Silver Fabric Docker Enabler  adapts an application process running within a Docker container to be provisioned and orchestrated vy Silver Fabric. 

Supported Platforms
--------------------------------------
* Silver Fabric 5.6.x or above
* Windows, Linux

Installation
--------------------------------------
The Silver fabric Docker Enabler consists of an Enabler Runtime Grid Library only.
The Enabler Runtime contains information specific to a Silver Fabric version that is 
used to integrate the Enabler.
Installation of the Silver Fabric Docker Enabler involves copying the Grid 
Library to the SF_HOME/webapps/livecluster/deploy/resources/gridlib directory on the Silver Fabric Broker. 

Additionally, this Enabler requires the Silver Fabric Engine use Java 1.7. 

Runtime Grid Library
--------------------------------------
The Enabler Runtime Grid Library is created by building the maven project. The build depends on the SilverFabricSDK jar file that is distributed with TIBCO Silver Fabric. 
The SilverFabricSDK.jar file needs to be referenced in the maven pom.xml or it can be placed in the project root directory.

```bash
mvn package
```
The version of the distribution is defaulted to 1.0.0  However, it can be optionally overridden:
```bash
mvn package -Ddistribution.version=1.0.1
```


Overview
--------------------------------------
Docker containers can be viewed from two perspectives:

* As lightweight VMs
* As an application-level packaging mechanism

This Enabler take the latter view where a Docker container is treated as alternative application packaging in addition to Silver Fabric's own "gridlib" application packaging mechanism.
Doing this way allows Silver Fabric to allow application process from both type of packaging to mix and interact as Docker is not suitable for all kinds of application.

Statistics
--------------------------------------
There are 2 kinds of statistic related to a running Docker container:
* Application-level statistics.
This are often via some mechanism like JMX or plain writing to files.

* Container-level statistics.
At the moment, Docker do not have a formal way to extract those statistics via Docker CLI or Remote API.

Regardles, user can user Silver Fabric's scripting statistics to gather statistics of interest by using scripting support in Jython, JRubcy or ECMAScript.

Logs
-----

Runtime Context Variables
--------------------------------------

            