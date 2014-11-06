[fabrician.org](http://fabrician.org/)
==========================================================================
Silver Fabric Docker Enabler Guide
==========================================================================

Introduction
--------------------------------------
`Silver Fabric Docker Enabler` adapts a Docker container to be provisioned and orchestrated by `TIBCO Silver Fabric`. Only useful Docker features are adapted in the context of the normative use of Silver Fabric.
This enabler is based on **Docker 1.2.0, 1.3.0** runtime on Linux64.

Supported Platforms
--------------------------------------
* Silver Fabric 5.7.x or above
* Linux-64 only for Silver Fabric Engine Daemon
* Windows, Linux  for Silver Fabric Broker

Supported Docker versions
-------------------------
* 1.2.0
* 1.3.0

Installing the Enabler
--------------------------------------
The Enabler consists of an enabler runtime `Grid Library` only.
The Enabler Runtime contains information specific to a Silver Fabric version that is used to integrate the Enabler.
Installation of the Silver Fabric Docker Enabler involves copying the Grid 
Library to the **SF_HOME/webapps/livecluster/deploy/resources/gridlib** directory on the Silver Fabric Broker. 

Additionally, this Enabler requires the Silver Fabric Engine to use Java 1.7

Runtime Grid Library
--------------------------------------
The Enabler Runtime Grid Library is created by building the maven project. The build depends on the **SilverFabricSDK.jar** file that is distributed with `TIBCO Silver Fabric SDK API`. 
The `SilverFabricSDK.jar` file needs to be referenced in the maven pom.xml or it can be placed in the project root directory.

```bash
mvn package
```
The version of the distribution is defaulted to **1.0.0**.  However, it can be optionally overridden like so:
```bash
mvn package -Ddistribution.version=1.0.1
```

Enabling Docker on the Silver Fabric engine daemon host
--------------------------------------------------------
Each Silver Fabric engine daemon host needs to be "docker-enabled" before it can be used to build Docker images or run Docker containers. The 3 main steps are:

1. Install ***Docker 1.2.0*** or ***Docker 1.3.0*** runtime 
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

This Enabler take the latter view, where a Docker container is treated as alternative application packaging in addition to Silver Fabric's own `gridlib` application packaging mechanism.
This allows application process from both types of packaging to mix and interact.
While Docker container advocate single process ala `microservice`, on the otherhand, Silver Fabric `gridlib` allows for more complex traditional application. 
So they can both be used in a complementary way.

We also do not support Docker `--link` and `--volumes-from` features to allow instead the use of Silver Fabric's `component dependency management` to be used to link running processes across different hosts by `exporting runtime context variables`. For example, a Docker container running on host A could be "linked" to a Silver Fabric gridlib application process running on host B by exporting ports and environment variables. Besides allowing an application stack to use components originating from mixed container packaging models, they also overcome's Docker limitation of **same-host** linking only.

Since a Docker container is in essence a self-contained `blackbox`, Silver Fabric functionality and features can only be binded on to the container indirectly via scripting support.

Docker container instantiation limit
-------------------------------------
Though Docker containers are lightweight and can be spun up fast, provisioning multiple containers on a single `Docker host`, will eventually run into limits based on the physical or virtual machine being used. Even a large server with many GB (or even TB) of memory and many cores will eventually be unable to handle further containers, especially with workload from non-trivial containers.

Therefore, you should  "limit" the Docker container instantiations by setting the number of `Silver Fabric engine instances` to acceptable upper limit that reflects the capability of the `Docker host` and the characteristics of the Docker container resource consumption.

Configuring Silver Fabric Engine Resource Preference
-----------------------------------------------------

Since not all Silver Fabric daemon engine hosts(physical or virtual) are Docker-enabled, a [Resource Preference rule] needs to be set when configuring a Silver Fabric component from this enabler using **Docker Enabled** engine property.
This allows Silver Fabric Broker to deploy component to the right engine hosts that are Docker-enabled; otherwise the component deployment will fail.

In addition, you can also use the **Docker VersionInfo** engine property to run on certain Docker version only.

Building image using `Dockerfile`
---------------------------------
This enabler allows you to build an image using `Dockerfile` before running a container, but you are required to pulling down beforehand any base image(s) that is required on a `Docker host` since this is security-sensitive operation.

`TIBCO Silver Fabric 5.7`'s [Puppet and Chef support] for a Silver Fabric component maybe use for this purpose. You may also use any other CM toolings of your choice.
Typically, doing a `Dockerfile` build for production on the fly is discouraged as `Docker hosts` may not have access to Internet or may face other security, permission or unforeseen build issues.

***Note***:
When a `Dockerfile` build is initiated on more than one engines on a given `Docker host` for a Silver Fabric component, the build process is serialized so that the same image is not being build twice.

Pulling images from `Docker Hub` or private registries
------------------------------------------------------
Like building `Dockerfile`, this too is left to using `Puppet` and `Chef` or some CM toolings of your choice.

Statistics
--------------------------------------
There are 2 kinds of statistics related to a running Docker container:
### Application-level statistics###
* This are often read via some standard mechanism like **JMX** or just log to files.
If you log to files within the container , you can mount a host "stats volume" using the runtime context variable **DOCKER_STATS_DIR** to the stats volume directory within the container.

### Container-level statistics###
* At the moment, Docker do not have a formal way to extract those statistics via Docker CLI or Remote API, nor are the container statistics that useful at the moment from the application perspective.

There are a number of OSS hacks out there but lack of an API means its liable to be broken with Docker runtime evolving. So use it at your own risks.

Regardless of Application-level or Container-level statistics, you can use Silver Fabric's **statistics scripting support** to gather statistics of interest by using `Jython`, `JRuby` or `ECMAScript`.
The script can be used to call **URLs**(`JMX`, `JDBC`, `HTTP`,etc) supported by the application running within a container or simply read host-to-docker container mounted volumes files located at **DOCKER_STATS_DIR** from a container stats dump.

Example: Get some statistics off a SQL database running in a Docker container.
```python
 def getStatistic(statName):
     statValue = 0.0
     if statName == 'Wiki Size':
         connection = dbConnection()
         statement = connection.createStatement()
         resultSet = statement.executeQuery("SELECT sum(M_FILESIZE) FROM PUBLIC.M_JSPWIKI")
         while resultSet.next():
             wikiSize = resultSet.getString(1)
             if wikiSize != None:
                 statValue = float(wikiSize)
         resultSet.close()
         statement.close()
         connection.close()
     else:
        logger.warning('Unknown statistic: ' + statName)
     
     return statValue

```

Note: By default, `Jython 2.52` and `ECMAScript` are enabled in the Silver Fabric Engines.
See `TIBCO Silver Fabric SDK API, version 5.7` for more details.

Logs
-----
Currently, there are several approaches to handling logs with Docker:

* ***Collecting from inside container*** - Either each application writes it own logs or each container starts up a log collection process in addition to the application that will be running.
* ***Collecting from outside container*** - A single collection agent runs on the host and containers have a volume mounted from the host where they write their logs.
* ***Collecting via a helper container*** - The collection agent is run in a separate container and volumes from that container are bound to any application containers using the `volumes-from` docker run option. 

This enabler adopted a variation of using the ***Collecting from outside container*** approach since most non-trivial existing applications probably already logged to a directory within the Docker container. This just needs to be exposed to the existing Silver Fabric statistic collection mechanism by mapping an external host volume to an internal Docker log volume, specified by the runtime context variable **DOCKER_LOGS_DIR**.

However, if an application logs to **STDOUT/STDERR**, Docker can extract that via the CLI ( see [docker logs]):
```sh
docker logs <container name >
```
and copy the log to **DOCKER_LOGS_DIR**. By default, this enabler collects any logs placed in the location **DOCKER_LOGS_DIR**.

***Warning***: 
There are a few operational issues with `docker logs` as it is quite primitive:
This log file grows indefinitely. Docker logs each line as a JSON message which can cause this file to grow quickly and exceed the disk space on the host since it is not rotated automatically.
The docker logs command returns all recorded logs each time it’s run. Any long running process that is a little verbose can be difficult to examine. Logs under the containers `/var/log` or other locations are not easily visible or accessible.
Also each call to `docker logs` command retrieves the whole log!


Post-activation auxiliary process injection
----------------------------------------------------
Prior to `Docker 1.3.0`, a `foreground process` is required to run inside a Docker container to keep the container running. We call this the `primary process` of the container. It is not possible to start any background process first, followed lastly by a primary process.
With `Docker 1.3.0`, it is possible to inject one or more auxiliary processes into the Docker container once its in running state(i.e. activated). This could be used creatively, for example, logging, statistics-collections or any additional processing, augmenting the primary process.

You can specify an ordered list of auxiliary processes to be injected into the activated Docker container by editing the [post_activations.cmds] specified by the runtime context variable **EXEC_CMD_FILE** with delay between process injections in seconds dictated by the runtime context variable **EXEC_CMD_DELAY**.

***Note***:
If any process injections failed, an exception is thrown and remaining injections discarded but the `primary process` still runs.

Exporting Runtime Context Variables - `linking` ala Silver Fabric
--------------------------------------------------------------------
As aforementioned, this enabler uses Silver Fabric's runtime context variables exports to replace `linking` and `volumes-from`. We briefly describes here how it works in the big picture.
The two portable attributes from a running Docker container perspective are its publicly-mapped ports and environment variables.

Example: Two Docker container `A` and `B` are configured as Silver Fabric components `sf-A` and `sf-B` respectively with `A` depending on `B`.

We can formally forced an activation sequence dependency by creating a Silver Fabric stack that activates `sf-A` first, followed by `sf-B`.
After `sf-A` is activated, it exports publicly-mapped ports and environment variables desired by `B`. 
When `sf-B` is activating, it will have access to the exported publicly-mapped ports and environment variables from `A`. `B` can then use these exported values by injecting them as its environment variables or use them to format Docker `CMD` or `ENTRYPOINT` when starting.
In this way the two Docker containers are "linked".

A real life example would be a `MySQL` database as `B` and a web application running Apache Tomcat as `A`.

See `TIBCO Silver Fabric SDK API, version 5.7` for more details

Runtime Context Variable Providers and service discovery - `microservices` ala Silver Fabric
--------------------------------------------------------------------------------------------------
Cloud popularize [microservice architectural style](http://martinfowler.com/articles/microservices.html):
where building applications as suites of services are favored over monolith. 
In this style, services are independently deployable and scalable where each service also provides a firm module boundary, even allowing for different services to be written in different programming languages.
Docker espouse this practice and advocate `single process` container use in this manner.

`TIBCO Silver Fabric SDK API` allows you to implement and register one or more `Variable providers` as a source of configurations and environment values to be used when activating your Docker containers. This source is constantly polled and updated by the Silver Fabric broker and make available as sets of runtime context variables namespaced by the name of the provider.
This allows a `service discovery` usage.

For example,  you can activate a pool of `MySQL` docker containers and register them under a variable provider `MySQL_Swarm` with their key-value pairs of database connection parameters(jdbc urls, ports, passwords, etc).
When a database consuming docker container needs a `MySQL` instance, it just ask for a set of "available" connection parameters namespaced under `MySQL_Swarm` and access it like so:
```bash
${MYSQL_Swarm.get("JDBC_URL")}
```
***Note***:
A service discovery `Varible provider` maybe implemented using a lightweight distributed key value store like [etcd](https://coreos.com/using-coreos/etcd/) with each `Docker hosts` serving as a `etcd node`.

See ```AbstractVariableProvider``` and ```AbstractDynamicVariableProvider``` in the `TIBCO Silver Fabric SDK API 5.7` for details.


Silver Fabric Engine activation info from Docker container
----------------------------------------------------------
[Container-related metadata] info are collected as activation info by the engine that proxy the lifecycle of the associated Docker it manages. They are prefixed by **docker_**.

Component lifecycle notifications
---------------------------------
While Silver Fabric `component dependency rule` allows you to specify a stack `X` where component `A` depends on `B`,`C`,`D`,etc during the stack activation or shutdown, Silver Fabric's [component notification feature] allows any component `A` to register notifications related to any other component's lifecycle. This is especially helpful with respect to its dependent components `B`,`C`,`D` which may restart or failed while the depending component `A` is runnning.
With this notification, component `A` can be routed or bind to different dependent components or take the necessary course of action.

See ```ComponentNotificationFeatureInfo``` in the `TIBCO Silver Fabric SDK API 5.7` for details.

Continuous Deployment and archive scaling
------------------------------------------
Continuous J2EE archives deployment and associated `microscaling` of archives for a running Silver Fabric component can be brought about by using `ArchiveManagementFeatureInfo`, in conjunction with implementation of scripting support for interface `ArchiveManagement`.

See `TIBCO Silver Fabric SDK API, version 5.7` for more details


Runtime Context Variables
--------------------------------------
The runtime context variables for this enabler are classified into 4 categories:

###A. Operations-related###

Variable Name|Default value|Type|Description|Export|Auto Increment
---|---|---|---|---|---
**USE_SUDO**|false|Environment|Run Docker with sudo. The sudo must not prompt for password!|false|None
**JDK_NAME**|j2sdk|String|The name of the required JDK|false|None
**JDK_VERSION**|1.7|String|The version of the required JDK|false|None
**JAVA_HOME**|${GRIDLIB_JAVA_HOME}|Environment|The Java home directory|false|None
**DELETE_RUNTIME_DIR_ON_SHUTDOWN**|true|Environment|Whether to delete the Docker runtime directory on shutdown. This includes removing the Docker container.|false|None
**COMPONENT_INSTANCE_NUMBERING_ENABLED**|false|String|Allows distinct component instance numbers assignment and ensuring numbers are reused by delaying activations.|false|None
**DOCKER_BASE_DIR**|${container.getWorkDir()}/docker|Environment|Base parent dir containing Dockerfile build context dir, logs dir and stats dir|false|None
**DOCKER_CONTEXT_DIR**|${DOCKER_BASE_DIR}/docker_context|Environment|Dir containing the Dockerfile and associated dirs and files to be used in image build.|false|None
**DOCKER_LOGS_DIR**|${DOCKER_BASE_DIR}/docker_logs|Environment|Host dir mounted for dumping any logs data from within Docker containers|false|None
**DOCKER_STATS_DIR**|${DOCKER_BASE_DIR}/docker_stats|Environment|Host dir mounted for dumping any stats data from within Docker containers|false|None
**DOCKER_ENVS_DIR**|${DOCKER_BASE_DIR}/docker_envs|Environment|Host dir where environment properties files for container are located|false|None
**DOCKER_EXECS_DIR**|${DOCKER_BASE_DIR}/docker_exec|Environment|Host dir where Docker container post-activation helper process-injection commands are located. (Valid for Docker >=1.3.0 only)|false|None
**HTTP_PORT**|9090|Environment|HTTP listen port|false|Numeric
**HTTPS_PORT**|9443|Environment|HTTPS listen port|false|Numeric

###B. Dockerfile build-related###

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

###C. Docker container-related###

Variable Name|Default value|Type|Description|Export|Auto Increment
---|---|---|---|---|---
**DOCKER_CONTAINER_NAME**||String|Base name of the container, with instances of container having same base name prefixed by engine instance id. Ex. 'my_container1','my_container2',etc. Leave this blank if you want unique name auto-generated.|false|Append
**REUSE_CONTAINER**|false|String|Reuse existing same named container instead of creating a new one|false|None
**PRIVILEDGED_MODE**|false|String|Set the container to run in privileged mode|false|None
**CMD_OVERRIDE**||Environment|Command executable (and any of its arguments) to run in a container that result in a foreground process. Note: If the image also specifies an `ENTRYPOINT` then this get appended as arguments to the `ENTRYPOINT`.|false|None
**ENTRY_POINT_OVERRIDE**||String|Overrides default executable(usually `/bin/bash`) to run when container starts up. Use this in conjunction with `CMD_OVERRIDE`|false|None
**USER_OVERRIDE**||String|Overrides default user(`root`, `uid=0`) within a container when it starts up. Use Username or UID|false|None
**WORKDIR_OVERRIDE**||String|Overrides default working dir inside Docker container|false|None
**!ENV_FILE_Default**|${DOCKER_ENVS_DIR}/envs.properties|String|An properties file containing environment variables to be injected into container. This may override some or all `ENV` already set in the image|false|None
**MEMORY_LIMIT**|256m|String|Upper limit to container RAM memory in the format NNNx where NNN is an integer and x is the unit(b,k,m, or g). ex. 256m|false|None
**MAX_STOP_TIME_BEFORE_KILL**|30|Environment|Maxiumum secs to wait before a force stop is used to shutdown a Docker container|false|None
**CID_FILE**|${DOCKER_BASE_DIR}/${DOCKER_CONTAINER_NAME}.cid|Environment|A file that is created when a Docker container is created and run.|false|None
**BIND_ON_ALL_LOCAL_ADDRESSES**|false|Environment|Specify if all network interfaces should be bounded for all public port access|false|None
**LISTEN_ADDRESS_NET_MASK**||Environment|A comma delimited list of net masks in `CIDR` notation. The first IP address found that matches one of the net masks is used as the listen address. Note that BIND_ON_ALL_LOCAL_ADDRESSES overrides this setting.|false|None

###D. Docker container post-activation auxiliary processes injections###

Variable Name|Default value|Type|Description|Export|Auto Increment
---|---|---|---|---|---
**EXEC_CMD_ENABLED**|false|String|Run a sequence of commands for Docker container post-activation helper process-injections. (Valid for Docker >=1.3.0 only)|false|None
**EXEC_CMD_FILE**|${DOCKER_EXECS_DIR}/post_activation.cmds|String|A file containing a sequence of commands for Docker container post-activation helper process-injections. (Valid for Docker >=1.3.0 only)|false|None
**EXEC_CMD_DELAY**|1|String|Delay in seconds in-between injecting a sequence of processes specified in 'EXEC_CMD_FILE'. (Valid for Docker >=1.3.0 only)|false|None


Special runtime context variable name directives
------------------------------------------------

Runtime context variables names that are prefixed with [Special directives] allow them to be treated differently.

Prefix directive|Purpose|Variable name syntax|Variable value
---|---|---|---
**!PORT_EXPOSE_**|Expose a private port internally|`!PORT_EXPOSE_`xxxx|\<`internal port`\>
**!PORT_MAP_** | Map an external host port to an internally exposed Docker container port|`!PORT_MAP_`xxxx|\<`external port`\>:\<`internal port`\>
**!VOL_MAP_**| Mount an external host volume to an internal Docker container volume|`!VOL_MAP_`xxxx|\<`external volume path`\>:\<`internal volume path`\>:[`rw`,`ro`]
**!ENV_VAR_**| Inject an environment variable into the Docker container|`!ENV_VAR_`xxxx|`key=value`, `key=`, `key`
**!ENV_FILE_**| Inject a list of environment variables specified as `key=value` pairs from a file into the Docker container|`!ENV_FILE_`xxxx|\<`path to a file`\>
**!SEC_OPT_**| Specify a Docker conatiner security option(Valid only for Docker \>= 1.3.0)|`!SEC_OPT_`xxxx|One or more from the list [ `label:user:`\<**USER**\>, `label:role:`\<**ROLE**\>, `label:type:`\<**TYPE**\>, `label:level:`\<**LEVEL**\>, `label:disable`,`apparmor:`\<**PROFILE**\>]. See [Docker and SELinux] for detail usage for `USER`,`ROLE`,`TYPE` and `LEVEL`.

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

Note: If you are using `COPY` or `ADD` commands in the Dockerfile, you can add the files to be copied/added in those commands as content files in the same relative content dir above.
```



[Install Docker]:https://docs.docker.com/installation/
[Password-less sudo]:https://docs.docker.com/installation/ubuntulinux/#giving-non-root-access
[Configure Docker Remote API]:http://www.virtuallyghetto.com/2014/07/quick-tip-how-to-enable-docker-remote-api.html
[docker logs]:https://docs.docker.com/reference/commandline/cli/#logs
[Docker and SELinux]:http://www.projectatomic.io/docs/docker-and-selinux/

[Resource Preference rule]:https://github.com/fabrician/docker-enabler/blob/master/src/main/resources/images/docker_resource_preference.gif

[Special directives]:https://github.com/fabrician/docker-enabler/blob/master/src/main/resources/images/docker_runtime_context_vars.gif

[Container-related metadata]:https://github.com/fabrician/docker-enabler/blob/master/src/main/resources/images/docker_enabler_activationInfo.gif

[post_activations.cmds]:https://github.com/fabrician/docker-enabler/blob/master/src/main/resources/images/docker_post_activation_process_injections.gif

[Puppet and Chef support]:https://github.com/fabrician/docker-enabler/blob/master/src/main/resources/images/sf_5.7_puppet_chef_support.gif

[component notification feature]:https://github.com/fabrician/docker-enabler/blob/master/src/main/resources/images/sf_component_notifications.gif
