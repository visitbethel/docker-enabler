ELK stack
==========

Overview
----------

This example shows you how to create an `ELK stack` based on adaptation of an [online ELK Docker tutorial](http://evanhazlett.com/2013/08/Logstash-and-Kibana-via-Docker/) by `Evan Hazlett`.

Briefly, the 3 `ELK` components of the stack are

- `ElasticSearch` which stores the logged messages in a database
- `Logstash` which receives logged messages and relays them to ElasticSearch after filtering, formatting and parsing
- `Kibana` which connects to ElasticSearch to retrieve the logged messages and presents them in a web interface.

Normally, the first thing that we need to do and could have done is package up our three applications components above along with their dependencies into three separate Docker images.  
Alternatively, we just reused what others has already done on the `DockerHub`:

- [ElasticSearch](https://registry.hub.docker.com/u/dockerfile/elasticsearch/)
- [Logstash](https://registry.hub.docker.com/u/arcus/logstash/)
- [Kibana](https://registry.hub.docker.com/u/arcus/kibana/)

The Docker container mechanics
------------------------------

The mechanics of the various components are roughly as follows:

- `ElasticSearch` exposes HTTP port `9200` for HTTP clients and port `9300` for inter-node communication
- `Logstash` exposes port `514` as shipping port for any `syslog` messages sources and route them to `ElasticSearch` port `9300` after any filtering and parsing.
- `Kibana` exposes port `80` for its own web interface and gets its data from `ElasticSearch` via HTTP port `9200`.


Wiring Docker containers via `Silver Fabric components and stack`
----------------------------------------------------------------

The steps to create an `ELK stack` are as follows:

- First, create and publish a `Silver Fabric componen`t associated with the `ElasticSearch` Docker image above.
     Runtime context variables for this component are set like [this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/example_elasticsearch_rcv.gif)

- Next, create and publish a `Silver Fabric component` associated with the `Kibana` Docker image above.
      Runtime context variables for this component are set like [this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/example_kibana_rcv.gif)

- Similarly, create and publish a `Silver Fabric component` associated with the `Logstash` Docker image above.
       Runtime context variables for this component are set like [this](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/example_logstash_rcv.gif)

- Finally, create and publish `Silver Fabric stack`, `MyELK` wiring the `component dependencies` among `ElasticSearch`.`Kibana` and `LogStash` components like [so](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/example_myelk_stack_component_dep.gif). 

Note: Components `Kibana` and `Logstash` each, has a `component dependency` on the common component `ElasticSearch`. The dependency can be declared at component-level(as in this example) or specified at stack-level.

Using `ELK stack` to collect `syslogs` messages
-----------------------------------------------

To use the `ELK stack` created above to collect `syslog` messages from sources, follow the steps below:

- First, start the `Silver Fabric stack` named `MyELK` above by starting it in `MANUAL mode`. This starts the stack immediately.
- After the stack is started, look at the `engine activation info` associated with the `Silver Fabric component` named `Logstash`. It should look like [so](https://github.com/fabrician/docker-enabler/blob/master/examples/ELK/example_logstash_activationinfo.gif).
Take note of the value of the `exported runtime context variable` named `LOGSTASH_SYSLOG_PORT`. This is the `TCP port` value where all the `syslog` sources must used to send their logs to the running `Logstash` component(Docker container).

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

- Some standalone sources via `syslog` tools:
   - Linux [logger utility](http://manpages.ubuntu.com/manpages/precise/man1/logger.1.html)
   - Linux [Logzilla syslogen](https://subversion.assembla.com/svn/logzilla/scripts/contrib/sysloggen/sysloggen)
   - Windows [Kiwi SyslogGen Freeware](http://www.kiwisyslog.com/help/sysloggen/index.html?kiwisysloggen.htm)
   - Windows [SysLogGen](http://www.snmpsoft.com/freetools/sysloggen.html)
   - Java [Slogger](http://syslog-slogger.sourceforge.net/)

- Docker container sources:

We can also create a Docker container that logs directly to the running `Logstash` as highlighted [here for the Apache Tomcat](http://www.unixpowered.com/unixpowered/2012/05/29/configuring-tomcat-to-log-via-syslog/)

#####Important: Remember to use the `LOGSTASH_SYSLOG_PORT` TCP port value and the `Docker host` IP where the `Logstash` is running.

