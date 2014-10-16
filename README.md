[fabrician.org](http://fabrician.org/)
==========================================================================
Silver Fabric Docker Enabler Guide
==========================================================================

Introduction
--------------------------------------
Silver Fabric Docker Enabler adapts a Docker container to be provisioned and orchestrated by TIBCO Silver Fabric. Only useful Docker features are adapted in the context of the normative use of Silver Fabric.
This enabler is based on ***Docker 1.2.0** runtime on Linux64.

Supported Platforms
--------------------------------------
* Silver Fabric 5.7.x or above
* Linux-64 only for Silver Fabric Engine Daemon
* Windows, Linux  for Silver Fabric Broker

Installing the Enabler
--------------------------------------
The Enabler consists of an enabler runtime Grid Library only.
The Enabler Runtime contains information specific to a Silver Fabric version that is used to integrate the Enabler.
Installation of the Silver Fabric Docker Enabler involves copying the Grid 
Library to the **SF_HOME/webapps/livecluster/deploy/resources/gridlib** directory on the Silver Fabric Broker. 

Additionally, this Enabler requires the Silver Fabric Engine to use Java 1.7

Runtime Grid Library
--------------------------------------
The Enabler Runtime Grid Library is created by building the maven project. The build depends on the **SilverFabricSDK.jar** file that is distributed with TIBCO Silver Fabric. 
The SilverFabricSDK.jar file needs to be referenced in the maven pom.xml or it can be placed in the project root directory.

```bash
mvn package
```
The version of the distribution is defaulted to **1.0.0**.  However, it can be optionally overridden like so:
```bash
mvn package -Ddistribution.version=1.0.1
```

Enabling Docker on the Silver Fabric engine daemon host
-------------------------------------------------
Each Silver Fabric engine daemon host needs to be "docker-enabled" before it can be used to build Docker images or run Docker containers. The 3 main steps are:

1. Install ***Docker 1.2.0*** runtime 
    * See [Install Docker] for details
2. Configure ***Password-less sudo*** access to run Docker commands
    * See [Password-less sudo] for details
3. Configure ***Docker Remote API*** to run on port ***2375(HTTP)***
    * See [Configure Docker Remote API] for details
   
After you have done the above 3 steps, you need to restart Silver Fabric engine daemon so that it will register the host as "***Docker-enabled***".

***Note***: Typically, you might want to use CM tools like ***Puppet***, ***Chef***, ***SaltStack*** or ***Ansible***
to configure a significant number of Silver Fabric  Engine Daemon hosts with Docker-enabling steps above

Overview
--------------------------------------
Docker containers can be viewed from two perspectives:

* As lightweight VMs
* As an application-level packaging mechanism

This Enabler take the latter view, where a Docker container is treated as alternative application packaging in addition to Silver Fabric's own "gridlib" application packaging mechanism.
This allows application process from both types of packaging to mix and interact.
While Docker container advocate single process ala **microservice**, on the otherhand, Silver Fabric **gridlib** allows for more complex traditional application. 
So they can both be used in a complementary way.

We also do not support Docker **--link** and **--volumes-from** features to allow instead the use of Silver Fabric's component dependency management to be used to link running processes across different hosts by exporting runtime context variables. For example, a Docker container running on host A could be "linked" to a Silver Fabric grid lib application process running on host by exporting ports and environment variables. Besides allowing an application stack to use components originating from mixed container packaging models, they also overcome's Docker limitation of **same-host** linking only.

Statistics
--------------------------------------
There are 2 kinds of statistics related to a running Docker container:
### Application-level statistics
* This are often read via some standard mechanism like **JMX** or just log to files.

### Container-level statistics
* At the moment, Docker do not have a formal way to extract those statistics via Docker CLI or Remote API.

There are a number of OSS hacks out there but lack of an API means its liable to be broken with Docker runtime evolving. So use it at your own risks.

Regardless, user can use Silver Fabric's statistics scripting support to gather statistics of interest by using **Jython**, **JRuby** or **ECMAScript**.
The script can be used to call JMX or simply read host-to-docker container mounted volumes files.

Note: By default, **Jython 2.52** and **ECMAScript** are enabled in the Silver Fabric Engines.


Logs
-----
* Most application probably logged to a directory within the Docker container. This needs to be exposed to Silver Fabric statistic collection mechanism by mapping an external  host volume to an internal Docker log volume.

* If an application logs to **STD_OUT**, Docker can extract that via the CLI ( see [docker logs]):
```sh
docker logs <container name >
```

Runtime Context Variables
--------------------------------------


How Tos
-------
1. #### How do I map a public host port(ex.9090) to an internal Docker container port(ex.8080)?
```

  First, create an auto-increment "Numeric" runtime context variable of type "String" for host port like  so:

  "MY_PORT=9090"
  
  Next, create a port mapping runtime context variable with name prefixed by "!PORT_MAP_" like so:

  "!PORT_MAP_my_http=${MY_PORT}:8080"
  
  Note: The internal port 8080 will be publically exposed as port 9090,9091, 9092,...depending on the engine     instances the Docker container is managed from.
```

2. #### How do I mount a host directory(ex. "/logs") to an internal Docker container directory(ex. "/my_logs")?

```
  
  First, make sure the host directory is created first.
  Next, create a runtime context variable of type "String" with name prefixed by "!VOL_MAP_" like so:
  
  "!VOL_MAP_logs=/logs:/my_logs:rw"
  where 'rw' can be replaced by 'ro'
  
```

3. #### How do I add one or more environmental variables to the Docker container?

```
  There are 2 ways that you can do that:
  
  (a) Create a runtime context variable of type "String" with a name prefixed by "!ENV_VAR_"
  
  Example : "!ENV_VAR_myvar1=Hello"

  
  (b) Create a runtime context variable of type "String" with a name prefixed by "!ENV_FILE_"
  
  Example: "!ENV_FILE_env1=/env_config/env1.properties" where "env1.properties" holds each line of environment variable in key-value pair format.
  
  Note: There is a already a preconfigured "!ENV_FILE_default" environment files that you can used. Use this
  unless there is a need to add another environment file
  

```

4. #### How do I build and run a Docker file?

```
  Upload your Dockerfile as a content file using relative content   "docker/docker_context" directory as the     target directory for the Dockerfile.
  Then set runtime context variable "BUILD_ALWAYS=true" and "DOCKER_IMAGE_NAME=<namespace>/<app name>:[tag]"
  
  Example: "DOCKER_IMAGE_NAME=bbrets/tomcat:6.0.36"

```



[Install Docker]:https://docs.docker.com/installation/
[Password-less sudo]:https://docs.docker.com/installation/ubuntulinux/#giving-non-root-access
[Configure Docker Remote API]:http://www.virtuallyghetto.com/2014/07/quick-tip-how-to-enable-docker-remote-api.html
[docker logs]:https://docs.docker.com/reference/commandline/cli/#logs

