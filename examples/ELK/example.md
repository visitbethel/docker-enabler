ELK stack
----------

This example shows you how to create an `ELK stack`; motivated by the an excellent [online tutorial](http://evanhazlett.com/2013/08/Logstash-and-Kibana-via-Docker/)

Briefly, the 3 ELK components of the stack are

- `ElasticSearch` which stores the logged messages in a database
- `Logstash` which receives logged messages and relays them to ElasticSearch after filtering, formatting and parsing
- `Kibana` which connects to ElasticSearch to retrieve the logged messages and presents them in a web interface.

Normally, the first thing that we need to do and could have done is package up our three applications components above along with their dependencies into three separate Docker images.  
But, since we are "lazy", we just reused what others has already done on the `DockerHub`:

- [ElasticSearch](https://registry.hub.docker.com/u/dockerfile/elasticsearch/)
- [Logstash](https://registry.hub.docker.com/u/arcus/logstash/)
- [Kibana](https://registry.hub.docker.com/u/arcus/kibana/)


The mechanics of the various components are roughly as follows:

- `ElasticSearch` exposes HTTP port 9200 for HTTP clients and port 9300 for inter-node communication
- `Logstash` exposes port 514 as forward port for any `syslog` messages sources and route them to `ElasticSearch` port 9300 after filtering and parsing
- `Kibana` exposes port 80 as web interface and gets its data from `ElasticSearch` HTTP port 9200
