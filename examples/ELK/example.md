ELK stack tutorial
==================

Overview
----------

This example shows how to create an ELK stack based on an adaptation of [Logstash and Kibana via Docker] (http://evanhazlett.com/2013/08/Logstash-and-Kibana-via-Docker/), an online tutorial by Evan Hazlett.
In this adaptation, the **Silver Fabric Docker Enabler** is used to specify Silver Fabric Components from Docker containers and then combine them into a Silver Fabric Stack.

Briefly, the three `ELK` components of the Stack are

- `ElasticSearch` which stores the logged messages in a database
- `Logstash` which receives logged messages and relays them to ElasticSearch after filtering, formatting and parsing
- `Kibana` which connects to ElasticSearch to retrieve the logged messages and presents them in a web interface.

Normally, the first thing to do would be package the three application components above along with their dependencies into three separate Docker images.  
Alternatively, these can be reused from what others have already done on DockerHub:

- [ElasticSearch](https://registry.hub.docker.com/u/dockerfile/elasticsearch/)
- [Logstash](https://registry.hub.docker.com/u/arcus/logstash/)
- [Kibana](https://registry.hub.docker.com/u/arcus/kibana/)

The Docker container mechanics for `ELK` Stack
----------------------------------------------

The mechanics of the various components are roughly as follows:

- `ElasticSearch` exposes HTTP port `9200` for HTTP clients and port `9300` for inter-node communication
- `Logstash` exposes port `514` as the shipping port for any `syslog` messages sources and routes them to the `ElasticSearch` port `9300` after any filtering and parsing.
- `Kibana` exposes port `80` for its own web interface and gets its data from `ElasticSearch` at HTTP port `9200`.


Wiring Docker containers with Silver Fabric Components and Stack
----------------------------------------------------------------
#####The exercise below assumes a basic working knowledge of TIBCO Silver Fabric and Docker.

We'll create three Silver Fabric Components associated with the three Docker containers above and run each component in **Ad Hoc** Stack mode to iteratively create the Stack.

To create the `ELK` Stack iteratively:

- First, create and publish a Silver Fabric Component associated with the `ElasticSearch` Docker image above.
     - Set the Runtime context variables for this Component: ![this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_elasticsearch_rcv.gif?raw=true "ElasticSearch runtime context variables")
     - Start the `ElasticSearch` Component under **Ad Hoc Stack** mode: ![so](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_elasticsearch_adhoc_mode.gif?raw=true "Starting ElasticSearch in Ad Hoc stack")
     - Wait until the `ElasticSearch` Component is seen in state ![running](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_elasticsearch_engine.gif?raw=true "ElasticSearch in running state") as seen in the **Engines view**
     - Check the  running `ElasticSearch` Component's Engine activation info; it should look similar to ![this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_elasticsearch_activationinfo.gif?raw=true "ElasticSearch Engine activation info")

- Next, create and publish a Silver Fabric Component associated with the `Kibana` Docker image above.
     - Set the `HTTP Routing` context: ![this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_kibana_http_routing.gif?raw=true "Kibana HTTP Routing setting")
     - Set the Runtime context variables for this Component: ![this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_kibana_rcv.gif?raw=true "Kibana runtime context variables")
     - Set its `Component dependency` to the Component `ElasticSearch`: ![this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_kibana_component_dep.gif?raw=true "Kibana component dependency on ElasticSearch")
     - Start the `Kibana` Component under **Ad Hoc Stack** mode
     - Wait till the `Kibana` Component is running as seen in the Engines view
     - Check the running `Kibana` Component's Engine activation info; should look similar to ![this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_kibana_activationinfo.gif?raw=true "Kibana Engine activation info")
     
- Similarly, create and publish a Silver Fabric Component associated with the `Logstash` Docker image above
     - Set Runtime context variables for this component: ![this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_logstash_rcv.gif?raw=true "Logstash runtime context variables")
     - Set its `Component dependency` to component `ElasticSearch` like in the `Kibana` Component
     - Start the `Logstash` Component under **Ad Hoc Stack**
     - Wait until the `Logstash` Component is running as seen in the Engines view
     - Check the running `Logstash` Component's Engine activation info; it should look similar to
![this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_logstash_activationinfo.gif?raw=true "Logstash Engine activation info")

- Finally, create and publish a named Silver Fabric Stack, `MyELK` wiring the `Component dependencies` among `ElasticSearch`.`Kibana` and `LogStash` components like ![so](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_myelk_stack_component_dep.gif?raw=true "MyELK Stack declaration").

Note: 
- The Components `Kibana` and `Logstash` each, already has a `Component dependency` on the common component `ElasticSearch`. The dependency can be declared at Component-level(as in this example) or specified at the Stack level.
- **Ad Hoc stack** mode is used in Silver Fabric to start a Component to test it under an "anonymous" Stack as done above. This enables users to create a named Stack iteratively from its running Components.

Using `ELK stack` to collect `syslogs` messages
-----------------------------------------------

To use the `ELK stack` created above to collect `syslog` messages from sources:

- Stop the components `ElasticSearch`,`Kibana` and `Logstash` from running under **Ad Hoc stack** mode
- Wait until all Components have stopped and unallocated as seen in the Engines view.
- Start the named Stack `MyELK` above, by starting it in **Manual mode**. This starts the Stack immediately.
- After the Stack is started, look at the `Engine activation info` associated with the Silver Fabric Component named `Logstash`. It should look like [so](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_logstash_activationinfo.gif).
Take note of the value of the exported runtime context variable named `LOGSTASH_SYSLOG_PORT`. This is the `TCP port` value where all the `syslog` sources must used to send their logs to the running `Logstash` component(Docker container).

- Next, the `Kibana` web GUI can be accessed via
```
   http://<broker_host>:8080/kibana
   (Assume a route context has been set at "/kibana" for the HTTP feature.)
```

or,
```
   http://<docker_host>:<kibana_http_port>
   (Look at the engine activation info for component "Kibana" and "HTTP_PORT" runtime context variable value)
```

`Syslog` sources
-----------------

There are variety of ways to send `syslog` messages to the runnning `Logstash`:

- Some standalone sources with `syslog` tools:
   - Linux [logger utility](http://manpages.ubuntu.com/manpages/precise/man1/logger.1.html)
   - Linux [Logzilla syslogen](https://subversion.assembla.com/svn/logzilla/scripts/contrib/sysloggen/sysloggen)
   - Windows [Kiwi SyslogGen Freeware](http://www.kiwisyslog.com/help/sysloggen/index.html?kiwisysloggen.htm)
   - Windows [SysLogGen](http://www.snmpsoft.com/freetools/sysloggen.html)
   - Java [Slogger](http://syslog-slogger.sourceforge.net/)

- Docker containers or Silver Fabric Enabler Components

For example, we can create a new Docker container or configure an existing Silver Fabric Enabler to log directly to the running `Logstash` as highlighted [here for the Apache Tomcat](http://www.unixpowered.com/unixpowered/2012/05/29/configuring-tomcat-to-log-via-syslog/)

#####Important: Remember to use the `LOGSTASH_SYSLOG_PORT` TCP port value and the `Docker host` IP where the `Logstash` is running.

An example of using `Kiwi Syslogen` as syslog source:

- First enter the value of `LOGSTASH_SYSLOG_PORT` into `Kiwi Syslogen GUI` like
![so](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_syslog_source_kiwigen.gif?raw=true "Example using Kiwi Syslogen GUI as syslogs source")

- Next open the `Kibana` GUI and you will see that the syslogs messages has been forwarded to `ElasticSearch` server by `Logstash` like ![so](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/images/example_kibana_syslog_input.gif?raw=true "Kibana GUI with syslogs inputs")

Sample solution
----------------

To run the sample solution include here:

- (1) Do a pull of ElasticSearch, Logstash and Kibana with the `docker pull` command:

    ```bash
       docker pull dockerfile/elasticsearch
       docker pull arcus/kibana
       docker pull arcus/logstash
    ```
- (2) Download the `SilverFabricCLI.tar.gz` library from the Broker and unzip into a directory.
- (3) Download and unzip `MyELK-ant-package.zip`  into the same directory as above.
- (4) Run the Ant task `build.xml` against a Silver Fabric Broker with this `Silver Fabric Docker Enabler` installed

   ```ant
      ant -f build.xml
   ```
   This reconstructs a stack `MyELK` and the three Components `ElasticSearch`,`Logstash` and `Kibana` it is composed from.
   
- (5) Run the stack `MyELK` with the Silver Fabric Admin UI.
      
      This will launch one instance of the `ElasticSearch` Docker container first, followed by one instance of `Logstash` and `Kibana` container each.

- (6) Open the `Kibana` web app.
- (7) Send some `syslogs` messages from a source and refresh the `Kibana` web page and you should see some `syslogs` messages coming through.

     
