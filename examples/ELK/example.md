ELK stack
----------

This example shows you how to create an `ELK stack`.
The various ELK components of the stack are

- `ElasticSearch` which stores the logged messages in a database
- `Logstash` which receives logged messages and relays them to ElasticSearch after filtering, formatting and parsing
- `Kibana` which connects to ElasticSearch to retrieve the logged messages and presents them in a web interface.

Normally, the first thing that we need to do and could have done is package up our three applications components above along with their dependencies into three separate Docker images.  
But, since we are "lazy", we just reused what others has already done on the `DockerHub`:

- [ElasticSearch]()
- [Logstash](https://registry.hub.docker.com/u/arcus/logstash/)
- [Kibana](https://registry.hub.docker.com/u/arcus/kibana/)
