Redis Docker container
=========================================================

Introduction
-------------
This is an example based on [Redis](https://registry.hub.docker.com/u/tutum/redis/),an open source, BSD licensed, advanced key-value cache and store.

Highlights
----------
 - shows how to map Docker host ports to internal Docker container ports
 - automatic port increments across Silver Fabric engines for Redis listening port
 - shows how to set environment variables into Docker container
 - shows usage of [Silver Fabric runtime context variables and variable substitutions](https://github.com/fabrician/docker-enabler/blob/master/examples/images/example_Redis_rcv.gif)

Setup
------
- (1) Do a pull of Redis via docker command:

    ```bash
       docker pull tutum/redis
    ```
- (2) Download the `SilverFabricCLI.tar.gz` library from the Broker and unzip into a directory.
- (3) Download and unzip `MyRedis-ant-package.zip`  into the same directory as above
- (4) Run the Ant task `build.xml` against a Silver Fabric Broker with this `Silver Fabric Docker Enabler` installed

   ```ant
      ant -f build.xml
   ```
- (5) Run the stack `MyRedis` via the Silver Fabric Admin UI.
      
      This will launch 2 instances of Redis Docker container, map to 2 different Docker host ports. This allows you to log  into the 2 Redis instances and do "SET/GET" commands to set/get key-values.

- (6) Access the Redis instances: 

You can access your `Redis` instances via one of the myriads of [Redis clients](http://redis.io/clients)
or from another `Redis` server already installed; via its `redis-cli` like so:

      ```
      redis-cli -a <password> - h <docker host> - p < docker host port>
      
      where <docker host> - the host name or IP address of the docker host
            < docker host port> - the externally-mapped port of Redis docker container.
      
      ```
    
