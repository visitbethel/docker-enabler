[fabrician.org](http://fabrician.org/)
==========================================================================
Silver Fabric Docker Enabler Guide
==========================================================================

Introduction
--------------------------------------
Silver Fabric Docker Enabler adapts a Docker container to be provisioned and orchestrated by TIBCO Silver Fabric. Only useful Docker features are adapted in the context of the normative use of Silver Fabric.
This enabler is based on **Docker 1.2.0** runtime on Linux64.

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
2. Configure ***Password-less sudo*** access to run Docker CLI commands and Remote API
    * See [Password-less sudo] for details
    * If sudo is not required, the password-less requirement still holds
3. Configure ***Docker Remote API*** to run a TCP port
    * See [Configure Docker Remote API] for details
    * The Remote API daemon needs to be acessible only via address ***127.0.0.1 on port 2375(HTTP)***
   
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

* If an application logs to **STDOUT**, Docker can extract that via the CLI ( see [docker logs]):
```sh
docker logs <container name >
```

Runtime Context Variables
--------------------------------------
The runtime context variables for this enabler are classified into 4 categories:

***A. Operations-related***

Variable Name|Default value|Type|Description|Export|Auto Increment
---|---|---|---|---|---
**USE_SUDO**|false|Environment|Run Docker with sudo. The sudo must not prompt for password!|false|None
**JDK_NAME**|j2sdk|String|The name of the required JDK|false|None
**JDK_VERSION**|1.7|String|The version of the required JDK|false|None
**JAVA_HOME**|${GRIDLIB_JAVA_HOME}|Environment|The Java home directory|false|None
**DELETE_RUNTIME_DIR_ON_SHUTDOWN**|true|Environment|Whether to delete the Docker runtime directory on shutdown. This includes removing the Docker container.|false|None
**DOCKER_BASE_DIR**|${container.getWorkDir()}/docker|Environment|Base parent dir containing Dockerfile build context dir, logs dir and stats dir|false|None
**DOCKER_CONTEXT_DIR**|${DOCKER_BASE_DIR}/docker_context|Environment|Dir containing the Dockerfile and associated dirs and files to be used in image build.|false|None
**DOCKER_LOGS_DIR**|${DOCKER_BASE_DIR}/docker_logs|Environment|Host dir mounted for dumping any logs data from within Docker containers|false|None
**DOCKER_STATS_DIR**|${DOCKER_BASE_DIR}/docker_stats|Environment|Host dir mounted for dumping any stats data from within Docker containers|false|None
**DOCKER_ENVS_DIR**|${DOCKER_BASE_DIR}/docker_envs|Environment|Host dir where environment properties files for container are located|false|None
**HTTP_PORT**|9090|Environment|HTTP listen port|false|Numeric
**HTTPS_PORT**|9443|Environment|HTTPS listen port|false|Numeric

***B. Dockerfile build-related***

Variable Name|Default value|Type|Description|Export|Auto Increment
---|---|---|---|---|---
**DOCKER_IMAGE_NAME**|joe/app|Environment|Docker image name to generate or use for container creation. ex. 'joe/archiva:211'|false|None
**BUILD_ALWAYS**|false|String|Always attempt a Dockerfile build first before running a container.|false|None
**REUSE_IMAGE**|true|String|Skip build and reuse image if it already exist.|false|None
**BUILD_VERBOSE**|true|String|Emit verbose build steps when building image.|false|None
**USE_CACHE**|true|String|Use existing build cache to speed up build|false|None
**REMOVE_SUCCESS**|true|String|Only remove any build intermediate containers if final build is successful.|false|None
**REMOVE_ALWAYS**|false|String|Always remove any build intermediate containers, even if final build failed.|false|None
**BUILD_TIMEOUT**|200|String|Max number of secs before build is terminated and failed.|false|None

***C. Docker container-related***

Variable Name|Default value|Type|Description|Export|Auto Increment
---|---|---|---|---|---
**DOCKER_CONTAINER_NAME**|my_container|Environment|Base name of the container, with instances of container having same base name prefixed by engine instance id. Ex. 'my_container1','my_container2',etc|false|Append
**REUSE_CONTAINER**|false|String|Reuse existing same named container instead of creating a new one|false|None
**PRIVILEDGED_MODE**|false|String|Set the container to run in privileged mode|false|None
**CMD_OVERRIDE**||Environment|Command executable (and any of its arguments) to run in a container that result in a foreground process. Note: If the image also specifies an 'ENTRYPOINT' then this get appended as arguments to the ENTRYPOINT.|false|None
**ENTRY_POINT_OVERRIDE**||String|Overrides default executable(usually '/bin/bash') to run when container starts up. Use this in conjunction with 'CMD_OVERRIDE'|false|None
**USER_OVERRIDE**||String|Overrides default user('root', uid=0) within a container when it starts up. Use Username or UID|false|None
**WORKDIR_OVERRIDE**||String|Overrides default working dir inside Docker container|false|None
**!ENV_FILE_Default**|${DOCKER_ENVS_DIR}/envs.properties|String|An properties file containing environment variables to be injected into container. This may override some or all 'ENV' already set in the image|false|None
**MEMORY_LIMIT**|256m|String|Upper limit to container RAM memory in the format NNNx where NNN is an integer and x is the unit(b,k,m, or g). ex. 256m|false|None
**MAX_STOP_TIME_BEFORE_KILL**|30|Environment|Maxiumum secs to wait before a force stop is used to shutdown a Docker container|false|None
**CID_FILE**|${DOCKER_BASE_DIR}/${DOCKER_CONTAINER_NAME}.cid|Environment|A file that is created when a Docker container is created and run.|false|None
**BIND_ON_ALL_LOCAL_ADDRESSES**|false|Environment|Specify if all network interfaces should be bounded for all public port access|false|None
**LISTEN_ADDRESS_NET_MASK**||Environment|A comma delimited list of net masks in CIDR notation. The first IP address found that matches one of the net masks is used as the listen address. Note that BIND_ON_ALL_LOCAL_ADDRESSES overrides this setting.|false|None

Configuring Silver Fabric Engine Resource Preference
-----------------------------------------------------

Since not all Silver Fabric daemon engine hosts(physical or virtual) are Docker-enabled, a [Resource Preference rule] needs to be set when configuring a Silver Fabric component from this enabler.
This allows Silver Fabric Broker to deploy component to the right engine hosts that are Docker-enabled; otherwise the component deployment will fail.

Special runtime context variable name directives
------------------------------------------------

Runtime context variables names that are prefixed with [Special directives] allow them to be treated differently.

Prefix directive|Purpose|Variable name syntax|Variable value
---|---|---|---
**!PORT_MAP_** | Map an external host port to an internally exposed Docker container port|!PORT_MAP_xxxx|\<external port\>:\<internal port\>
**!VOL_MAP_**| Mount an external host volume to an internal Docker container volume|!VOL_MAP_xxxx|\<external volume path\>:\<internal volume path\>:[rw,ro]
**!ENV_VAR_**| Inject an environment variable into the Docker container|!ENV_VAR_xxxx|key=value, key=, key
**!ENV_FILE_**| Inject a list of environment variables specified as `key=value` pairs from a file into the Docker container|!ENV_FILE_xxxx|\<path to a file\>

Silver Fabric Engine activation info from Docker container
----------------------------------------------------------
[Container-related metadata] info are collected as activation info by the engine that proxy the lifecycle of the associated Docker it manages. They are prefixed by **docker_**.

How Tos
-------
 ***(1) How do I map a public host port(ex.9090) to an internal Docker container port(ex.8080)?***
```

  First, create an auto-increment "Numeric" runtime context variable of type "String" for host port like  so:

  "MY_PORT=9090"
  
  Next, create a port mapping runtime context variable with name prefixed by "!PORT_MAP_" like so:

  "!PORT_MAP_my_http=${MY_PORT}:8080"
  
  Note: The internal port 8080 will be publically exposed as port 9090,9091, 9092,...depending on the engine     instances the Docker container is managed from.
```

 ***(2) How do I mount a host directory(ex. "/logs") to an internal Docker container directory(ex. "/my_logs")?***

```
  
  First, make sure the host directory is created first.
  Next, create a runtime context variable of type "String" with name prefixed by "!VOL_MAP_" like so:
  
  "!VOL_MAP_logs=/logs:/my_logs:rw"
  where 'rw' can be replaced by 'ro'
  
```

 ***(3) How do I add one or more environmental variables to the Docker container?***

```
  There are 2 ways that you can do that:
  
  (a) Create a runtime context variable of type "String" with a name prefixed by "!ENV_VAR_"
  
  Example : "!ENV_VAR_myvar1=Hello"

  
  (b) Create a runtime context variable of type "String" with a name prefixed by "!ENV_FILE_"
  
  Example: "!ENV_FILE_env1=/env_config/env1.properties" where "env1.properties" holds each line of environment variable in key-value pair format.
  
  Note: There is a already a preconfigured "!ENV_FILE_default" environment files that you can used. Use this
  unless there is a need to add another environment file
  

```

  ***(4) How do I build and run a Docker file?***

```
  Upload your Dockerfile as a content file using relative content   "docker/docker_context" directory as the     target directory for the Dockerfile.
  Then set runtime context variable "BUILD_ALWAYS=true" and "DOCKER_IMAGE_NAME=<namespace>/<app name>:[tag]"
  
  Example: "DOCKER_IMAGE_NAME=bbrets/tomcat:6.0.36"

```



[Install Docker]:https://docs.docker.com/installation/
[Password-less sudo]:https://docs.docker.com/installation/ubuntulinux/#giving-non-root-access
[Configure Docker Remote API]:http://www.virtuallyghetto.com/2014/07/quick-tip-how-to-enable-docker-remote-api.html
[docker logs]:https://docs.docker.com/reference/commandline/cli/#logs
[Resource Preference rule]:https://raw.githubusercontent.com/fabrician/docker-enabler/master/src/main/resources/images/docker_resource_preference.gif?token=3927123__eyJzY29wZSI6IlJhd0Jsb2I6ZmFicmljaWFuL2RvY2tlci1lbmFibGVyL21hc3Rlci9zcmMvbWFpbi9yZXNvdXJjZXMvaW1hZ2VzL2RvY2tlcl9yZXNvdXJjZV9wcmVmZXJlbmNlLmdpZiIsImV4cGlyZXMiOjE0MTQwOTcyNzZ9--64b834ab47f3c8ad295791d3d0e21d307aebec15

[Special directives]:https://raw.githubusercontent.com/fabrician/docker-enabler/master/src/main/resources/images/docker_runtime_context_vars.gif?token=3927123__eyJzY29wZSI6IlJhd0Jsb2I6ZmFicmljaWFuL2RvY2tlci1lbmFibGVyL21hc3Rlci9zcmMvbWFpbi9yZXNvdXJjZXMvaW1hZ2VzL2RvY2tlcl9ydW50aW1lX2NvbnRleHRfdmFycy5naWYiLCJleHBpcmVzIjoxNDE0MTAxMTE3fQ%3D%3D--ceccc95a0b64f88fc0c0e34039d4a6222d2061c2

[Container-related metadata]:https://raw.githubusercontent.com/fabrician/docker-enabler/master/src/main/resources/images/docker_enabler_activationInfo.gif?token=3927123__eyJzY29wZSI6IlJhd0Jsb2I6ZmFicmljaWFuL2RvY2tlci1lbmFibGVyL21hc3Rlci9zcmMvbWFpbi9yZXNvdXJjZXMvaW1hZ2VzL2RvY2tlcl9lbmFibGVyX2FjdGl2YXRpb25JbmZvLmdpZiIsImV4cGlyZXMiOjE0MTQxMTA1MTF9--39c9c921946525e0751d98494508b4e9dda263c4

